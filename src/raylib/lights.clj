(ns raylib.lights
  "Light management utilities for shader-based lighting
   Based on raylib's rlights.h implementation"
  (:require
   [raylib.core.shaders :as rcs]))

;; Light types
(def LIGHT_DIRECTIONAL 0)
(def LIGHT_POINT 1)

;; Maximum number of lights supported by shader
(def MAX_LIGHTS 4)

;; Light counter (atom to track created lights)
(def ^:private lights-count (atom 0))

(defn reset-lights!
  "Reset the lights counter (call when restarting)"
  []
  (reset! lights-count 0))

(defn update-light-values!
  "Send light properties to shader"
  [shader light]
  (let [{:keys [enabled type position target color
                enabled-loc type-loc position-loc target-loc color-loc]} light]
    ;; Send enabled state and type
    (rcs/set-shader-value-int! shader enabled-loc (if enabled 1 0))
    (rcs/set-shader-value-int! shader type-loc type)
    ;; Send position
    (rcs/set-shader-value-vec3! shader position-loc
                                [(:x position) (:y position) (:z position)])
    ;; Send target
    (rcs/set-shader-value-vec3! shader target-loc
                                [(:x target) (:y target) (:z target)])
    ;; Send color (normalized to 0-1 range)
    (rcs/set-shader-value-vec4! shader color-loc
                                [(/ (:r color) 255.0)
                                 (/ (:g color) 255.0)
                                 (/ (:b color) 255.0)
                                 (/ (:a color) 255.0)])))

(defn create-light
  "Create a light and get shader locations.
   Returns a light map with all properties and shader locations."
  [light-type position target color shader]
  (when (< @lights-count MAX_LIGHTS)
    (let [idx @lights-count
          enabled-loc (rcs/get-shader-location shader (format "lights[%d].enabled" idx))
          type-loc (rcs/get-shader-location shader (format "lights[%d].type" idx))
          position-loc (rcs/get-shader-location shader (format "lights[%d].position" idx))
          target-loc (rcs/get-shader-location shader (format "lights[%d].target" idx))
          color-loc (rcs/get-shader-location shader (format "lights[%d].color" idx))
          light {:type light-type
                 :enabled true
                 :position position
                 :target target
                 :color color
                 :enabled-loc enabled-loc
                 :type-loc type-loc
                 :position-loc position-loc
                 :target-loc target-loc
                 :color-loc color-loc}]
      (swap! lights-count inc)
      ;; Update initial values
      (update-light-values! shader light)
      light)))

(defn toggle-light
  "Toggle light enabled state, returns updated light"
  [light]
  (update light :enabled not))
