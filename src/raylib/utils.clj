(ns raylib.utils
  "Utility functions (random values, colors, misc)"
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem]
   [coffi.ffi :refer [defcfn]]))

;; Random value generation
(defcfn get-random-value
  "Get a random value between min and max (both included)"
  {:arglists '([min max])}
  "GetRandomValue"
  [::mem/int ::mem/int] ::mem/int)

(defcfn set-random-seed!
  "Set the seed for the random number generator"
  {:arglists '([seed])}
  "SetRandomSeed"
  [::mem/int] ::mem/void)

;; Color utilities
(defcfn fade
  "Get color with alpha applied, alpha goes from 0.0f to 1.0f.

   This is also raylib's `ColorAlpha`. The two are separate exported
   symbols with byte-identical bodies in `rtextures.c`, so `ColorAlpha`
   is deliberately not bound - a C example calling it ports to `fade`."
  {:arglists '([color alpha])}
  "Fade"
  [::rs/color ::mem/float] ::rs/color)

(defcfn color-to-int
  "Get hexadecimal value for a Color"
  {:arglists '([color])}
  "ColorToInt"
  [::rs/color] ::mem/int)

(defcfn color-from-hsv
  "Get a Color from HSV values, hue [0..360], saturation/value [0..1]"
  {:arglists '([hue saturation value])}
  "ColorFromHSV"
  [::mem/float ::mem/float ::mem/float] ::rs/color)

;; Moved from raylib-ext (2026-08-22 consolidation)
(defcfn get-color
  "Get Color structure from hexadecimal value"
  {:arglists '([hex-value])}
  "GetColor"
  [::mem/int] ::rs/color)

;; Additional shape drawing functions

(defcfn color-lerp
  "Get color lerp interpolation between two colors"
  {:arglists '([color1 color2 factor])}
  "ColorLerp"
  [::rs/color ::rs/color ::mem/float] ::rs/color)
