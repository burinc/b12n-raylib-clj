(ns raylib.raygui
  "A Clojure port of the raygui controls raylib's own examples use.

   Third of the header-only companions ported rather than bound, after
   `raylib.easings` (reasings.h) and `raylib.raymath` (raymath.h) - but this
   one had no choice in the matter. raygui is immediate-mode C compiled into
   the including translation unit, so the bundled libraylib exports zero
   `Gui*` symbols and there is nothing a `defcfn` could point at.

   Scope is the controls the examples actually reach for, not all of raygui:
   19 of raylib's 218 examples include raygui.h, and across them
   GuiSliderBar, GuiSetStyle, GuiButton, GuiLabel and GuiCheckBox account for
   the large majority of calls.

   Two departures from the C, both forced by not having pointers:

   - Controls RETURN their new value instead of writing through one. The C
     writes `*value` and returns whether it changed; here `slider` returns
     the value itself and `check-box` returns the new boolean. A caller
     threads the result back into its own state.
   - The C's `guiState`, style table and drag tracking are file-scope
     globals. They are atoms here. That is not incidental: an immediate-mode
     control has to remember, between frames, that a drag which began inside
     its bounds is still in progress even when the pointer has left them.
     Making it explicit would mean threading a context through every call
     and would stop these reading like their C originals.

   Colours are stored as raygui does it, 0xRRGGBBAA ints, so a style value
   copied straight out of a C example works unchanged."
  (:require
   [raylib.core.mouse :as rcm]
   [raylib.core.collision :as rcol]
   [raylib.shapes.basic :as rsb]
   [raylib.text.drawing :as rtd]
   [raylib.enums :as enums]))

;; ------------------------------------------------------------------ state

(def state-normal 0)
(def state-focused 1)
(def state-pressed 2)
(def state-disabled 3)

(def align-left 0)
(def align-center 1)
(def align-right 2)

(def default-style
  "raygui's default LIGHT theme, transcribed from GuiLoadStyleDefault.
   Keyed by [control property]; `:default` supplies any control that has no
   entry of its own, which is how raygui's own fallback works."
  {[:default :border-normal] 0x838383ff
   [:default :base-normal] 0xc9c9c9ff
   [:default :text-normal] 0x686868ff
   [:default :border-focused] 0x5bb2d9ff
   [:default :base-focused] 0xc9effeff
   [:default :text-focused] 0x6c9bbcff
   [:default :border-pressed] 0x0492c7ff
   [:default :base-pressed] 0x97e8ffff
   [:default :text-pressed] 0x368bafff
   [:default :border-disabled] 0xb5c1c2ff
   [:default :base-disabled] 0xe6e9e9ff
   [:default :text-disabled] 0xaeb7b8ff
   [:default :border-width] 1
   [:default :text-padding] 0
   [:default :text-alignment] align-center
   [:default :text-size] 10
   [:default :text-spacing] 1
   [:default :line-color] 0x90abb5ff
   [:default :background-color] 0xf5f5f5ff
   [:label :text-alignment] align-left
   [:button :border-width] 2
   [:slider :text-padding] 4
   [:slider :slider-width] 16
   [:slider :slider-padding] 1
   [:checkbox :text-padding] 4
   [:checkbox :text-alignment] align-right
   [:checkbox :check-padding] 1
   [:valuebox :text-padding] 0
   [:valuebox :text-alignment] align-left
   [:valuebox :spinner-button-width] 24
   [:valuebox :spinner-button-spacing] 2})

(defonce ^:private style (atom default-style))

;; Drag ownership. raygui calls this guiControlExclusiveMode: once a drag
;; starts inside a slider it keeps that slider, so sweeping the pointer
;; across a panel does not hand the drag to whatever sits under it.
(defonce ^:private exclusive (atom nil))

(defonce ^:private gui-state (atom state-normal))
(defonce ^:private locked (atom false))

(defn set-style! [control property value] (swap! style assoc [control property] value))

(defn get-style
  "Style lookup with raygui's fallback: the control's own value, else
   `:default`'s, else 0."
  [control property]
  (let [s @style]
    (or (get s [control property]) (get s [:default property]) 0)))

(defn load-style-default! [] (reset! style default-style))
(defn set-state! [s] (reset! gui-state s))
(defn enable! [] (reset! gui-state state-normal))
(defn disable! [] (reset! gui-state state-disabled))
(defn lock! [] (reset! locked true))
(defn unlock! [] (reset! locked false))

(defn ^:private state-suffix [st]
  (case (int st) 0 :normal, 1 :focused, 2 :pressed, 3 :disabled))

(defn ^:private color-of
  "raygui packs colours as 0xRRGGBBAA."
  [n]
  {:r (bit-and (bit-shift-right n 24) 0xff)
   :g (bit-and (bit-shift-right n 16) 0xff)
   :b (bit-and (bit-shift-right n 8) 0xff)
   :a (bit-and n 0xff)})

(defn ^:private style-color [control kind st]
  (color-of (get-style control (keyword (str (name kind) "-" (name (state-suffix st)))))))

(def ^:private blank {:r 0 :g 0 :b 0 :a 0})

;; ---------------------------------------------------------------- drawing

(defn ^:private text-width [text]
  (if (or (nil? text) (= "" text)) 0 (rtd/measure-text text (get-style :default :text-size))))

(defn ^:private draw-box
  "raygui's GuiDrawRectangle: fill, then an inset border of `border-width`."
  [bounds border-width border-color fill-color]
  (when (pos? (:a fill-color)) (rsb/draw-rectangle-rec! bounds fill-color))
  (when (pos? border-width)
    (rsb/draw-rectangle-lines-ex! bounds (float border-width) border-color)))

(defn ^:private draw-aligned-text [text bounds alignment color]
  (when (and text (not= "" text))
    (let [size (get-style :default :text-size)
          w (text-width text)
          x (condp = alignment
              align-left (:x bounds)
              align-right (+ (:x bounds) (- (:width bounds) w))
              (+ (:x bounds) (/ (- (:width bounds) w) 2.0)))
          y (+ (:y bounds) (/ (- (:height bounds) size) 2.0))]
      (rtd/draw-text! text (int x) (int y) size color))))

;; ------------------------------------------------------------ interaction

(defn ^:private mouse [] (rcm/get-mouse-position))
(defn ^:private button-down? [] (rcm/is-mouse-button-down? (:left enums/mouse-button)))
(defn ^:private button-released? [] (rcm/is-mouse-button-released? (:left enums/mouse-button)))
(defn ^:private inside? [point rect] (pos? (rcol/check-collision-point-rec? point rect)))
(defn ^:private interactive? [] (and (not= @gui-state state-disabled) (not @locked)))

;; ---------------------------------------------------------------- controls

(defn label
  "Draw text in `bounds`. Returns nil; it is a label."
  [bounds text]
  (draw-aligned-text text bounds (get-style :label :text-alignment)
                     (style-color :label :text @gui-state))
  nil)

(defn button
  "Draw a button. Returns true on the frame the press is RELEASED inside it,
   matching the C, which fires on release rather than on press."
  [bounds text]
  (let [st (if (and (interactive?) (inside? (mouse) bounds))
             (if (button-down?) state-pressed state-focused)
             @gui-state)
        clicked? (and (interactive?) (inside? (mouse) bounds) (button-released?))]
    (draw-box bounds (get-style :button :border-width)
              (style-color :button :border st) (style-color :button :base st))
    (draw-aligned-text text bounds (get-style :button :text-alignment)
                       (style-color :button :text st))
    (boolean clicked?)))

(defn check-box
  "Draw a checkbox with its label. Returns the new checked state - the
   caller stores it, where the C wrote through a bool*.

   The clickable area covers the box AND its text, as in the C."
  [bounds text checked?]
  (let [size (get-style :default :text-size)
        padding (get-style :checkbox :text-padding)
        tw (+ (text-width text) 2)
        left? (= (get-style :checkbox :text-alignment) align-left)
        text-bounds {:x (if left? (- (:x bounds) tw padding) (+ (:x bounds) (:width bounds) padding))
                     :y (+ (:y bounds) (- (/ (:height bounds) 2.0) (/ size 2.0)))
                     :width (float tw) :height (float size)}
        total {:x (if left? (:x text-bounds) (:x bounds))
               :y (:y bounds)
               :width (+ (:width bounds) tw padding)
               :height (:height bounds)}
        hovering? (and (interactive?) (inside? (mouse) total))
        st (if hovering? (if (button-down?) state-pressed state-focused) @gui-state)
        checked? (if (and hovering? (button-released?)) (not checked?) checked?)
        bw (get-style :checkbox :border-width)]
    (draw-box bounds bw (style-color :checkbox :border st) blank)
    (when checked?
      (let [inset (+ bw (get-style :checkbox :check-padding))]
        (rsb/draw-rectangle-rec! {:x (+ (:x bounds) inset) :y (+ (:y bounds) inset)
                                  :width (- (:width bounds) (* 2 inset))
                                  :height (- (:height bounds) (* 2 inset))}
                                 (style-color :checkbox :text st))))
    (draw-aligned-text text text-bounds (if left? align-right align-left)
                       (style-color :label :text st))
    checked?))

(defn ^:private slider-pro
  "Shared body of `slider` and `slider-bar`. raygui distinguishes them purely
   by SLIDER_WIDTH: a positive width draws a knob that slides, and zero draws
   a bar that fills from the left. Returns the new value."
  [bounds text-left text-right value min-value max-value slider-width]
  (let [bw (get-style :slider :border-width)
        pad (get-style :slider :slider-padding)
        span (- max-value min-value)
        ;; A drag is ours only if it started here; that is what lets the
        ;; pointer leave the control mid-drag without dropping it.
        owned? (= @exclusive bounds)
        over? (inside? (mouse) bounds)
        value-from-mouse (fn []
                           (+ (* span (/ (- (:x (mouse)) (:x bounds) (/ slider-width 2.0))
                                         (- (:width bounds) slider-width)))
                              min-value))
        [value st]
        (cond
          (not (interactive?)) [value @gui-state]
          owned? (if (button-down?)
                   [(value-from-mouse) state-pressed]
                   (do (reset! exclusive nil) [value state-focused]))
          (and over? (button-down?)) (do (reset! exclusive bounds)
                                         [(value-from-mouse) state-pressed])
          over? [value state-focused]
          :else [value @gui-state])
        value (max min-value (min max-value value))
        filled (* (/ (- value min-value) span) (- (:width bounds) slider-width (* 2 bw)))
        track {:x (+ (:x bounds) bw) :y (+ (:y bounds) bw pad)
               :height (- (:height bounds) (* 2 bw) (* 2 pad))}
        knob (if (pos? slider-width)
               (assoc track :x (min (- (+ (:x bounds) (:width bounds)) slider-width bw)
                                    (max (+ (:x bounds) bw) (+ (:x bounds) filled)))
                      :width (float slider-width))
               (assoc track :width (min filled (- (:width bounds) (* 2 bw)))))
        size (get-style :default :text-size)
        tpad (get-style :slider :text-padding)
        mid-y (+ (:y bounds) (- (/ (:height bounds) 2.0) (/ size 2.0)))]
    (draw-box bounds bw (style-color :slider :border st)
              (color-of (get-style :slider (if (= st state-disabled) :base-disabled :base-normal))))
    (when (pos? (:width knob))
      (rsb/draw-rectangle-rec!
       knob
       (color-of (get-style :slider (condp = st
                                      state-normal :base-pressed
                                      state-focused :text-focused
                                      state-pressed :text-pressed
                                      :text-disabled)))))
    (when text-left
      (draw-aligned-text text-left
                         {:x (- (:x bounds) (text-width text-left) tpad) :y mid-y
                          :width (float (text-width text-left)) :height (float size)}
                         align-right (style-color :label :text st)))
    (when text-right
      (draw-aligned-text text-right
                         {:x (+ (:x bounds) (:width bounds) tpad) :y mid-y
                          :width (float (text-width text-right)) :height (float size)}
                         align-left (style-color :label :text st)))
    value))

(defn slider
  "A slider with a fixed-width knob. Returns the new value."
  [bounds text-left text-right value min-value max-value]
  (slider-pro bounds text-left text-right value min-value max-value
              (get-style :slider :slider-width)))

(defn slider-bar
  "A slider whose bar fills from the left edge. Returns the new value.

   In the C this is GuiSlider with SLIDER_WIDTH temporarily forced to 0."
  [bounds text-left text-right value min-value max-value]
  (slider-pro bounds text-left text-right value min-value max-value 0))

(defn line
  "A horizontal rule, optionally with a label set into it: the C draws
   `--- text ------------`, a short stub before the text and the remainder
   after it."
  [bounds text]
  (let [color (color-of (get-style :default (if (= @gui-state state-disabled)
                                              :border-disabled :line-color)))
        mid (+ (:y bounds) (/ (:height bounds) 2.0))
        margin 12.0 pad 4.0]
    (if (or (nil? text) (= "" text))
      (rsb/draw-rectangle-rec! {:x (:x bounds) :y mid :width (:width bounds) :height 1.0} color)
      (let [tw (+ (text-width text) 2)]
        (rsb/draw-rectangle-rec! {:x (:x bounds) :y mid :width (- margin pad) :height 1.0} color)
        (draw-aligned-text text {:x (+ (:x bounds) margin) :y (:y bounds)
                                 :width (float tw) :height (:height bounds)}
                           align-left color)
        (rsb/draw-rectangle-rec! {:x (+ (:x bounds) margin tw pad) :y mid
                                  :width (- (:width bounds) tw margin pad) :height 1.0}
                                 color))))
  nil)

(defn group-box
  "A labelled frame. Only three sides are drawn as plain rules - the top is a
   `line` carrying the label, which is what breaks the border for the text."
  [bounds text]
  (let [color (color-of (get-style :default (if (= @gui-state state-disabled)
                                              :border-disabled :line-color)))
        thick 1.0]
    (rsb/draw-rectangle-rec! {:x (:x bounds) :y (:y bounds)
                              :width thick :height (:height bounds)} color)
    (rsb/draw-rectangle-rec! {:x (:x bounds) :y (+ (:y bounds) (:height bounds) -1)
                              :width (:width bounds) :height thick} color)
    (rsb/draw-rectangle-rec! {:x (+ (:x bounds) (:width bounds) -1) :y (:y bounds)
                              :width thick :height (:height bounds)} color)
    (line {:x (:x bounds) :y (- (:y bounds) (/ (get-style :default :text-size) 2.0))
           :width (:width bounds) :height (float (get-style :default :text-size))}
          text))
  nil)

(defn toggle
  "A button that stays in. Returns the new active state.

   While idle an active toggle borrows the PRESSED colours, which is how it
   reads as latched; once the pointer is over it the ordinary hover and
   press colours take over and the active state stops showing. That is the
   C's behaviour, not an oversight here."
  [bounds text active?]
  (let [hovering? (and (interactive?) (inside? (mouse) bounds))
        released? (and hovering? (button-released?))
        active? (if released? (not active?) active?)
        st (cond (not hovering?) @gui-state
                 (button-down?) state-pressed
                 released? state-normal
                 :else state-focused)
        latched? (and (= st state-normal) active?)
        pick (fn [kind] (if latched?
                          (color-of (get-style :toggle (keyword (str (name kind) "-pressed"))))
                          (style-color :toggle kind st)))]
    (draw-box bounds (get-style :toggle :border-width) (pick :border) (pick :base))
    (draw-aligned-text text bounds (get-style :toggle :text-alignment) (pick :text))
    active?))

(defn spinner
  "A number with a decrement and an increment button. Returns the new value.

   raygui's spinner can also be typed into, via an edit mode backed by
   GuiValueBox. That is not ported: the only example reaching for a spinner
   passes editMode false, so the box here displays but does not accept text."
  [bounds text value min-value max-value]
  (let [bw (get-style :valuebox :spinner-button-width)
        gap (get-style :valuebox :spinner-button-spacing)
        left {:x (:x bounds) :y (:y bounds) :width (float bw) :height (:height bounds)}
        right {:x (+ (:x bounds) (:width bounds) (- bw)) :y (:y bounds)
               :width (float bw) :height (:height bounds)}
        box {:x (+ (:x bounds) bw gap) :y (:y bounds)
             :width (- (:width bounds) (* 2 (+ bw gap))) :height (:height bounds)}
        st (if (and (interactive?) (inside? (mouse) bounds))
             (if (button-down?) state-pressed state-focused)
             @gui-state)
        down? (button left "<")
        up? (button right ">")
        value (-> value (cond-> down? dec, up? inc) (max min-value) (min max-value))]
    (draw-box box (get-style :valuebox :border-width)
              (style-color :valuebox :border st) (style-color :valuebox :base st))
    (draw-aligned-text (str value) box align-center (style-color :valuebox :text st))
    (when (and text (not= "" text))
      (let [tw (+ (text-width text) 2)
            size (get-style :default :text-size)
            pad (get-style :valuebox :text-padding)
            left-aligned? (= (get-style :valuebox :text-alignment) align-left)]
        (draw-aligned-text
         text
         {:x (if left-aligned?
               (- (:x bounds) tw pad)
               (+ (:x bounds) (:width bounds) pad))
          :y (+ (:y bounds) (- (/ (:height bounds) 2.0) (/ size 2.0)))
          :width (float tw) :height (float size)}
         (if left-aligned? align-right align-left)
         (style-color :label :text st))))
    value))
