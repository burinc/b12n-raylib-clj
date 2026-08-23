(ns examples.recursive-tree
  "raylib [shapes] example - recursive tree

   A binary tree grown by repeatedly splitting each branch in two, with the
   split angle, length, decay rate, depth and thickness on sliders.

   The C grows the tree with a fixed `Branch branches[1030]` array and a
   loop whose bound moves as it appends - it walks index `i` up while
   `count` grows ahead of it, so it is a breadth-first expansion written as
   a single pass. `branches` below keeps exactly that shape, because the
   ORDER matters: the `count < maxBranches` guard is checked against the
   running total, so which branches get children depends on how far the
   expansion has already got. A depth-first or lazy formulation would grow a
   visibly different tree.

   The same guard is why the branch count can exceed `maxBranches`: it is
   tested before appending TWO children, so the last accepted split
   overshoots by one. Kept, since it is what the C draws.

   Difficulty: 3/4
   Based on: shapes/shapes_recursive_tree.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.raygui :as gui]
   [raylib.colors :as colors]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)

(def min-branch-length
  "Below this a branch is neither drawn nor split, matching the C's `< 2`."
  2.0)

(defn- tip
  "Where a branch of `length` leaving `start` at `angle` radians ends.
   Screen y grows downward, hence the minus on the cosine."
  [start angle length]
  {:x (+ (:x start) (* length (Math/sin angle)))
   :y (- (:y start) (* length (Math/cos angle)))})

(defn branches
  "Grow the tree, returning branches in the C's expansion order.

   Pure, so the geometry is checkable without a window."
  [start length angle-degrees decay tree-depth]
  (let [theta (Math/toRadians angle-degrees)
        max-branches (long (Math/pow 2 (Math/floor tree-depth)))
        root {:start start :end (tip start 0.0 length) :angle 0.0 :length length}]
    (loop [acc [root] i 0]
      (if (>= i (count acc))
        acc
        (let [{:keys [end angle length] :as b} (nth acc i)
              next-length (* length decay)]
          (if (or (< (:length b) min-branch-length)
                  (>= (count acc) max-branches)
                  (< next-length min-branch-length))
            (recur acc (inc i))
            (let [a1 (+ angle theta) a2 (- angle theta)]
              (recur (conj acc
                           {:start end :end (tip end a1 next-length) :angle a1 :length next-length}
                           {:start end :end (tip end a2 next-length) :angle a2 :length next-length})
                     (inc i)))))))))

(defn initial-state []
  {:angle 40.0 :thick 1.0 :tree-depth 10.0 :branch-decay 0.66
   :length 120.0 :bezier? false})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - recursive tree")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [state]
  (debug-stats/update!)
  state)

(defn draw [{:keys [angle thick tree-depth branch-decay length bezier?] :as state}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (let [start {:x (- (/ screen-width 2.0) 125.0) :y (double screen-height)}
        draw-branch (if bezier? rsb/draw-line-bezier! rsb/draw-line-ex!)]
    (doseq [{:keys [start end length]} (branches start length angle branch-decay tree-depth)
            :when (>= length min-branch-length)]
      (draw-branch {:x (float (:x start)) :y (float (:y start))}
                   {:x (float (:x end)) :y (float (:y end))}
                   (float thick) colors/red)))

  (rsb/draw-line! 580 0 580 (rcw/get-screen-height) {:r 218 :g 218 :b 218 :a 255})
  (rsb/draw-rectangle! 580 0 (rcw/get-screen-width) (rcw/get-screen-height)
                       {:r 232 :g 232 :b 232 :a 255})

  (let [bar (fn [y label fmt v mn mx]
              (gui/slider-bar {:x 640.0 :y (double y) :width 120.0 :height 20.0}
                              label (format fmt v) v mn mx))
        angle (bar 40 "Angle" "%.0f" angle 0.0 180.0)
        length (bar 70 "Length" "%.0f" length 12.0 240.0)
        branch-decay (bar 100 "Decay" "%.2f" branch-decay 0.1 0.78)
        tree-depth (bar 130 "Depth" "%.0f" tree-depth 1.0 10.0)
        thick (bar 160 "Thick" "%.0f" thick 1.0 8.0)
        bezier? (gui/check-box {:x 640.0 :y 190.0 :width 20.0 :height 20.0} "Bezier" bezier?)]
    (rtd/draw-fps! 10 10)
    (debug-stats/draw!)
    (rcd/end-drawing!)
    (assoc state :angle angle :length length :branch-decay branch-decay
           :tree-depth tree-depth :thick thick :bezier? bezier?)))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (when-not (rcw/window-should-close?)
      (swap! game-atom (comp draw tick))
      (recur)))
  (rcw/close-window!))
