(ns examples.simple-particles
  "raylib [shapes] example - simple particles

   Particle system with water, smoke, and fire effects.
   Click to move emitter, arrow keys to change rate/type.

   Difficulty: 2/4
   Based on: shapes/shapes_simple_particles.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.utils :as ru]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

(def screen-width 800)
(def screen-height 450)
(def MAX-PARTICLES 3000)

(def particle-types [:water :smoke :fire])
(def type-names {:water "WATER" :smoke "SMOKE" :fire "FIRE"})

(defn- emit-particle [emitter-pos ptype]
  (let [speed (/ (rand-int 10) 5.0)
        direction (rand-int 360)
        rad (* direction (/ Math/PI 180.0))
        [radius color speed] (case ptype
                               :water [5.0 colors/blue speed]
                               :smoke [7.0 colors/gray speed]
                               :fire  [10.0 colors/yellow (/ speed 10.0)])]
    {:pos emitter-pos
     :vel {:x (* speed (Math/cos rad)) :y (* speed (Math/sin rad))}
     :radius radius
     :color color
     :type ptype
     :lifetime 0.0
     :alive true}))

(defn- update-particle [{:keys [pos vel radius color type lifetime] :as p}]
  (let [lifetime (+ lifetime (/ 1.0 60.0))]
    (case type
      :water
      (let [vy (+ (:y vel) 0.2)]
        (assoc p
               :pos {:x (+ (:x pos) (:x vel)) :y (+ (:y pos) vy)}
               :vel (assoc vel :y vy)
               :lifetime lifetime))
      :smoke
      (let [vy (- (:y vel) 0.05)
            new-radius (+ radius 0.5)
            new-alpha (max 0 (- (:a color) 4))]
        (assoc p
               :pos {:x (+ (:x pos) (:x vel)) :y (+ (:y pos) vy)}
               :vel (assoc vel :y vy)
               :radius new-radius
               :color (assoc color :a new-alpha)
               :lifetime lifetime
               :alive (>= new-alpha 4)))
      :fire
      (let [vy (- (:y vel) 0.05)
            new-radius (- radius 0.15)
            new-g (max 0 (- (:g color) 3))]
        (assoc p
               :pos {:x (+ (:x pos) (:x vel) (Math/cos (* lifetime 215.0)))
                     :y (+ (:y pos) vy)}
               :vel (assoc vel :y vy)
               :radius new-radius
               :color (assoc color :g new-g)
               :lifetime lifetime
               :alive (> new-radius 0.02))))))

(defn- particle-in-bounds? [{:keys [pos radius]}]
  (let [{:keys [x y]} pos]
    (and (> x (- radius)) (< x (+ screen-width radius))
         (> y (- radius)) (< y (+ screen-height radius)))))

(defn initial-state []
  {:particles []
   :emission-rate -2
   :current-type :water
   :emitter-pos {:x (/ screen-width 2.0) :y (/ screen-height 2.0)}})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [shapes] example - simple particles")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [particles emission-rate current-type emitter-pos] :as state}]
  (debug-stats/update!)
  (let [;; Emit particles
        new-particles
        (if (neg? emission-rate)
          (if (zero? (rand-int (- emission-rate)))
            [(emit-particle emitter-pos current-type)]
            [])
          (vec (repeatedly (inc emission-rate) #(emit-particle emitter-pos current-type))))

        ;; Add new particles (cap at MAX-PARTICLES)
        particles (into particles (take (- MAX-PARTICLES (count particles)) new-particles))

        ;; Update particles
        particles (->> particles
                       (map update-particle)
                       (filter #(and (:alive %) (particle-in-bounds? %)))
                       vec)

        ;; Controls
        emission-rate (cond
                        (rck/is-key-pressed? (:up enums/keyboard-key)) (inc emission-rate)
                        (rck/is-key-pressed? (:down enums/keyboard-key)) (dec emission-rate)
                        :else emission-rate)

        type-idx (.indexOf particle-types current-type)
        current-type (cond
                       (rck/is-key-pressed? (:right enums/keyboard-key))
                       (nth particle-types (mod (inc type-idx) 3))
                       (rck/is-key-pressed? (:left enums/keyboard-key))
                       (nth particle-types (mod (+ type-idx 2) 3))
                       :else current-type)

        emitter-pos (if (rcm/is-mouse-button-down? (:left enums/mouse-button))
                      (rcm/get-mouse-position)
                      emitter-pos)]
    (assoc state
           :particles particles
           :emission-rate emission-rate
           :current-type current-type
           :emitter-pos emitter-pos)))

(defn draw [{:keys [particles emission-rate current-type]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; Draw particles
  (doseq [{:keys [pos radius color]} particles]
    (rsb/draw-circle-v! pos (float radius) color))

  ;; UI
  (rsb/draw-rectangle! 5 5 315 75 (ru/fade colors/skyblue (float 0.5)))
  (rsb/draw-rectangle-lines! 5 5 315 75 colors/blue)

  (rtd/draw-text! "CONTROLS:" 15 15 10 colors/black)
  (rtd/draw-text! "UP/DOWN: Change Particle Emission Rate" 15 35 10 colors/black)
  (rtd/draw-text! "LEFT/RIGHT: Change Particle Type (Water, Smoke, Fire)" 15 55 10 colors/black)

  (if (neg? emission-rate)
    (rtd/draw-text! (format "Particles every %d frames | Type: %s" (- emission-rate) (type-names current-type))
                    15 95 10 colors/darkgray)
    (rtd/draw-text! (format "%d Particles per frame | Type: %s" (inc emission-rate) (type-names current-type))
                    15 95 10 colors/darkgray))

  (rtd/draw-text! (format "PARTICLES: %d" (count particles)) (- screen-width 150) 10 10 colors/darkgray)

  (debug-stats/draw!)
  (rcd/end-drawing!))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (when-not (rcw/window-should-close?)
        (reset! game-atom game)
        (draw game)
        (recur))))
  (rcw/close-window!))
