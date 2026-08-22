(ns raylib.raymath
  "Clojure port of raylib's `raymath.h`.

   Same category as `raylib.easings` (from `reasings.h`) and `raylib.lights`
   (from `rlights.h`): a header-only companion that ships alongside raylib
   rather than inside it, ported rather than bound.

   Ported rather than bound deliberately, and the reason is not the usual
   one. Most header-only C cannot be bound at all, but raymath actually can
   be: on Unix shared builds `RMAPI` expands to
   `__attribute__((visibility(\"default\")))`, so the symbols really are
   exported and `nm` lists them. The reason to port anyway is cost - a
   foreign call to add two floats is far more expensive than the addition -
   plus these were already being reimplemented privately in several
   examples, which is duplication this replaces.

   Vectors are the same plain maps the struct aliases use: `{:x :y}` for
   Vector2 and `{:x :y :z}` for Vector3, so results pass straight into any
   binding taking `::rs/vector-2` or `::rs/vector-3`.

   One difference from the C worth stating: raymath computes in 32-bit
   `float` and this computes in Clojure doubles. Values handed back to a
   binding are narrowed to float at the boundary anyway, so the difference
   shows up only if you compare a long chain of intermediate results against
   the C bit-for-bit.

   Matrix and Quaternion are deliberately absent. They need a Matrix struct
   that this project does not yet define, and their demand sits almost
   entirely in the 3D model examples, so they belong with that work.")

(def ^:const epsilon 0.000001)

;; ---------------------------------------------------------------- scalars

(defn clamp [value mn mx]
  (let [r (if (< value mn) mn value)]
    (if (> r mx) mx r)))

(defn lerp [start end amount]
  (+ start (* amount (- end start))))

(defn normalize
  "Position of `value` within [start end] as a 0..1 fraction."
  [value start end]
  (/ (- value start) (- end start)))

(defn remap
  "Re-express `value` from one range into another."
  [value input-start input-end output-start output-end]
  (+ (* (/ (- value input-start) (- input-end input-start))
        (- output-end output-start))
     output-start))

(defn wrap
  "Wrap `value` into [mn mx), the way an angle wraps past 360."
  [value mn mx]
  (- value (* (- mx mn) (Math/floor (/ (- value mn) (- mx mn))))))

(defn float-equals?
  "Almost-equal comparison, scaled to the larger operand rather than a flat
   tolerance - so it stays meaningful for both tiny and huge values."
  [x y]
  (<= (Math/abs (- (double x) (double y)))
      (* epsilon (Math/max 1.0 (Math/max (Math/abs (double x)) (Math/abs (double y)))))))

;; --------------------------------------------------------------- Vector2

(defn v2 [x y] {:x x :y y})
(def v2-zero (constantly {:x 0.0 :y 0.0}))
(def v2-one (constantly {:x 1.0 :y 1.0}))

(defn v2-add [a b] {:x (+ (:x a) (:x b)) :y (+ (:y a) (:y b))})
(defn v2-add-value [v n] {:x (+ (:x v) n) :y (+ (:y v) n)})
(defn v2-subtract [a b] {:x (- (:x a) (:x b)) :y (- (:y a) (:y b))})
(defn v2-subtract-value [v n] {:x (- (:x v) n) :y (- (:y v) n)})
(defn v2-scale [v s] {:x (* (:x v) s) :y (* (:y v) s)})
(defn v2-multiply [a b] {:x (* (:x a) (:x b)) :y (* (:y a) (:y b))})
(defn v2-divide [a b] {:x (/ (:x a) (:x b)) :y (/ (:y a) (:y b))})
(defn v2-negate [v] {:x (- (:x v)) :y (- (:y v))})
(defn v2-invert [v] {:x (/ 1.0 (:x v)) :y (/ 1.0 (:y v))})

(defn v2-length-sqr [v] (+ (* (:x v) (:x v)) (* (:y v) (:y v))))
(defn v2-length [v] (Math/sqrt (v2-length-sqr v)))
(defn v2-dot [a b] (+ (* (:x a) (:x b)) (* (:y a) (:y b))))

(defn v2-distance-sqr [a b]
  (let [dx (- (:x a) (:x b)) dy (- (:y a) (:y b))]
    (+ (* dx dx) (* dy dy))))

(defn v2-distance [a b] (Math/sqrt (v2-distance-sqr a b)))

(defn v2-angle
  "Signed angle in radians from `a` to `b`, via atan2(det, dot)."
  [a b]
  (Math/atan2 (- (* (:x a) (:y b)) (* (:y a) (:x b)))
              (+ (* (:x a) (:x b)) (* (:y a) (:y b)))))

(defn v2-line-angle
  "Angle of the line from `start` to `end`.

   The sign is negated, matching the C. raymath carries a standing TODO
   questioning whether that clockwise convention is wanted; it is kept here
   so a port behaves like its original rather than silently disagreeing."
  [start end]
  (- (Math/atan2 (- (:y end) (:y start)) (- (:x end) (:x start)))))

(defn v2-normalize [v]
  (let [len (v2-length v)]
    (if (zero? len) {:x 0.0 :y 0.0} (v2-scale v (/ 1.0 len)))))

(defn v2-lerp [a b amount]
  {:x (lerp (:x a) (:x b) amount) :y (lerp (:y a) (:y b) amount)})

(defn v2-reflect
  "Reflect `v` about `normal`."
  [v normal]
  (let [d (v2-dot v normal)]
    {:x (- (:x v) (* 2.0 (:x normal) d))
     :y (- (:y v) (* 2.0 (:y normal) d))}))

(defn v2-rotate [v angle]
  (let [c (Math/cos angle) s (Math/sin angle)]
    {:x (- (* (:x v) c) (* (:y v) s))
     :y (+ (* (:x v) s) (* (:y v) c))}))

(defn v2-move-towards
  "Step `v` toward `target` by at most `max-distance`, snapping to the
   target once within range."
  [v target max-distance]
  (let [dx (- (:x target) (:x v)) dy (- (:y target) (:y v))
        value (+ (* dx dx) (* dy dy))]
    (if (or (zero? value)
            (and (>= max-distance 0) (<= value (* max-distance max-distance))))
      target
      (let [dist (Math/sqrt value)]
        {:x (+ (:x v) (* (/ dx dist) max-distance))
         :y (+ (:y v) (* (/ dy dist) max-distance))}))))

(defn v2-min [a b] {:x (Math/min (:x a) (:x b)) :y (Math/min (:y a) (:y b))})
(defn v2-max [a b] {:x (Math/max (:x a) (:x b)) :y (Math/max (:y a) (:y b))})

(defn v2-clamp [v mn mx]
  {:x (clamp (:x v) (:x mn) (:x mx)) :y (clamp (:y v) (:y mn) (:y mx))})

(defn v2-clamp-value
  "Clamp the vector's LENGTH into [mn mx], keeping its direction."
  [v mn mx]
  (let [len-sqr (v2-length-sqr v)]
    (if (zero? len-sqr)
      v
      (let [len (Math/sqrt len-sqr)
            scale (cond (< len mn) (/ mn len)
                        (> len mx) (/ mx len)
                        :else 1.0)]
        (v2-scale v scale)))))

(defn v2-equals? [a b]
  (and (float-equals? (:x a) (:x b)) (float-equals? (:y a) (:y b))))

;; --------------------------------------------------------------- Vector3

(defn v3 [x y z] {:x x :y y :z z})
(def v3-zero (constantly {:x 0.0 :y 0.0 :z 0.0}))
(def v3-one (constantly {:x 1.0 :y 1.0 :z 1.0}))

(defn v3-add [a b] {:x (+ (:x a) (:x b)) :y (+ (:y a) (:y b)) :z (+ (:z a) (:z b))})
(defn v3-add-value [v n] {:x (+ (:x v) n) :y (+ (:y v) n) :z (+ (:z v) n)})
(defn v3-subtract [a b] {:x (- (:x a) (:x b)) :y (- (:y a) (:y b)) :z (- (:z a) (:z b))})
(defn v3-subtract-value [v n] {:x (- (:x v) n) :y (- (:y v) n) :z (- (:z v) n)})
(defn v3-scale [v s] {:x (* (:x v) s) :y (* (:y v) s) :z (* (:z v) s)})
(defn v3-multiply [a b] {:x (* (:x a) (:x b)) :y (* (:y a) (:y b)) :z (* (:z a) (:z b))})
(defn v3-divide [a b] {:x (/ (:x a) (:x b)) :y (/ (:y a) (:y b)) :z (/ (:z a) (:z b))})
(defn v3-negate [v] {:x (- (:x v)) :y (- (:y v)) :z (- (:z v))})

(defn v3-length-sqr [v] (+ (* (:x v) (:x v)) (* (:y v) (:y v)) (* (:z v) (:z v))))
(defn v3-length [v] (Math/sqrt (v3-length-sqr v)))
(defn v3-dot [a b] (+ (* (:x a) (:x b)) (* (:y a) (:y b)) (* (:z a) (:z b))))

(defn v3-cross [a b]
  {:x (- (* (:y a) (:z b)) (* (:z a) (:y b)))
   :y (- (* (:z a) (:x b)) (* (:x a) (:z b)))
   :z (- (* (:x a) (:y b)) (* (:y a) (:x b)))})

(defn v3-distance-sqr [a b]
  (let [dx (- (:x b) (:x a)) dy (- (:y b) (:y a)) dz (- (:z b) (:z a))]
    (+ (* dx dx) (* dy dy) (* dz dz))))

(defn v3-distance [a b] (Math/sqrt (v3-distance-sqr a b)))

(defn v3-normalize [v]
  (let [len (v3-length v)]
    (if (zero? len) {:x 0.0 :y 0.0 :z 0.0} (v3-scale v (/ 1.0 len)))))

(defn v3-angle
  "Unsigned angle in radians between `a` and `b`, via atan2 of the cross
   product's length against the dot - stabler near 0 and pi than acos."
  [a b]
  (Math/atan2 (v3-length (v3-cross a b)) (v3-dot a b)))

(defn v3-lerp [a b amount]
  {:x (lerp (:x a) (:x b) amount)
   :y (lerp (:y a) (:y b) amount)
   :z (lerp (:z a) (:z b) amount)})

(defn v3-reflect [v normal]
  (let [d (v3-dot v normal)]
    {:x (- (:x v) (* 2.0 (:x normal) d))
     :y (- (:y v) (* 2.0 (:y normal) d))
     :z (- (:z v) (* 2.0 (:z normal) d))}))

(defn v3-min [a b]
  {:x (Math/min (:x a) (:x b)) :y (Math/min (:y a) (:y b)) :z (Math/min (:z a) (:z b))})

(defn v3-max [a b]
  {:x (Math/max (:x a) (:x b)) :y (Math/max (:y a) (:y b)) :z (Math/max (:z a) (:z b))})

(defn v3-clamp [v mn mx]
  {:x (clamp (:x v) (:x mn) (:x mx))
   :y (clamp (:y v) (:y mn) (:y mx))
   :z (clamp (:z v) (:z mn) (:z mx))})

(defn v3-equals? [a b]
  (and (float-equals? (:x a) (:x b))
       (float-equals? (:y a) (:y b))
       (float-equals? (:z a) (:z b))))
