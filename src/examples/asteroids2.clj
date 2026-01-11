(ns examples.asteroids2
  "Classic asteroids game - converted from raylib C example
   Original: https://github.com/raysan5/raylib-games/blob/master/classics/src/asteroids.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.nrepl :as nrepl]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib-ext :as ext]
   [debug-stats])
  (:gen-class))

;; DEFINES ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def SCREEN_WIDTH 800)
(def SCREEN_HEIGHT 450)

(def PLAYER_BASE_SIZE 20.0)
(def PLAYER_SPEED 6.0)
(def PLAYER_MAX_SHOOTS 10)

(def METEORS_SPEED 2)
(def MAX_BIG_METEORS 4)
(def MAX_MEDIUM_METEORS 8)
(def MAX_SMALL_METEORS 16)

;; MATH HELPERS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def DEG2RAD (/ Math/PI 180.0))

(defn get-random-value [min max]
  (+ min (rand-int (inc (- max min)))))

;; GAME STATE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-player []
  (let [ship-height (/ (/ PLAYER_BASE_SIZE 2) (Math/tan (* 20 DEG2RAD)))
        pos-x (/ SCREEN_WIDTH 2)
        pos-y (- (/ SCREEN_HEIGHT 2) (/ ship-height 2))
        rotation 0.0
        collider-x (+ pos-x (* (Math/sin (* rotation DEG2RAD)) (/ ship-height 2.5)))
        collider-y (- pos-y (* (Math/cos (* rotation DEG2RAD)) (/ ship-height 2.5)))]
    {:position {:x pos-x
                :y pos-y}
     :speed {:x 0.0
             :y 0.0}
     :acceleration 0.0
     :rotation rotation
     :collider {:x collider-x
                :y collider-y
                :z 12.0}
     :color colors/lightgray
     :ship-height ship-height}))

(defn make-shoot []
  {:position {:x 0.0
              :y 0.0}
   :speed {:x 0.0
           :y 0.0}
   :radius 2.0
   :rotation 0.0
   :life-spawn 0
   :active false
   :color colors/white})

(defn make-meteor [x y radius active]
  {:position {:x (float x)
              :y (float y)}
   :speed {:x 0.0
           :y 0.0}
   :radius (float radius)
   :active active
   :color colors/blue})

(defn init-game []
  (let [player (make-player)
        shoots (vec (repeatedly PLAYER_MAX_SHOOTS make-shoot))

        ;; Initialize big meteors
        big-meteors (vec
                     (for [_ (range MAX_BIG_METEORS)]
                       (let [;; Find posx outside center zone
                             posx (loop [px (get-random-value 0 SCREEN_WIDTH)]
                                    (if (and (> px (- (/ SCREEN_WIDTH 2) 150))
                                             (< px (+ (/ SCREEN_WIDTH 2) 150)))
                                      (recur (get-random-value 0 SCREEN_WIDTH))
                                      px))
                             ;; Find posy outside center zone
                             posy (loop [py (get-random-value 0 SCREEN_HEIGHT)]
                                    (if (and (> py (- (/ SCREEN_HEIGHT 2) 150))
                                             (< py (+ (/ SCREEN_HEIGHT 2) 150)))
                                      (recur (get-random-value 0 SCREEN_HEIGHT))
                                      py))
                             ;; Find non-zero velocity
                             [velx vely] (loop [vx (get-random-value (- METEORS_SPEED) METEORS_SPEED)
                                                vy (get-random-value (- METEORS_SPEED) METEORS_SPEED)]
                                           (if (and (= vx 0) (= vy 0))
                                             (recur (get-random-value (- METEORS_SPEED) METEORS_SPEED)
                                                    (get-random-value (- METEORS_SPEED) METEORS_SPEED))
                                             [vx vy]))]
                         {:position {:x (float posx)
                                     :y (float posy)}
                          :speed {:x (float velx)
                                  :y (float vely)}
                          :radius 40.0
                          :active true
                          :color colors/blue})))

        medium-meteors (vec (repeatedly MAX_MEDIUM_METEORS #(make-meteor -100 -100 20 false)))
        small-meteors (vec (repeatedly MAX_SMALL_METEORS #(make-meteor -100 -100 10 false)))]

    {:game-over false
     :pause false
     :victory false
     :player player
     :shoots shoots
     :big-meteors big-meteors
     :medium-meteors medium-meteors
     :small-meteors small-meteors
     :mid-meteors-count 0
     :small-meteors-count 0
     :destroyed-meteors-count 0}))

;; UPDATE GAME ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-player-rotation [player]
  (cond-> player
    (rck/is-key-down? (:left enums/keyboard-key))
    (update :rotation #(- % 5))

    (rck/is-key-down? (:right enums/keyboard-key))
    (update :rotation #(+ % 5))))

(defn update-player-speed [player]
  (assoc player
         :speed {:x (* (Math/sin (* (:rotation player) DEG2RAD)) PLAYER_SPEED)
                 :y (* (Math/cos (* (:rotation player) DEG2RAD)) PLAYER_SPEED)}))

(defn update-player-acceleration [player]
  (let [acc (:acceleration player)]
    (cond
      (rck/is-key-down? (:up enums/keyboard-key))
      (assoc player :acceleration (if (< acc 1) (+ acc 0.04) acc))

      (rck/is-key-down? (:down enums/keyboard-key))
      (assoc player :acceleration (if (> acc 0) (- acc 0.04) 0))

      :else
      (assoc player :acceleration
             (cond
               (> acc 0) (- acc 0.02)
               (< acc 0) 0
               :else acc)))))

(defn update-player-position [player]
  (let [pos (:position player)
        speed (:speed player)
        acc (:acceleration player)
        ship-height (:ship-height player)
        new-x (+ (:x pos) (* (:x speed) acc))
        new-y (- (:y pos) (* (:y speed) acc))

        ;; Wall wrapping
        wrapped-x (cond
                    (> new-x (+ SCREEN_WIDTH ship-height)) (- ship-height)
                    (< new-x (- ship-height)) (+ SCREEN_WIDTH ship-height)
                    :else new-x)
        wrapped-y (cond
                    (> new-y (+ SCREEN_HEIGHT ship-height)) (- ship-height)
                    (< new-y (- ship-height)) (+ SCREEN_HEIGHT ship-height)
                    :else new-y)]
    (assoc player :position {:x wrapped-x
                             :y wrapped-y})))

(defn update-player-collider [player]
  (let [pos (:position player)
        rot (:rotation player)
        ship-height (:ship-height player)
        collider-x (+ (:x pos) (* (Math/sin (* rot DEG2RAD)) (/ ship-height 2.5)))
        collider-y (- (:y pos) (* (Math/cos (* rot DEG2RAD)) (/ ship-height 2.5)))]
    (assoc player :collider {:x collider-x
                             :y collider-y
                             :z 12.0})))

(defn update-player [player]
  (-> player
      update-player-rotation
      update-player-speed
      update-player-acceleration
      update-player-position
      update-player-collider))

(defn fire-shoot [shoots player]
  (let [ship-height (:ship-height player)
        player-pos (:position player)
        player-rot (:rotation player)]
    (if-let [idx (first (keep-indexed #(when (not (:active %2)) %1) shoots))]
      (assoc-in shoots [idx]
                {:position {:x (+ (:x player-pos) (* (Math/sin (* player-rot DEG2RAD)) ship-height))
                            :y (- (:y player-pos) (* (Math/cos (* player-rot DEG2RAD)) ship-height))}
                 :active true
                 :speed {:x (* 1.5 (Math/sin (* player-rot DEG2RAD)) PLAYER_SPEED)
                         :y (* 1.5 (Math/cos (* player-rot DEG2RAD)) PLAYER_SPEED)}
                 :rotation player-rot
                 :radius 2.0
                 :life-spawn 0
                 :color colors/white})
      shoots)))

(defn update-shoot [shoot]
  (if (:active shoot)
    (let [pos (:position shoot)
          speed (:speed shoot)
          new-x (+ (:x pos) (:x speed))
          new-y (- (:y pos) (:y speed))
          radius (:radius shoot)
          life-spawn (:life-spawn shoot)

          ;; Check if out of bounds
          out-of-bounds? (or (> new-x (+ SCREEN_WIDTH radius))
                             (< new-x (- 0 radius))
                             (> new-y (+ SCREEN_HEIGHT radius))
                             (< new-y (- 0 radius)))

          ;; Check if life expired
          life-expired? (>= life-spawn 60)]

      (if (or out-of-bounds? life-expired?)
        (assoc shoot
               :position {:x 0.0
                          :y 0.0}
               :speed {:x 0.0
                       :y 0.0}
               :life-spawn 0
               :active false)
        (assoc shoot
               :position {:x new-x
                          :y new-y}
               :life-spawn (inc life-spawn))))
    shoot))

(defn update-shoots [shoots]
  (mapv update-shoot shoots))

(defn update-meteor-position [meteor]
  (if (:active meteor)
    (let [pos (:position meteor)
          speed (:speed meteor)
          radius (:radius meteor)
          new-x (+ (:x pos) (:x speed))
          new-y (+ (:y pos) (:y speed))

          ;; Wall wrapping
          wrapped-x (cond
                      (> new-x (+ SCREEN_WIDTH radius)) (- radius)
                      (< new-x (- 0 radius)) (+ SCREEN_WIDTH radius)
                      :else new-x)
          wrapped-y (cond
                      (> new-y (+ SCREEN_HEIGHT radius)) (- radius)
                      (< new-y (- 0 radius)) (+ SCREEN_HEIGHT radius)
                      :else new-y)]
      (assoc meteor :position {:x wrapped-x
                               :y wrapped-y}))
    meteor))

(defn update-meteors [meteors]
  (mapv update-meteor-position meteors))

(defn check-collision-circles [pos1 radius1 pos2 radius2]
  (let [result (ext/check-collision-circles? pos1 (float radius1) pos2 (float radius2))]
    ;; raylib returns byte (0 or non-zero), convert to boolean
    (if (boolean? result)
      result
      (not (zero? result)))))

(defn check-player-meteor-collision [player meteors]
  (let [collider (:collider player)
        collider-pos {:x (:x collider)
                      :y (:y collider)}
        collider-radius (:z collider)]
    (some (fn [meteor]
            (and (:active meteor)
                 (check-collision-circles collider-pos collider-radius
                                          (:position meteor) (:radius meteor))))
          meteors)))

(defn handle-shoot-meteor-collisions [game]
  (let [{:keys [shoots big-meteors medium-meteors small-meteors
                mid-meteors-count small-meteors-count destroyed-meteors-count]} game]
    (loop [shoots shoots
           big-meteors big-meteors
           medium-meteors medium-meteors
           small-meteors small-meteors
           mid-count mid-meteors-count
           small-count small-meteors-count
           destroyed destroyed-meteors-count
           shoot-idx 0]

      (if (>= shoot-idx (count shoots))
        ;; Done processing all shoots
        (assoc game
               :shoots shoots
               :big-meteors big-meteors
               :medium-meteors medium-meteors
               :small-meteors small-meteors
               :mid-meteors-count mid-count
               :small-meteors-count small-count
               :destroyed-meteors-count destroyed)

        (let [shoot (nth shoots shoot-idx)]
          (if-not (:active shoot)
            (recur shoots big-meteors medium-meteors small-meteors
                   mid-count small-count destroyed (inc shoot-idx))

            ;; Check collision with big meteors
            (if-let [big-idx (first (keep-indexed
                                     (fn [idx meteor]
                                       (when (and (:active meteor)
                                                  (check-collision-circles (:position shoot) (:radius shoot)
                                                                           (:position meteor) (:radius meteor)))
                                         idx))
                                     big-meteors))]
              (let [meteor (nth big-meteors big-idx)
                    new-shoot (assoc shoot :active false :life-spawn 0)
                    new-big-meteor (assoc meteor :active false :color colors/red)
                    ;; Create 2 medium meteors
                    medium1-idx mid-count
                    medium2-idx (inc mid-count)
                    medium1 (assoc (nth medium-meteors medium1-idx)
                                   :position (:position meteor)
                                   :speed (if (even? mid-count)
                                            {:x (* (Math/cos (* (:rotation shoot) DEG2RAD)) METEORS_SPEED -1)
                                             :y (* (Math/sin (* (:rotation shoot) DEG2RAD)) METEORS_SPEED -1)}
                                            {:x (* (Math/cos (* (:rotation shoot) DEG2RAD)) METEORS_SPEED)
                                             :y (* (Math/sin (* (:rotation shoot) DEG2RAD)) METEORS_SPEED)})
                                   :active true)
                    medium2 (assoc (nth medium-meteors medium2-idx)
                                   :position (:position meteor)
                                   :speed (if (odd? (inc mid-count))
                                            {:x (* (Math/cos (* (:rotation shoot) DEG2RAD)) METEORS_SPEED -1)
                                             :y (* (Math/sin (* (:rotation shoot) DEG2RAD)) METEORS_SPEED -1)}
                                            {:x (* (Math/cos (* (:rotation shoot) DEG2RAD)) METEORS_SPEED)
                                             :y (* (Math/sin (* (:rotation shoot) DEG2RAD)) METEORS_SPEED)})
                                   :active true)]
                (recur (assoc shoots shoot-idx new-shoot)
                       (assoc big-meteors big-idx new-big-meteor)
                       (-> medium-meteors
                           (assoc medium1-idx medium1)
                           (assoc medium2-idx medium2))
                       small-meteors
                       (+ mid-count 2)
                       small-count
                       (inc destroyed)
                       (inc shoot-idx)))

              ;; Check collision with medium meteors
              (if-let [med-idx (first (keep-indexed
                                       (fn [idx meteor]
                                         (when (and (:active meteor)
                                                    (check-collision-circles (:position shoot) (:radius shoot)
                                                                             (:position meteor) (:radius meteor)))
                                           idx))
                                       medium-meteors))]
                (let [meteor (nth medium-meteors med-idx)
                      new-shoot (assoc shoot :active false :life-spawn 0)
                      new-med-meteor (assoc meteor :active false :color colors/green)
                      ;; Create 2 small meteors
                      small1-idx small-count
                      small2-idx (inc small-count)
                      small1 (assoc (nth small-meteors small1-idx)
                                    :position (:position meteor)
                                    :speed (if (even? small-count)
                                             {:x (* (Math/cos (* (:rotation shoot) DEG2RAD)) METEORS_SPEED -1)
                                              :y (* (Math/sin (* (:rotation shoot) DEG2RAD)) METEORS_SPEED -1)}
                                             {:x (* (Math/cos (* (:rotation shoot) DEG2RAD)) METEORS_SPEED)
                                              :y (* (Math/sin (* (:rotation shoot) DEG2RAD)) METEORS_SPEED)})
                                    :active true)
                      small2 (assoc (nth small-meteors small2-idx)
                                    :position (:position meteor)
                                    :speed (if (odd? (inc small-count))
                                             {:x (* (Math/cos (* (:rotation shoot) DEG2RAD)) METEORS_SPEED -1)
                                              :y (* (Math/sin (* (:rotation shoot) DEG2RAD)) METEORS_SPEED -1)}
                                             {:x (* (Math/cos (* (:rotation shoot) DEG2RAD)) METEORS_SPEED)
                                              :y (* (Math/sin (* (:rotation shoot) DEG2RAD)) METEORS_SPEED)})
                                    :active true)]
                  (recur (assoc shoots shoot-idx new-shoot)
                         big-meteors
                         (assoc medium-meteors med-idx new-med-meteor)
                         (-> small-meteors
                             (assoc small1-idx small1)
                             (assoc small2-idx small2))
                         mid-count
                         (+ small-count 2)
                         (inc destroyed)
                         (inc shoot-idx)))

                ;; Check collision with small meteors
                (if-let [small-idx (first (keep-indexed
                                           (fn [idx meteor]
                                             (when (and (:active meteor)
                                                        (check-collision-circles (:position shoot) (:radius shoot)
                                                                                 (:position meteor) (:radius meteor)))
                                               idx))
                                           small-meteors))]
                  (let [meteor (nth small-meteors small-idx)
                        new-shoot (assoc shoot :active false :life-spawn 0)
                        new-small-meteor (assoc meteor :active false :color colors/yellow)]
                    (recur (assoc shoots shoot-idx new-shoot)
                           big-meteors
                           medium-meteors
                           (assoc small-meteors small-idx new-small-meteor)
                           mid-count
                           small-count
                           (inc destroyed)
                           (inc shoot-idx)))

                  ;; No collision, continue to next shoot
                  (recur shoots big-meteors medium-meteors small-meteors
                         mid-count small-count destroyed (inc shoot-idx)))))))))))

(defn update-game [game]
  ;; Update debug stats (handles F1 toggle)
  (debug-stats/update!)

  ;; Handle F11 fullscreen toggle
  (when (rck/is-key-pressed? (:f11 enums/keyboard-key))
    (rcw/toggle-borderless-windowed!))

  (if (:game-over game)
    ;; Game over - check for restart
    (if (rck/is-key-pressed? (:enter enums/keyboard-key))
      (init-game)
      game)

    ;; Game running
    (let [game (if (rck/is-key-pressed? (:p enums/keyboard-key))
                 (update game :pause not)
                 game)]
      (if (:pause game)
        game
        (let [;; Update player
              game (update game :player update-player)

              ;; Handle shooting
              game (if (rck/is-key-pressed? (:space enums/keyboard-key))
                     (update game :shoots fire-shoot (:player game))
                     game)

              ;; Update shoots
              game (update game :shoots update-shoots)

              ;; Update meteors
              game (-> game
                       (update :big-meteors update-meteors)
                       (update :medium-meteors update-meteors)
                       (update :small-meteors update-meteors))

              ;; Check player collision with meteors
              player-hit? (or (check-player-meteor-collision (:player game) (:big-meteors game))
                              (check-player-meteor-collision (:player game) (:medium-meteors game))
                              (check-player-meteor-collision (:player game) (:small-meteors game)))

              game (if player-hit?
                     (assoc game :game-over true)
                     game)

              ;; Handle shoot-meteor collisions
              game (handle-shoot-meteor-collisions game)

              ;; Check victory
              total-meteors (+ MAX_BIG_METEORS MAX_MEDIUM_METEORS MAX_SMALL_METEORS)
              game (if (= (:destroyed-meteors-count game) total-meteors)
                     (assoc game :victory true)
                     game)

              ;; Update custom debug stats
              _ (do
                  (debug-stats/set-custom-stat! :big-meteors (count (filter :active (:big-meteors game))))
                  (debug-stats/set-custom-stat! :med-meteors (count (filter :active (:medium-meteors game))))
                  (debug-stats/set-custom-stat! :small-meteors (count (filter :active (:small-meteors game))))
                  (debug-stats/set-custom-stat! :shoots (count (filter :active (:shoots game))))
                  (debug-stats/set-custom-stat! :destroyed (:destroyed-meteors-count game)))]
          game)))))

;; DRAW GAME ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-spaceship [player]
  (let [pos (:position player)
        rot (:rotation player)
        ship-height (:ship-height player)
        v1 {:x (+ (:x pos) (* (Math/sin (* rot DEG2RAD)) ship-height))
            :y (- (:y pos) (* (Math/cos (* rot DEG2RAD)) ship-height))}
        v2 {:x (- (:x pos) (* (Math/cos (* rot DEG2RAD)) (/ PLAYER_BASE_SIZE 2)))
            :y (- (:y pos) (* (Math/sin (* rot DEG2RAD)) (/ PLAYER_BASE_SIZE 2)))}
        v3 {:x (+ (:x pos) (* (Math/cos (* rot DEG2RAD)) (/ PLAYER_BASE_SIZE 2)))
            :y (+ (:y pos) (* (Math/sin (* rot DEG2RAD)) (/ PLAYER_BASE_SIZE 2)))}]
    (ext/draw-triangle! v1 v2 v3 colors/maroon)))

(defn draw-meteors [meteors active-color]
  (doseq [meteor meteors]
    (let [pos (:position meteor)
          x (int (:x pos))
          y (int (:y pos))
          radius (:radius meteor)]
      (if (:active meteor)
        (ext/draw-circle! x y radius active-color)
        ;; Faded lightgray (0.3 alpha)
        (ext/draw-circle! x y radius {:r 200
                                      :g 200
                                      :b 200
                                      :a 76})))))

(defn draw-shoots [shoots]
  (doseq [shoot shoots]
    (when (:active shoot)
      (let [pos (:position shoot)
            x (int (:x pos))
            y (int (:y pos))
            radius (:radius shoot)]
        (ext/draw-circle! x y radius colors/black)))))

(defn draw-game [game]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  (when game  ; Only draw if game state exists
    (if-not (:game-over game)
      (do
        ;; Draw spaceship
        (draw-spaceship (:player game))

        ;; Draw meteors
        (draw-meteors (:big-meteors game) colors/darkgray)
        (draw-meteors (:medium-meteors game) colors/gray)
        (draw-meteors (:small-meteors game) colors/gray)

        ;; Draw shoots
        (draw-shoots (:shoots game))

        ;; Draw victory message
        (when (:victory game)
          (let [text "VICTORY"
                size 20
                width (ext/measure-text text size)]
            (rtd/draw-text! text (int (- (/ SCREEN_WIDTH 2) (/ width 2))) (int (/ SCREEN_HEIGHT 2)) size colors/lightgray)))

        ;; Draw pause message
        (when (:pause game)
          (let [text "GAME PAUSED"
                size 40
                width (ext/measure-text text size)]
            (rtd/draw-text! text (int (- (/ SCREEN_WIDTH 2) (/ width 2))) (int (- (/ SCREEN_HEIGHT 2) 40)) size colors/gray)))

        ;; Draw controls info
        (rtd/draw-text! "F1 debug stats | F11 fullscreen | P pause" 10 (- SCREEN_HEIGHT 25) 12 colors/darkgray)

        ;; Draw debug stats overlay (F1 to toggle)
        (debug-stats/draw!))

      ;; Game over screen
      (do
        (let [text "PRESS [ENTER] TO PLAY AGAIN"
              size 20
              width (ext/measure-text text size)]
          (rtd/draw-text! text (int (- (/ (rcw/get-screen-width) 2) (/ width 2)))
                          (int (- (/ (rcw/get-screen-height) 2) 50)) size colors/gray))
        ;; Draw debug stats even on game over screen
        (debug-stats/draw!))))

  (rcd/end-drawing!))

;; MAIN ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def game-state (atom nil))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (rcw/set-config-flags! (bit-or (:window-resizable enums/config-flag)
                                 (:vsync-hint enums/config-flag)))
  (rcw/init-window! SCREEN_WIDTH SCREEN_HEIGHT "classic game: asteroids")

;;   (rct/set-target-fps! 60)

  ;; Enable debug stats - press F1 to toggle
  (debug-stats/enable!)

  (reset! game-state (init-game))

  (loop []
    (when-not (rcw/window-should-close?)
      (swap! game-state update-game)
      (draw-game @game-state)
      (recur)))

  (rcw/close-window!))

(comment
  (future (-main))
  ;;
  )
