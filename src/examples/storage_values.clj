(ns examples.storage-values
  "raylib [core] example - storage values

   Generate two random scores, save them to a file, quit, come back and
   load them. R randomises, ENTER saves, SPACE loads.

   The C carries 60-odd lines of example-local SaveStorageValue and
   LoadStorageValue built on LoadFileData, RL_REALLOC and pointer
   arithmetic, growing the file by hand when a position lies past its end.
   None of that is raylib API, and none of it is needed here: the file is
   just an array of little-endian 32-bit ints, which java.nio reads and
   writes directly. Same file format, same storage.data name - a file
   written by the C version loads here and vice versa.

   Difficulty: 2/4
   Based on: core/core_storage_values.c"
  (:require
   [raylib.core.window :as rcw]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.utils :as ru]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.nrepl :as nrepl]
   [debug-stats])
  (:import
   [java.io File]
   [java.nio ByteBuffer ByteOrder]
   [java.nio.file Files Paths]))

(def screen-width 800)
(def screen-height 450)

(def storage-file "storage.data")

;; Slot indices within the file, matching the C's enum.
(def position-score 0)
(def position-hiscore 1)

(defn- path [] (Paths/get storage-file (into-array String [])))

(defn- read-ints
  "The file as a vector of ints, or empty if it does not exist yet."
  []
  (if (.exists (File. storage-file))
    (let [buf (doto (ByteBuffer/wrap (Files/readAllBytes (path)))
                (.order ByteOrder/LITTLE_ENDIAN))]
      (vec (repeatedly (quot (.remaining buf) 4) #(.getInt buf))))
    []))

(defn- write-ints! [xs]
  (let [buf (doto (ByteBuffer/allocate (* 4 (count xs)))
              (.order ByteOrder/LITTLE_ENDIAN))]
    (doseq [x xs] (.putInt buf (int x)))
    (Files/write (path) (.array buf) (make-array java.nio.file.OpenOption 0))))

(defn save-storage-value!
  "Write `value` at slot `position`, growing the file with zeros if the
   slot lies past the current end - the same growth the C's realloc branch
   does, minus the pointer arithmetic."
  [position value]
  (let [xs (read-ints)
        xs (into xs (repeat (max 0 (- (inc position) (count xs))) 0))]
    (write-ints! (assoc xs position value))
    nil))

(defn load-storage-value
  "Value at `position`, or 0 if the file is missing or shorter. Matches the
   C, which documents 0 as the not-found result."
  [position]
  (get (read-ints) position 0))

(defn initial-state [] {:score 0 :hiscore 0 :frames 0})

(def game-atom (atom (initial-state)))

(defn init []
  (rcw/init-window! screen-width screen-height "raylib [core] example - storage values")
  (rct/set-target-fps! 60)
  (debug-stats/enable!))

(defn tick [{:keys [score hiscore] :as state}]
  (debug-stats/update!)
  (let [[score hiscore]
        (cond
          (rck/is-key-pressed? (:r enums/keyboard-key))
          [(ru/get-random-value 1000 2000) (ru/get-random-value 2000 4000)]

          (rck/is-key-pressed? (:enter enums/keyboard-key))
          (do (save-storage-value! position-score score)
              (save-storage-value! position-hiscore hiscore)
              [score hiscore])

          (rck/is-key-pressed? (:space enums/keyboard-key))
          [(load-storage-value position-score) (load-storage-value position-hiscore)]

          :else [score hiscore])]
    (assoc state :score score :hiscore hiscore :frames (inc (:frames state)))))

(defn draw [{:keys [score hiscore frames]}]
  (rcd/begin-drawing!)
  (rcd/clear-background! colors/raywhite)
  (rtd/draw-text! (format "SCORE: %d" score) 280 130 40 colors/maroon)
  (rtd/draw-text! (format "HI-SCORE: %d" hiscore) 210 200 50 colors/black)
  (rtd/draw-text! (format "frames: %d" frames) 10 10 20 colors/lime)
  (rtd/draw-text! "Press R to generate random numbers" 220 40 20 colors/lightgray)
  (rtd/draw-text! "Press ENTER to SAVE values" 250 310 20 colors/lightgray)
  (rtd/draw-text! "Press SPACE to LOAD values" 252 350 20 colors/lightgray)
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
