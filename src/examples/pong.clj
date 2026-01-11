(ns examples.pong
  (:require
   [clojure.string :as string]
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib-ext :as ext]))

(def WIDTH 800)
(def HEIGHT 450)
(def PADDLE_HEIGHT 50)
(def PADDLE_WIDTH 10)
(def BALL_RADIUS 10)
(def TOP {:x 0
          :y 10
          :width WIDTH
          :height 1})
(def BOTTOM {:x 0
             :y (- HEIGHT 10)
             :width WIDTH
             :height 1})
(def PADDLE1_X 50)
(def PADDLE2_X 750)
(def MAX_POINTS 11)

(defn rand-direction []
  (* (quot 6 2) (rand-nth [1 -1])))

(defn initial-state []
  {:dt 0
   :time (System/nanoTime)
   :time-acc [1]
   :right 0
   :left 0
   :screen :title
   :paddle1 (- (quot 450 2) (quot PADDLE_HEIGHT 2))
   :paddle2 (- (quot 450 2) (quot PADDLE_HEIGHT 2))
   :ball [400 225 (rand-direction) (rand-direction)]})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "Raylib Clojure Pong")
  (rct/set-target-fps! 60))

(defn update-fps [game]
  (let [last-time (:time game)
        acc (:time-acc game)
        newtime (System/nanoTime)
        diff (- newtime last-time)
        newacc (vec (take-last 100 (conj acc diff)))
        average-diff (/ (reduce + newacc) (count newacc))
        average-fps (long (/ 1000000000 average-diff))]
    (-> game
        (assoc :dt (rct/get-frame-time))
        (assoc :time newtime)
        (assoc :time-acc acc)
        (assoc :avg-fps average-fps))))

(defn move-paddle [game up-key down-key paddle-key]
  (cond
    (rck/is-key-down? up-key) (update game paddle-key #(max (:y TOP) (+ % (- 6))))
    (rck/is-key-down? down-key) (update game paddle-key #(min (- (:y BOTTOM) PADDLE_HEIGHT) (+ % 6)))
    :else game))

(defn move-paddle1 [game]
  (move-paddle game (:w enums/keyboard-key) (:s enums/keyboard-key) :paddle1))

(defn move-paddle2 [game]
  (move-paddle game (:k enums/keyboard-key) (:j enums/keyboard-key) :paddle2))

(defn check-collision [x y radius rect]
  (let [result (ext/check-collision-circle-rec? {:x x
                                                 :y y} radius rect)]
    (if (boolean? result) result (pos? result))))

(defn move-ball [{:keys [ball paddle1 paddle2]
                  :as game}]
  (let [[x y dx dy] ball
        top? (check-collision x y BALL_RADIUS TOP)
        bottom? (check-collision x y BALL_RADIUS BOTTOM)
        paddle1-rect {:x PADDLE1_X
                      :y paddle1
                      :width PADDLE_WIDTH
                      :height PADDLE_HEIGHT}
        paddle2-rect {:x PADDLE2_X
                      :y paddle2
                      :width PADDLE_WIDTH
                      :height PADDLE_HEIGHT}
        paddle1? (and (check-collision x y BALL_RADIUS paddle1-rect)
                      (< x (+ PADDLE1_X PADDLE_WIDTH)))
        paddle2? (and (check-collision x y BALL_RADIUS paddle2-rect)
                      (> x (- PADDLE2_X PADDLE_WIDTH)))
        scored (if (and (not (or paddle1? paddle2?))
                        (not (or top? bottom?)))
                 (cond (< x 0) :right
                       (> x WIDTH) :left)
                 nil)
        [xx yy] (cond
                  (or paddle1? paddle2?) [(- dx) dy]
                  (or top? bottom?) [dx (- dy)]
                  :else [dx dy])]

    (assoc game :ball [(+ x xx) (+ y yy) xx yy]
           :scored scored)))

(defn handle-score [{:keys [scored]
                     :as game}]
  (if (nil? scored)
    game
    (-> game
        (update scored inc)
        (assoc :ball [400 225 (rand-direction) (rand-direction)]))))

(defn handle-endgame [{:keys [left right]
                       :as game}]
  (let [ended? (or (>= left MAX_POINTS) (>= right MAX_POINTS))]
    (if ended?
      (assoc game :screen :ending
             :winner (if (>= left MAX_POINTS) :left :right))
      game)))

(defn handle-start [game]
  (if (rck/is-key-pressed? (:enter enums/keyboard-key))
    (assoc game :screen :game)
    game))

(defn handle-restart [game]
  (if (rck/is-key-pressed? (:enter enums/keyboard-key))
    (initial-state)
    game))

(defn tick [{:keys [screen]
             :as game}]
  (condp = screen
    :title (-> game handle-start)
    :game (-> game
              move-ball
              move-paddle1
              move-paddle2
              handle-score
              handle-endgame)
    :ending (-> game handle-restart)))

(defn draw-title [_]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/black)
  (let [text "press ENTER to start"
        size 20
        width (ext/measure-text text size)]
    (rtd/draw-text! text (int (- (quot WIDTH 2) (/ width 2))) (int (/ HEIGHT 2)) size colors/white))
  (rcd/end-drawing!))

(defn draw-game [{:keys [avg-fps paddle1 paddle2 ball left right]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/black)
  (rsb/draw-rectangle! PADDLE1_X (int paddle1) PADDLE_WIDTH PADDLE_HEIGHT colors/blue)
  (rsb/draw-rectangle! PADDLE2_X (int paddle2) PADDLE_WIDTH PADDLE_HEIGHT colors/red)
  (rsb/draw-rectangle! (int (:x TOP)) (int (:y TOP)) (int (:width TOP)) (int (:height TOP)) colors/white)
  (rsb/draw-rectangle! (int (:x BOTTOM)) (int (:y BOTTOM)) (int (:width BOTTOM)) (int (:height BOTTOM)) colors/white)
  (rtd/draw-text! (str " fps: " avg-fps) 720 20 20 colors/purple)
  (rtd/draw-text! (str left) (- (quot WIDTH 2) 100) 20 20 colors/white)
  (rtd/draw-text! (str right) (+ (quot WIDTH 2) 100) 20 20 colors/white)
  (ext/draw-circle! (int (first ball)) (int (second ball)) BALL_RADIUS colors/white)
  (rcd/end-drawing!))

(defn draw-ending [{:keys [left right winner]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/black)
  (rtd/draw-text! (str left) (- (quot WIDTH 2) 100) 20 20 colors/white)
  (rtd/draw-text! (str right) (+ (quot WIDTH 2) 100) 20 20 colors/white)
  (let [winner-name (string/upper-case (name winner))
        text (str "Congratulations " winner-name "!")
        winner-color (if (= winner :left) colors/blue colors/red)
        size 20
        width (ext/measure-text text size)]
    (rtd/draw-text! text (int (- (quot WIDTH 2) (/ width 2))) (int (/ HEIGHT 2)) size winner-color))
  (let [text "press ENTER to restart"
        size 20
        width (ext/measure-text text size)]
    (rtd/draw-text! text (int (- (quot WIDTH 2) (/ width 2))) (- HEIGHT 100) size colors/white))
  (rcd/end-drawing!))

(defn draw [{:keys [screen]
             :as game}]
  (condp = screen
    :title (draw-title game)
    :game (draw-game game)
    :ending (draw-ending game)))

(defn start []
  (init)
  (loop []
    (let [game (tick (update-fps @game-atom))]
      (when-not (rcw/window-should-close?)
        (reset! game-atom game)
        (draw game)
        (recur))))
  (rcw/close-window!))

(defn -main [& args]
  (start))

(comment
  (future (start))
  ;;
  )
