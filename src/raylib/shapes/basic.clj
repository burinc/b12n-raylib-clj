(ns raylib.shapes.basic
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem]
   [coffi.ffi :refer [defcfn]]))

; ...

(defcfn draw-circle-v!
  "Draw a color-filled circle (Vector version)"
  {:arglists '([center radius color])}
  "DrawCircleV"
  [::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

; ...

(defcfn draw-rectangle!
  "Draw a color-filled rectangle"
  {:arglists '([x y width height color])}
  "DrawRectangle"
  [::mem/int ::mem/int ::mem/int ::mem/int ::rs/color] ::mem/void)

(defcfn draw-rectangle-lines!
  "Draw rectangle outline"
  {:arglists '([x y width height color])}
  "DrawRectangleLines"
  [::mem/int ::mem/int ::mem/int ::mem/int ::rs/color] ::mem/void)

(defcfn draw-rectangle-rec!
  "Draw a color-filled rectangle from Rectangle struct"
  {:arglists '([rec color])}
  "DrawRectangleRec"
  [::rs/rectangle ::rs/color] ::mem/void)

(defcfn draw-rectangle-lines-ex!
  "Draw rectangle outline with extended parameters"
  {:arglists '([rec line-thick color])}
  "DrawRectangleLinesEx"
  [::rs/rectangle ::mem/float ::rs/color] ::mem/void)

(defcfn draw-line!
  "Draw a line"
  {:arglists '([start-x start-y end-x end-y color])}
  "DrawLine"
  [::mem/int ::mem/int ::mem/int ::mem/int ::rs/color] ::mem/void)

(defcfn draw-circle!
  "Draw a color-filled circle"
  {:arglists '([center-x center-y radius color])}
  "DrawCircle"
  [::mem/int ::mem/int ::mem/float ::rs/color] ::mem/void)

(defcfn draw-circle-gradient!
  "Draw a gradient-filled circle"
  {:arglists '([center-x center-y radius inner-color outer-color])}
  "DrawCircleGradient"
  [::mem/int ::mem/int ::mem/float ::rs/color ::rs/color] ::mem/void)

(defcfn draw-circle-lines!
  "Draw circle outline"
  {:arglists '([center-x center-y radius color])}
  "DrawCircleLines"
  [::mem/int ::mem/int ::mem/float ::rs/color] ::mem/void)

(defcfn draw-ellipse!
  "Draw ellipse"
  {:arglists '([center-x center-y radius-h radius-v color])}
  "DrawEllipse"
  [::mem/int ::mem/int ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-ellipse-lines!
  "Draw ellipse outline"
  {:arglists '([center-x center-y radius-h radius-v color])}
  "DrawEllipseLines"
  [::mem/int ::mem/int ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-rectangle-gradient-h!
  "Draw a horizontal-gradient-filled rectangle"
  {:arglists '([x y width height left-color right-color])}
  "DrawRectangleGradientH"
  [::mem/int ::mem/int ::mem/int ::mem/int ::rs/color ::rs/color] ::mem/void)

(defcfn draw-triangle!
  "Draw a color-filled triangle (vertex in counter-clockwise order!)"
  {:arglists '([v1 v2 v3 color])}
  "DrawTriangle"
  [::rs/vector-2 ::rs/vector-2 ::rs/vector-2 ::rs/color] ::mem/void)

(defcfn draw-triangle-lines!
  "Draw triangle outline (vertex in counter-clockwise order!)"
  {:arglists '([v1 v2 v3 color])}
  "DrawTriangleLines"
  [::rs/vector-2 ::rs/vector-2 ::rs/vector-2 ::rs/color] ::mem/void)

(defcfn draw-poly!
  "Draw a regular polygon (Vector version)"
  {:arglists '([center sides radius rotation color])}
  "DrawPoly"
  [::rs/vector-2 ::mem/int ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-poly-lines!
  "Draw a polygon outline of n sides"
  {:arglists '([center sides radius rotation color])}
  "DrawPolyLines"
  [::rs/vector-2 ::mem/int ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-poly-lines-ex!
  "Draw a polygon outline of n sides with extended parameters"
  {:arglists '([center sides radius rotation line-thick color])}
  "DrawPolyLinesEx"
  [::rs/vector-2 ::mem/int ::mem/float ::mem/float ::mem/float ::rs/color] ::mem/void)

(defcfn draw-line-bezier!
  "Draw line segment cubic-bezier in-out interpolation"
  {:arglists '([start-pos end-pos thick color])}
  "DrawLineBezier"
  [::rs/vector-2 ::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

(defcfn draw-circle-lines-v!
  "Draw circle outline (Vector version)"
  {:arglists '([center radius color])}
  "DrawCircleLinesV"
  [::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

;; Moved from raylib-ext (2026-08-22 consolidation)
(defcfn draw-rectangle-rounded!
  "Draw rectangle with rounded edges"
  {:arglists '([rec roundness segments color])}
  "DrawRectangleRounded"
  [::rs/rectangle ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-rectangle-rounded-lines!
  "Draw rectangle lines with rounded edges"
  {:arglists '([rec roundness segments color])}
  "DrawRectangleRoundedLines"
  [::rs/rectangle ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-rectangle-rounded-lines-ex!
  "Draw rectangle with rounded edges outline"
  {:arglists '([rec roundness segments line-thick color])}
  "DrawRectangleRoundedLinesEx"
  [::rs/rectangle ::mem/float ::mem/int ::mem/float ::rs/color] ::mem/void)

(defcfn draw-line-ex!
  "Draw a line with thickness"
  {:arglists '([start-pos end-pos thick color])}
  "DrawLineEx"
  [::rs/vector-2 ::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

(defcfn draw-ring!
  "Draw ring"
  {:arglists '([center inner-radius outer-radius start-angle end-angle segments color])}
  "DrawRing"
  [::rs/vector-2 ::mem/float ::mem/float ::mem/float ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-ring-lines!
  "Draw ring outline"
  {:arglists '([center inner-radius outer-radius start-angle end-angle segments color])}
  "DrawRingLines"
  [::rs/vector-2 ::mem/float ::mem/float ::mem/float ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-circle-sector!
  "Draw a piece of a circle"
  {:arglists '([center radius start-angle end-angle segments color])}
  "DrawCircleSector"
  [::rs/vector-2 ::mem/float ::mem/float ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-circle-sector-lines!
  "Draw circle sector outline"
  {:arglists '([center radius start-angle end-angle segments color])}
  "DrawCircleSectorLines"
  [::rs/vector-2 ::mem/float ::mem/float ::mem/float ::mem/int ::rs/color] ::mem/void)

(defcfn draw-rectangle-v!
  "Draw a color-filled rectangle (Vector version)"
  {:arglists '([position size color])}
  "DrawRectangleV"
  [::rs/vector-2 ::rs/vector-2 ::rs/color] ::mem/void)

(defcfn draw-line-v!
  "Draw a line (Vector version)"
  {:arglists '([start-pos end-pos color])}
  "DrawLineV"
  [::rs/vector-2 ::rs/vector-2 ::rs/color] ::mem/void)

(defcfn draw-rectangle-pro!
  "Draw a color-filled rectangle with pro parameters"
  {:arglists '([rec origin rotation color])}
  "DrawRectanglePro"
  [::rs/rectangle ::rs/vector-2 ::mem/float ::rs/color] ::mem/void)

;; ---------------------------------------------------------------------------
;; Not a binding.
;;
;; raylib 6.x added DrawLineDashed, but this project bundles 5.5.0, whose
;; library does not export it - a defcfn would compile and then crash on a
;; null function pointer. This is a Clojure stand-in following rshapes.c's
;; own logic, including its fallback to a solid line when the line is too
;; short to dash or the dash size is not positive.
;;
;; raylib draws the dashes 1px wide and takes no thickness argument. The
;; 5-argument form matches it; the 6-argument form exists because the
;; dashed-line example draws at 2px and predates this helper.

(defn draw-dashed-line!
  "Draw a dashed line from `start-pos` to `end-pos`.

   Mirrors raylib 6.x's `DrawLineDashed`, which the bundled 5.5.0 lacks.
   Falls back to a solid line when the span is shorter than one dash plus
   one gap, or when `dash-size` is not positive."
  ([start-pos end-pos dash-size space-size color]
   (draw-dashed-line! start-pos end-pos dash-size space-size color 1.0))
  ([start-pos end-pos dash-size space-size color thickness]
   (let [dx (- (:x end-pos) (:x start-pos))
         dy (- (:y end-pos) (:y start-pos))
         line-length (Math/sqrt (+ (* dx dx) (* dy dy)))]
     (if (or (< line-length (+ dash-size space-size)) (<= dash-size 0))
       (draw-line-ex! {:x (float (:x start-pos)) :y (float (:y start-pos))}
                      {:x (float (:x end-pos)) :y (float (:y end-pos))}
                      (float thickness) color)
       (let [dir-x (/ dx line-length)
             dir-y (/ dy line-length)
             stride (+ dash-size space-size)]
         (loop [travelled 0.0]
           (when (< travelled line-length)
             (let [dash-end (min (+ travelled dash-size) line-length)]
               (draw-line-ex!
                {:x (float (+ (:x start-pos) (* dir-x travelled)))
                 :y (float (+ (:y start-pos) (* dir-y travelled)))}
                {:x (float (+ (:x start-pos) (* dir-x dash-end)))
                 :y (float (+ (:y start-pos) (* dir-y dash-end)))}
                (float thickness) color)
               (recur (+ travelled stride))))))))))

(defcfn draw-spline-linear-raw!
  "Draw spline: Linear, minimum 2 points (internal - takes a Vector2 array)"
  {:arglists '([points point-count thick color])}
  "DrawSplineLinear"
  [::mem/pointer ::mem/int ::mem/float ::rs/color] ::mem/void)

(defn draw-spline-linear!
  "Draw a linear spline through `points`, a seq of `{:x :y}` maps.

   raylib wants a contiguous `Vector2 *`, so the points are packed into one
   here - two floats per point, eight bytes each. The allocation goes to
   coffi's automatic arena, so it is reclaimed by the GC rather than leaked,
   which is what makes calling this once per frame acceptable.

   Needs at least two points; fewer is a no-op rather than a crash, since
   raylib would read past the end of a one-element array."
  [points thick color]
  (let [pts (vec points)
        n (count pts)]
    (when (>= n 2)
      (let [buf (mem/alloc (* 8 n))]
        (dotimes [i n]
          (let [p (nth pts i)
                slot (mem/slice buf (* 8 i))]
            (mem/write-float slot 0 (float (:x p)))
            (mem/write-float (mem/slice slot 4) 0 (float (:y p)))))
        (draw-spline-linear-raw! buf n (float thick) color)))))
