(ns examples.games.floppy
  "Classic game: Floppy (Flappy Bird clone)
   
   Original game by Ian Eito, Albert Martos and Ramon Santamaria.
   Ported to Clojure for raylib-clojure-playground.
   
   Complexity: ⭐⭐ (2/4)
   
   Controls:
   - SPACE: Fly up
   - P: Pause game
   - ENTER: Restart when game over
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [raylib-ext :as ext]))

(def WIDTH 800)
(def HEIGHT 450)
(def MAX-TUBES 100)
(def FLOPPY-RADIUS 24)
(def TUBES-WIDTH 80)

(defn random-value [min-val max-val]
  (+ min-val (rand-int (- max-val min-val -1))))

(defn create-tubes []
  (let [tube-positions (vec (for [i (range MAX-TUBES)]
                              {:x (+ 400 (* 280 i))
                               :y (- (random-value 0 120))}))]
    {:positions tube-positions
     :tubes (vec (for [i (range MAX-TUBES)]
                   (let [pos (nth tube-positions i)]
                     {:top {:x (:x pos)
                            :y (:y pos)
                            :width TUBES-WIDTH
                            :height 255}
                      :bottom {:x (:x pos)
                               :y (- (+ 600 (:y pos)) 255)
                               :width TUBES-WIDTH
                               :height 255}
                      :active? true})))}))

(defn initial-state []
  (let [tubes-data (create-tubes)]
    {:exit? false
     :game-over? false
     :paused? false
     :score 0
     :hi-score 0
     :superfx? false
     :floppy {:x 80
              :y (- (/ HEIGHT 2) FLOPPY-RADIUS)
              :radius FLOPPY-RADIUS}
     :tubes-speed 2
     :tube-positions (:positions tubes-data)
     :tubes (:tubes tubes-data)}))

(def game-atom (atom (initial-state)))

;; Persist hi-score across game resets
(def hi-score-atom (atom 0))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "classic game: floppy")
  (rct/set-target-fps! 60))

(defn check-collision-circle-rec [cx cy radius rect]
  ;; Simple AABB collision for circle vs rectangle
  (let [{:keys [x y width height]} rect
        ;; Find closest point on rectangle to circle center
        closest-x (max x (min cx (+ x width)))
        closest-y (max y (min cy (+ y height)))
        ;; Calculate distance
        dx (- cx closest-x)
        dy (- cy closest-y)
        dist-sq (+ (* dx dx) (* dy dy))]
    (< dist-sq (* radius radius))))

(defn handle-input [{:keys [game-over? paused?]
                     :as game}]
  (cond-> game
    (rck/is-key-pressed? (:q enums/keyboard-key))
    (assoc :exit? true)

    (and game-over? (rck/is-key-pressed? (:enter enums/keyboard-key)))
    (-> (merge (initial-state))
        (assoc :hi-score @hi-score-atom))

    (and (not game-over?) (rck/is-key-pressed? (:p enums/keyboard-key)))
    (update :paused? not)))

(defn update-floppy [{:keys [floppy game-over?]
                      :as game}]
  (if game-over?
    game
    (let [dy (if (rck/is-key-down? (:space enums/keyboard-key))
               -3
               1)
          new-y (+ (:y floppy) dy)]
      (assoc-in game [:floppy :y] new-y))))

(defn update-tubes [{:keys [tubes-speed tube-positions tubes]
                     :as game}]
  ;; Move tubes left
  (let [new-positions (mapv (fn [pos] (update pos :x - tubes-speed)) tube-positions)
        new-tubes (mapv (fn [tube pos]
                          (-> tube
                              (assoc-in [:top :x] (:x pos))
                              (assoc-in [:bottom :x] (:x pos))))
                        tubes new-positions)]
    (assoc game
           :tube-positions new-positions
           :tubes new-tubes)))

(defn check-collisions [{:keys [floppy tubes]
                         :as game}]
  (let [{:keys [x y radius]} floppy
        ;; Check if bird hits any tube
        collision? (some (fn [tube]
                           (or (check-collision-circle-rec x y radius (:top tube))
                               (check-collision-circle-rec x y radius (:bottom tube))))
                         tubes)
        ;; Check if bird is out of bounds
        out-of-bounds? (or (< y 0) (> y HEIGHT))]
    (if (or collision? out-of-bounds?)
      (assoc game :game-over? true)
      game)))

(defn update-score [{:keys [floppy tubes score hi-score]
                     :as game}]
  (let [floppy-x (:x floppy)]
    (loop [i 0
           current-tubes tubes
           current-score score
           superfx? false]
      (if (>= i (count current-tubes))
        (let [new-hi-score (max current-score hi-score)]
          (reset! hi-score-atom new-hi-score)
          (assoc game
                 :tubes current-tubes
                 :score current-score
                 :hi-score new-hi-score
                 :superfx? superfx?))
        (let [tube (nth current-tubes i)
              tube-x (get-in tube [:top :x])]
          (if (and (< tube-x floppy-x) (:active? tube))
            (recur (inc i)
                   (assoc-in current-tubes [i :active?] false)
                   (+ current-score 100)
                   true)
            (recur (inc i) current-tubes current-score superfx?)))))))

(defn tick [{:keys [game-over? paused?]
             :as game}]
  (let [game (handle-input game)]
    (if (or game-over? paused?)
      game
      (-> game
          update-floppy
          update-tubes
          check-collisions
          update-score))))

(defn draw [{:keys [game-over? paused? floppy tubes score hi-score superfx?]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (if game-over?
    (let [text "PRESS [ENTER] TO PLAY AGAIN"
          text-width (ext/measure-text text 20)]
      (rtd/draw-text! text
                      (- (quot WIDTH 2) (quot text-width 2))
                      (- (quot HEIGHT 2) 50)
                      20
                      colors/gray))
    (do
      ;; Draw floppy bird
      (rsb/draw-circle-v! {:x (:x floppy)
                           :y (:y floppy)} (float (:radius floppy)) colors/darkgray)

      ;; Draw tubes
      (doseq [tube tubes]
        (let [{:keys [x y width height]} (:top tube)]
          (rsb/draw-rectangle! (int x) (int y) (int width) (int height) colors/gray))
        (let [{:keys [x y width height]} (:bottom tube)]
          (rsb/draw-rectangle! (int x) (int y) (int width) (int height) colors/gray)))

      ;; Flash effect when scoring
      (when superfx?
        (rsb/draw-rectangle! 0 0 WIDTH HEIGHT colors/white))

      ;; Score display
      (rtd/draw-text! (format "%04d" score) 20 20 40 colors/gray)
      (rtd/draw-text! (format "HI-SCORE: %04d" hi-score) 20 70 20 colors/lightgray)

      ;; Pause overlay
      (when paused?
        (let [text "GAME PAUSED"
              text-width (ext/measure-text text 40)]
          (rtd/draw-text! text
                          (- (quot WIDTH 2) (quot text-width 2))
                          (- (quot HEIGHT 2) 40)
                          40
                          colors/gray)))))

  (rtd/draw-fps! 10 (- HEIGHT 25))
  (rcd/end-drawing!))

(defn start []
  (nrepl/start {:port 7888})
  (init)
  (loop []
    (let [game (tick @game-atom)]
      (when-not (or (:exit? game) (rcw/window-should-close?))
        (reset! game-atom game)
        (draw game)
        (recur))))
  (rcw/close-window!))

(defn -main [& _args]
  (start))

(comment
  @game-atom
  (swap! game-atom assoc :paused? true))
