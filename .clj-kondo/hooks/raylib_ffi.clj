(ns hooks.raylib-ffi
  "clj-kondo hook for coffi.ffi/defcfn.

  `defcfn` binds a C symbol to a Clojure var:

      (defcfn measure-text
        \"Measure string width for default font\"
        {:arglists '([text font-size])}
        \"MeasureText\"
        [::mem/c-string ::mem/int] ::mem/int)

  clj-kondo cannot see through the macro. Without a hook every bound name is
  an `Unresolved symbol` in src/raylib/ and an `Unresolved var: rcw/…` at each
  call site in the examples — 776 findings across the suite, enough to make
  the linter useless as a gate.

  coffi ships its own hook (clj-kondo.exports/org.suskalo/coffi), and it does
  resolve those. But its rewrite gives every binding a body that returns the
  *argument vector*, so clj-kondo infers `vector` as the return type of every
  C function. That turns ordinary arithmetic on a binding into a false error:

      (/ (rcw/get-screen-width) 2.0)
      ;; => error: Expected: number, received: vector.

  171 of those, which is why this project overrides the shipped hook rather
  than importing it. The rewrite below is the same shape, except the body is
  a literal whose inferred type matches the *declared C return type*. That
  buys three things clj-kondo could not otherwise know:

    * the var exists (kills the 776 false positives),
    * its arity — passing the wrong number of arguments to a binding is
      exactly the FFI mistake that otherwise surfaces as a native crash,
    * its return type, so `(/ (rcw/get-screen-width) 2.0)` type-checks for
      real instead of being suppressed.

  The syntax validation is carried over from coffi's hook so overriding it
  loses nothing: a malformed type or native symbol still reports
  `:coffi.ffi/invalid-syntax`."
  (:require [clj-kondo.hooks-api :as api]))

;; Type keywords are matched on their NAME, not the fully-qualified keyword.
;; clj-kondo's reader leaves an auto-resolved keyword's alias UNRESOLVED:
;; `::mem/int` arrives at the hook as `:mem/int`, never `:coffi.mem/int`
;; (verified against clj-kondo v2026.05.25). Matching the name also makes this
;; independent of what each namespace aliases coffi.mem to.

;; Every coffi scalar that deserializes to a Clojure number. "ubyte" is this
;; project's own (raylib.internals) and deserializes via Byte/toUnsignedLong.
(def ^:private numeric-type-names
  #{"byte" "short" "int" "long" "char" "float" "double" "ubyte"})

;; ::ri/bool deserializes with (not (zero? obj)) — a real boolean, not 0/1.
;; Raw ::mem/byte returns from raylib predicates stay numeric above; those call
;; sites wrap them in `pos?` themselves.
(def ^:private boolean-type-names #{"bool"})

;; Deserialize to nil (void) or to an opaque handle only ever passed back into
;; another binding (pointer). Neither is ever type-checked against.
(def ^:private nil-type-names #{"void" "pointer"})

(defn- validate-type
  "Carried over from coffi's own hook — a type is a qualified keyword, or a
  vector whose first element is one (e.g. [::mem/struct …])."
  [node]
  (when-not (or (qualified-keyword? (api/sexpr node))
                (and (api/vector-node? node)
                     (qualified-keyword? (api/sexpr (first (:children node))))))
    (api/reg-finding!
     {:row (:row (meta node))
      :col (:col (meta node))
      :message "A type must be a qualified keyword or a vector with one as the first element."
      :type :coffi.ffi/invalid-syntax})))

(defn- return-node
  "A literal whose inferred type matches the declared C return type.

  Anything not listed above is a struct alias (::rs/color, ::rs/vector-2,
  ::rc3d/camera3d, …). coffi deserializes those into Clojure maps, so an empty
  map node is the honest answer — it keeps `(:x (rcm/get-mouse-position))`
  type-checking without asserting which keys are present."
  [node]
  (let [k (try (api/sexpr node) (catch Exception _ nil))
        n (when (keyword? k) (name k))]
    (cond
      (nil? n)                          (api/map-node [])
      (contains? nil-type-names n)      (api/token-node nil)
      (contains? numeric-type-names n)  (api/token-node 0)
      (contains? boolean-type-names n)  (api/token-node true)
      (= "c-string" n)                  (api/string-node "")
      :else                             (api/map-node []))))

(defn defcfn
  [{:keys [node]}]
  (try
    (let [[var-name-node & more] (rest (:children node))
          ;; docstring, then attr-map, are both optional and both precede the
          ;; native symbol. Mirrors coffi's own arg-shuffling.
          [docstring-node & more] (if (and (api/string-node? (first more))
                                           (not (api/vector-node? (second more))))
                                    more
                                    (cons nil more))
          [attr-map-node & more] (if (api/map-node? (first more))
                                   more
                                   (cons nil more))
          [symbol-node native-arglist-node return-type-node & more] more]
      (when-not (or (and (api/token-node? symbol-node)
                         (simple-symbol? (api/sexpr symbol-node)))
                    (api/string-node? symbol-node))
        (api/reg-finding! {:row (:row (meta symbol-node))
                           :col (:col (meta symbol-node))
                           :message "Native symbol must be a string or symbol."
                           :type :coffi.ffi/invalid-syntax}))
      (run! validate-type (cons return-type-node (:children native-arglist-node)))
      ;; A defcfn may carry a wrapper: (defcfn f … ret native-fn [args] body).
      ;; When it does, the wrapper's own fn-tail is the real arity and body, so
      ;; use it verbatim and leave the return type to clj-kondo's inference.
      (let [wrapper (when (seq more) {:native-fn (first more) :fn-tail (rest more)})]
        (when (and (:native-fn wrapper) (empty? (:fn-tail wrapper)))
          (api/reg-finding!
           {:row (:row (meta node))
            :col (:col (meta node))
            :message "A defcfn with a native-fn must have a function body."
            :type :coffi.ffi/invalid-syntax}))
        (let [arglist (api/vector-node
                       (mapv (fn [i] (api/token-node (symbol (str "_arg" i))))
                             (range (count (:children native-arglist-node)))))
              fn-tail (if wrapper
                        (:fn-tail wrapper)
                        (list arglist (return-node return-type-node)))
              ;; The rewrite drops the C type vector and return type, so their
              ;; auto-resolved keywords (::mem/int, ::rs/color, ::ri/bool) stop
              ;; counting as uses of the aliases that qualify them — and every
              ;; binding namespace then reports its own `[coffi.mem :as mem]`
              ;; require as unused. Deleting those requires on that advice would
              ;; break the library at runtime. Carry the types through in an
              ;; attr-map instead: clj-kondo analyzes it, so the aliases stay
              ;; used, and the map is inert at runtime.
              ;; MERGED into any existing attr-map rather than appended as a
              ;; second one: `defn` accepts exactly one attr-map, so a second
              ;; map node is not analyzed and the aliases stay "unused".
              type-entries [(api/keyword-node :coffi/native-types)
                            (api/vector-node
                             (vec (cons return-type-node
                                        (:children native-arglist-node))))]
              attr-map-node (api/map-node
                             (concat (when attr-map-node (:children attr-map-node))
                                     type-entries))
              defn-node (api/list-node
                         (list* (api/token-node 'clojure.core/defn)
                                var-name-node
                                (concat (filter some? [docstring-node attr-map-node])
                                        fn-tail)))]
          ;; The wrapper form binds native-fn to a fn of the C arglist, so the
          ;; body's calls to it resolve and arity-check.
          {:node (if wrapper
                   (api/list-node
                    (list (api/token-node 'let)
                          (api/vector-node
                           [(:native-fn wrapper)
                            (api/list-node
                             (list (api/token-node 'fn) arglist
                                   (return-node return-type-node)))])
                          defn-node))
                   defn-node)})))
    (catch Exception _
      (api/reg-finding!
       {:row (:row (meta node))
        :col (:col (meta node))
        :message "Invalid syntax"
        :type :coffi.ffi/invalid-syntax})
      {:node node})))
