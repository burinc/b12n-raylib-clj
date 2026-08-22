(ns raylib.easings
  "raylib's examples/shapes/reasings.h, ported to Clojure.

  Not a binding layer: reasings.h is a header-only helper that ships with
  raylib's examples rather than part of the library, so there is no C symbol
  to bind. Same situation as raylib/lights.clj, which ports rlights.h.

  Every function takes the same four arguments as the C, and the argument
  order is worth stating because it is not the usual normalised 0..1 form:

    t  elapsed time (0 .. d)
    b  begin value
    c  CHANGE in value, i.e. end - begin, not the end value
    d  duration

  So a value moving 100 -> 300 over 2 seconds is (ease t 100.0 200.0 2.0).

  Formulas are transcribed from reasings.h rather than rewritten from
  memory - the in-out variants and the elastic/back constants are easy to
  get subtly wrong in ways that still animate plausibly."
  (:refer-clojure :exclude [linear]))

(def ^:private PI Math/PI)

;; Linear - all four names exist in reasings.h and are the same function,
;; kept so a caller iterating the full set finds what it expects.
(defn linear-none [t b c d] (+ (/ (* c t) d) b))
(defn linear-in [t b c d] (linear-none t b c d))
(defn linear-out [t b c d] (linear-none t b c d))
(defn linear-in-out [t b c d] (linear-none t b c d))

;; Sine
(defn sine-in [t b c d] (+ (* (- c) (Math/cos (* (/ t d) (/ PI 2.0)))) c b))
(defn sine-out [t b c d] (+ (* c (Math/sin (* (/ t d) (/ PI 2.0)))) b))
(defn sine-in-out [t b c d] (+ (* (/ (- c) 2.0) (- (Math/cos (/ (* PI t) d)) 1.0)) b))

;; Circular
(defn circ-in [t b c d]
  (let [t (/ t d)] (+ (* (- c) (- (Math/sqrt (- 1.0 (* t t))) 1.0)) b)))
(defn circ-out [t b c d]
  (let [t (- (/ t d) 1.0)] (+ (* c (Math/sqrt (- 1.0 (* t t)))) b)))
(defn circ-in-out [t b c d]
  (let [t (/ t (/ d 2.0))]
    (if (< t 1.0)
      (+ (* (/ (- c) 2.0) (- (Math/sqrt (- 1.0 (* t t))) 1.0)) b)
      (let [t (- t 2.0)] (+ (* (/ c 2.0) (+ (Math/sqrt (- 1.0 (* t t))) 1.0)) b)))))

;; Cubic
(defn cubic-in [t b c d] (let [t (/ t d)] (+ (* c t t t) b)))
(defn cubic-out [t b c d] (let [t (- (/ t d) 1.0)] (+ (* c (+ (* t t t) 1.0)) b)))
(defn cubic-in-out [t b c d]
  (let [t (/ t (/ d 2.0))]
    (if (< t 1.0)
      (+ (* (/ c 2.0) t t t) b)
      (let [t (- t 2.0)] (+ (* (/ c 2.0) (+ (* t t t) 2.0)) b)))))

;; Quadratic
(defn quad-in [t b c d] (let [t (/ t d)] (+ (* c t t) b)))
(defn quad-out [t b c d] (let [t (/ t d)] (+ (* (- c) t (- t 2.0)) b)))
(defn quad-in-out [t b c d]
  (let [t (/ t (/ d 2.0))]
    (if (< t 1.0)
      (+ (* (/ c 2.0) t t) b)
      (+ (* (/ (- c) 2.0) (- (* (- t 1.0) (- t 3.0)) 1.0)) b))))

;; Exponential - the t=0 and t=d cases are special-cased in the C because
;; 2^-inf and 2^0 do not land exactly on the endpoints.
(defn expo-in [t b c d]
  (if (zero? t) b (+ (* c (Math/pow 2.0 (* 10.0 (- (/ t d) 1.0)))) b)))
(defn expo-out [t b c d]
  (if (= t d) (+ b c) (+ (* c (+ (- (Math/pow 2.0 (/ (* -10.0 t) d))) 1.0)) b)))
(defn expo-in-out [t b c d]
  (cond
    (zero? t) b
    (= t d) (+ b c)
    :else (let [t (/ t (/ d 2.0))]
            (if (< t 1.0)
              (+ (* (/ c 2.0) (Math/pow 2.0 (* 10.0 (- t 1.0)))) b)
              (let [t (- t 1.0)]
                (+ (* (/ c 2.0) (+ (- (Math/pow 2.0 (* -10.0 t))) 2.0)) b))))))

;; Back - overshoots then settles. 1.70158 gives roughly 10% overshoot.
(def ^:private back-s 1.70158)

(defn back-in [t b c d]
  (let [t (/ t d)] (+ (* c t t (- (* (+ back-s 1.0) t) back-s)) b)))
(defn back-out [t b c d]
  (let [t (- (/ t d) 1.0)]
    (+ (* c (+ (* t t (+ (* (+ back-s 1.0) t) back-s)) 1.0)) b)))
(defn back-in-out [t b c d]
  (let [t (/ t (/ d 2.0))
        s (* back-s 1.525)]
    (if (< t 1.0)
      (+ (* (/ c 2.0) (* t t (- (* (+ s 1.0) t) s))) b)
      (let [t (- t 2.0)]
        (+ (* (/ c 2.0) (+ (* t t (+ (* (+ s 1.0) t) s)) 2.0)) b)))))

;; Bounce - four parabolic arcs of decreasing height.
(defn bounce-out [t b c d]
  (let [t (/ t d)]
    (cond
      (< t (/ 1.0 2.75)) (+ (* c 7.5625 t t) b)
      (< t (/ 2.0 2.75)) (let [t (- t (/ 1.5 2.75))] (+ (* c (+ (* 7.5625 t t) 0.75)) b))
      (< t (/ 2.5 2.75)) (let [t (- t (/ 2.25 2.75))] (+ (* c (+ (* 7.5625 t t) 0.9375)) b))
      :else (let [t (- t (/ 2.625 2.75))] (+ (* c (+ (* 7.5625 t t) 0.984375)) b)))))
(defn bounce-in [t b c d] (+ (- c (bounce-out (- d t) 0.0 c d)) b))
(defn bounce-in-out [t b c d]
  (if (< t (/ d 2.0))
    (+ (* (bounce-in (* t 2.0) 0.0 c d) 0.5) b)
    (+ (* (bounce-out (- (* t 2.0) d) 0.0 c d) 0.5) (* c 0.5) b)))

;; Elastic - a decaying sine. p is the period, s the phase shift.
(defn elastic-in [t b c d]
  (let [t' (/ t d)]
    (cond
      (zero? t) b
      (= t' 1.0) (+ b c)
      :else (let [p (* d 0.3) s (/ p 4.0) t (- t' 1.0)]
              (+ (- (* c (Math/pow 2.0 (* 10.0 t))
                       (Math/sin (/ (* (- (* t d) s) 2.0 PI) p)))) b)))))
(defn elastic-out [t b c d]
  (let [t' (/ t d)]
    (cond
      (zero? t) b
      (= t' 1.0) (+ b c)
      :else (let [p (* d 0.3) s (/ p 4.0)]
              (+ (* c (Math/pow 2.0 (* -10.0 t'))
                    (Math/sin (/ (* (- (* t' d) s) 2.0 PI) p))) c b)))))
(defn elastic-in-out [t b c d]
  (let [t' (/ t (/ d 2.0))]
    (cond
      (zero? t) b
      (= t' 2.0) (+ b c)
      :else (let [p (* d (* 0.3 1.5)) s (/ p 4.0)]
              (if (< t' 1.0)
                (let [t (- t' 1.0)]
                  (+ (* -0.5 (* c (Math/pow 2.0 (* 10.0 t))
                                (Math/sin (/ (* (- (* t d) s) 2.0 PI) p)))) b))
                (let [t (- t' 1.0)]
                  (+ (* c (Math/pow 2.0 (* -10.0 t))
                        (Math/sin (/ (* (- (* t d) s) 2.0 PI) p)) 0.5) c b)))))))

(def all
  "The 28 easings in reasings.h order, as [name fn] pairs. Ordered so an
   index into this vector matches the C's EasingTypes enum."
  [["EaseLinearNone" linear-none] ["EaseLinearIn" linear-in]
   ["EaseLinearOut" linear-out] ["EaseLinearInOut" linear-in-out]
   ["EaseSineIn" sine-in] ["EaseSineOut" sine-out] ["EaseSineInOut" sine-in-out]
   ["EaseCircIn" circ-in] ["EaseCircOut" circ-out] ["EaseCircInOut" circ-in-out]
   ["EaseCubicIn" cubic-in] ["EaseCubicOut" cubic-out] ["EaseCubicInOut" cubic-in-out]
   ["EaseQuadIn" quad-in] ["EaseQuadOut" quad-out] ["EaseQuadInOut" quad-in-out]
   ["EaseExpoIn" expo-in] ["EaseExpoOut" expo-out] ["EaseExpoInOut" expo-in-out]
   ["EaseBackIn" back-in] ["EaseBackOut" back-out] ["EaseBackInOut" back-in-out]
   ["EaseBounceOut" bounce-out] ["EaseBounceIn" bounce-in] ["EaseBounceInOut" bounce-in-out]
   ["EaseElasticIn" elastic-in] ["EaseElasticOut" elastic-out]
   ["EaseElasticInOut" elastic-in-out]])
