(ns examples.colors-palette
  "Raylib [shapes] example - colors palette
   
   Display all raylib colors in a grid. Hover over colors to see names.
   Based on: raylib/examples/shapes/shapes_colors_palette.c
   
   Complexity: ⭐⭐ Easy
   
   Controls:
   - Mouse: Hover over colors to see names
   - SPACE: Show all color names
   - F1: Toggle debug stats
   - Q: Exit"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.core.mouse :as rcm]
   [raylib.text.drawing :as rtd]
   [raylib.shapes.basic :as rsb]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats]))

;; Constants
(def WIDTH 800)
(def HEIGHT 450)
(def RECT_SIZE 100)
(def RECT_SPACING 10)
(def COLS 7)

;; Color palette - all raylib colors with their names
(def color-palette
  [{:name "DARKGRAY"
    :color colors/darkgray}
   {:name "MAROON"
    :color colors/maroon}
   {:name "ORANGE"
    :color colors/orange}
   {:name "DARKGREEN"
    :color colors/darkgreen}
   {:name "DARKBLUE"
    :color colors/darkblue}
   {:name "DARKPURPLE"
    :color colors/darkpurple}
   {:name "DARKBROWN"
    :color colors/darkbrown}
   {:name "GRAY"
    :color colors/gray}
   {:name "RED"
    :color colors/red}
   {:name "GOLD"
    :color colors/gold}
   {:name "LIME"
    :color colors/lime}
   {:name "BLUE"
    :color colors/blue}
   {:name "VIOLET"
    :color colors/violet}
   {:name "BROWN"
    :color colors/brown}
   {:name "LIGHTGRAY"
    :color colors/lightgray}
   {:name "PINK"
    :color colors/pink}
   {:name "YELLOW"
    :color colors/yellow}
   {:name "GREEN"
    :color colors/green}
   {:name "SKYBLUE"
    :color colors/skyblue}
   {:name "PURPLE"
    :color colors/purple}
   {:name "BEIGE"
    :color colors/beige}])

(defn calc-rect-position
  "Calculate rectangle position for index i"
  [i]
  (let [col (mod i COLS)
        row (quot i COLS)
        x (+ 20 (* col (+ RECT_SIZE RECT_SPACING)))
        y (+ 80 (* row (+ RECT_SIZE RECT_SPACING)))]
    {:x x
     :y y
     :width RECT_SIZE
     :height RECT_SIZE}))

(defn initial-state []
  {:exit? false
   :hovered-index nil})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! WIDTH HEIGHT "raylib [shapes] example - colors palette")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn point-in-rect?
  "Check if point is inside rectangle"
  [{px :x
    py :y} {rx :x
            ry :y
            rw :width
            rh :height}]
  (and (>= px rx) (<= px (+ rx rw))
       (>= py ry) (<= py (+ ry rh))))

(defn find-hovered-color
  "Find which color rectangle the mouse is hovering over"
  [mouse-pos]
  (first
   (keep-indexed
    (fn [i _]
      (let [rect (calc-rect-position i)]
        (when (point-in-rect? mouse-pos rect)
          i)))
    color-palette)))

(defn update-hover [game]
  (let [mouse-pos (rcm/get-mouse-position)
        hovered (find-hovered-color mouse-pos)]
    (assoc game :hovered-index hovered)))

(defn handle-input [game]
  (cond-> game
    (rck/is-key-down? (:q enums/keyboard-key))
    (assoc :exit? true)))

(defn tick [game]
  (debug-stats/update!)
  (-> game
      handle-input
      update-hover))

(defn draw [{:keys [hovered-index]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)

  ;; Title
  (rtd/draw-text! "raylib colors palette" 28 42 20 colors/black)
  (rtd/draw-text! "press SPACE to see all color names" (- WIDTH 220) (- HEIGHT 30) 10 colors/gray)

  ;; Draw show-all indicator
  (let [show-all? (rck/is-key-down? (:space enums/keyboard-key))]
    ;; Draw all color rectangles
    (doseq [[i {:keys [name color]}] (map-indexed vector color-palette)]
      (let [{:keys [x y width height]} (calc-rect-position i)
            is-hovered? (= i hovered-index)
            show-name? (or show-all? is-hovered?)]
        ;; Draw color rectangle
        (rsb/draw-rectangle! x y width height color)

        ;; Draw name label when hovered or showing all
        (when show-name?
          ;; Draw black background for name
          (rsb/draw-rectangle! x (+ y height -26) width 20 colors/black)
          ;; Draw color name
          (let [text-width (rtd/measure-text name 10)
                text-x (+ x width (- text-width) -12)
                text-y (+ y height -20)]
            (rtd/draw-text! name text-x text-y 10 color)))

        ;; Draw border when hovered
        (when is-hovered?
          (rsb/draw-rectangle! x y width 3 colors/black)
          (rsb/draw-rectangle! x y 3 height colors/black)
          (rsb/draw-rectangle! (+ x width -3) y 3 height colors/black)
          (rsb/draw-rectangle! x (+ y height -3) width 3 colors/black)))))

  ;; Draw debug stats overlay
  (debug-stats/draw!)

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
  ;; For REPL development - connect to port 7888 after running the game
  @game-atom

  ;; Check all colors
  color-palette

  ;; Reset to initial state
  (reset! game-atom (initial-state))
  ;;
  )
