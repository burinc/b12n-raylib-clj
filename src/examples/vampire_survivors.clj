(ns examples.vampire-survivors
  "Vampire Survivors Clone - A top-down survival roguelike
   Survive waves of enemies, auto-attack with weapons, collect XP, level up!"
  (:require
   [raylib.core.window :as rcw]
   [raylib.nrepl :as nrepl]
   [raylib.core.timing :as rct]
   [raylib.core.drawing :as rcd]
   [raylib.core.keyboard :as rck]
   [raylib.text.drawing :as rtd]
   [raylib.colors :as colors]
   [raylib.enums :as enums]
   [raylib.shapes.basic :as rsb]
   [debug-stats])
  (:gen-class))

;; ============================================================================
;; CONSTANTS
;; ============================================================================

(def SCREEN_WIDTH 1280)
(def SCREEN_HEIGHT 720)

(def WORLD_WIDTH 4000)
(def WORLD_HEIGHT 4000)

;; Player constants
(def PLAYER_SIZE 24)
(def PLAYER_BASE_SPEED 520.0)
(def PLAYER_BASE_HP 100)
(def PLAYER_PICKUP_RADIUS 50.0)
(def PLAYER_INVINCIBILITY_TIME 1.0)

;; Enemy spawn settings
(def SPAWN_DISTANCE 600)                                    ;; Distance from player to spawn enemies
(def BASE_SPAWN_INTERVAL 2.0)                               ;; Seconds between spawn waves
(def BASE_ENEMIES_PER_SPAWN 3)

;; XP and leveling
(def BASE_XP_TO_LEVEL 10)
(def XP_LEVEL_MULTIPLIER 1.3)

;; Game timing
(def BOSS_TIME_1 (* 10 60))                                 ;; 10 minutes in frames at 60fps
(def BOSS_TIME_2 (* 20 60))                                 ;; 20 minutes

;; Visual settings
(def DAMAGE_NUMBER_LIFETIME 1.0)
(def HIT_FLASH_DURATION 0.1)

;; ============================================================================
;; MATH HELPERS
;; ============================================================================

(defn rand-range [min max]
  (+ min (* (rand) (- max min))))

(defn rand-int-range [min max]
  (+ min (rand-int (inc (- max min)))))

(defn distance [p1 p2]
  (let [dx (- (:x p2) (:x p1))
        dy (- (:y p2) (:y p1))]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn normalize [v]
  (let [len (Math/sqrt (+ (* (:x v) (:x v)) (* (:y v) (:y v))))]
    (if (> len 0)
      {:x (/ (:x v) len)
       :y (/ (:y v) len)}
      {:x 0
       :y 0})))

(defn vec-scale [v s]
  {:x (* (:x v) s)
   :y (* (:y v) s)})

(defn vec-add [v1 v2]
  {:x (+ (:x v1) (:x v2))
   :y (+ (:y v1) (:y v2))})

(defn vec-sub [v1 v2]
  {:x (- (:x v1) (:x v2))
   :y (- (:y v1) (:y v2))})

(defn vec-len [v]
  (Math/sqrt (+ (* (:x v) (:x v)) (* (:y v) (:y v)))))

(defn angle-to [from to]
  (Math/atan2 (- (:y to) (:y from)) (- (:x to) (:x from))))

(defn rotate-vec [angle]
  {:x (Math/cos angle)
   :y (Math/sin angle)})

(defn clamp [v min-v max-v]
  (max min-v (min max-v v)))

(defn lerp [a b t]
  (+ a (* (- b a) t)))

;; ============================================================================
;; DATA DEFINITIONS - WEAPONS
;; ============================================================================

(def weapon-definitions
  {:whip
   {:name "Whip"
    :description "Attacks horizontally, passes through enemies"
    :base-damage 20
    :base-cooldown 1.5
    :base-area 1.0
    :base-projectile-count 1
    :max-level 8
    :attack-type :melee-arc
    :pierce -1}                                             ;; -1 = infinite pierce

   :magic-wand
   {:name "Magic Wand"
    :description "Fires at the nearest enemy"
    :base-damage 10
    :base-cooldown 1.0
    :base-area 1.0
    :base-projectile-count 1
    :max-level 8
    :attack-type :projectile-homing
    :pierce 1
    :projectile-speed 400}

   :knife
   {:name "Knife"
    :description "Fires quickly in the faced direction"
    :base-damage 8
    :base-cooldown 0.5
    :base-area 1.0
    :base-projectile-count 1
    :max-level 8
    :attack-type :projectile-directional
    :pierce 1
    :projectile-speed 500}

   :garlic
   {:name "Garlic"
    :description "Damages nearby enemies and reduces their speed"
    :base-damage 5
    :base-cooldown 0.5
    :base-area 1.0
    :base-projectile-count 1
    :max-level 8
    :attack-type :aura
    :pierce -1
    :aura-radius 80}

   :bible
   {:name "King Bible"
    :description "Orbits around the character"
    :base-damage 15
    :base-cooldown 3.0
    :base-area 1.0
    :base-projectile-count 1
    :max-level 8
    :attack-type :orbit
    :pierce -1
    :orbit-radius 100
    :orbit-duration 3.0
    :orbit-speed 3.0}

   :cross
   {:name "Cross"
    :description "Thrown weapon that returns"
    :base-damage 20
    :base-cooldown 1.5
    :base-area 1.0
    :base-projectile-count 1
    :max-level 8
    :attack-type :boomerang
    :pierce -1
    :projectile-speed 350
    :return-distance 300}

   :axe
   {:name "Axe"
    :description "Thrown high, falls in an arc"
    :base-damage 25
    :base-cooldown 1.8
    :base-area 1.0
    :base-projectile-count 1
    :max-level 8
    :attack-type :arc
    :pierce -1
    :projectile-speed 300
    :arc-height 200}})

;; Weapon upgrade effects per level (multiplicative bonuses)
(def weapon-level-bonuses
  {1 {:damage 1.0
      :projectile-count 1
      :area 1.0
      :cooldown 1.0}
   2 {:damage 1.2
      :projectile-count 1
      :area 1.1
      :cooldown 0.95}
   3 {:damage 1.4
      :projectile-count 2
      :area 1.2
      :cooldown 0.90}
   4 {:damage 1.6
      :projectile-count 2
      :area 1.3
      :cooldown 0.85}
   5 {:damage 1.8
      :projectile-count 3
      :area 1.4
      :cooldown 0.80}
   6 {:damage 2.0
      :projectile-count 3
      :area 1.5
      :cooldown 0.75}
   7 {:damage 2.3
      :projectile-count 4
      :area 1.6
      :cooldown 0.70}
   8 {:damage 2.6
      :projectile-count 5
      :area 1.8
      :cooldown 0.65}})

;; ============================================================================
;; DATA DEFINITIONS - PASSIVE ITEMS
;; ============================================================================

(def passive-definitions
  {:spinach
   {:name "Spinach"
    :description "+10% damage per level"
    :max-level 5
    :stat :damage-multiplier
    :bonus-per-level 0.1}

   :armor
   {:name "Armor"
    :description "+1 armor per level"
    :max-level 5
    :stat :armor
    :bonus-per-level 1}

   :wings
   {:name "Wings"
    :description "+10% movement speed per level"
    :max-level 5
    :stat :move-speed
    :bonus-per-level 0.1}

   :hollow-heart
   {:name "Hollow Heart"
    :description "+20% max HP per level"
    :max-level 5
    :stat :max-hp
    :bonus-per-level 0.2}

   :candelabrador
   {:name "Candelabrador"
    :description "+10% area per level"
    :max-level 5
    :stat :area
    :bonus-per-level 0.1}

   :bracer
   {:name "Bracer"
    :description "+10% projectile speed per level"
    :max-level 5
    :stat :projectile-speed
    :bonus-per-level 0.1}

   :empty-tome
   {:name "Empty Tome"
    :description "-8% cooldown per level"
    :max-level 5
    :stat :cooldown-reduction
    :bonus-per-level 0.08}

   :attractorb
   {:name "Attractorb"
    :description "+30% pickup radius per level"
    :max-level 5
    :stat :pickup-radius
    :bonus-per-level 0.3}})

;; ============================================================================
;; DATA DEFINITIONS - ENEMIES
;; ============================================================================

(def enemy-definitions
  {:zombie
   {:name "Zombie"
    :base-hp 15
    :base-damage 10
    :base-speed 40
    :xp-value 1
    :size 20
    :color {:r 100
            :g 150
            :b 100
            :a 255}}

   :bat
   {:name "Bat"
    :base-hp 5
    :base-damage 5
    :base-speed 100
    :xp-value 1
    :size 15
    :color {:r 80
            :g 60
            :b 80
            :a 255}}

   :skeleton
   {:name "Skeleton"
    :base-hp 30
    :base-damage 15
    :base-speed 60
    :xp-value 3
    :size 22
    :color {:r 200
            :g 200
            :b 180
            :a 255}}

   :ghoul
   {:name "Ghoul"
    :base-hp 80
    :base-damage 25
    :base-speed 35
    :xp-value 5
    :size 28
    :color {:r 60
            :g 80
            :b 60
            :a 255}}

   :elite-zombie
   {:name "Elite Zombie"
    :base-hp 100
    :base-damage 20
    :base-speed 50
    :xp-value 10
    :size 30
    :color {:r 150
            :g 200
            :b 150
            :a 255}
    :elite? true}

   :boss-giant
   {:name "Giant"
    :base-hp 1000
    :base-damage 50
    :base-speed 30
    :xp-value 100
    :size 60
    :color {:r 200
            :g 50
            :b 50
            :a 255}
    :boss? true}

   :boss-reaper
   {:name "Reaper"
    :base-hp 2000
    :base-damage 80
    :base-speed 40
    :xp-value 200
    :size 70
    :color {:r 30
            :g 30
            :b 30
            :a 255}
    :boss? true}})

;; ============================================================================
;; SPAWN WAVE DEFINITIONS
;; ============================================================================

(def spawn-waves
  [{:start-time 0
    :enemies [:zombie]
    :multiplier 1.0}
   {:start-time (* 60 2)
    :enemies [:zombie :bat]
    :multiplier 1.2}
   {:start-time (* 60 4)
    :enemies [:zombie :bat :skeleton]
    :multiplier 1.5}
   {:start-time (* 60 6)
    :enemies [:bat :skeleton]
    :multiplier 1.8}
   {:start-time (* 60 8)
    :enemies [:skeleton :ghoul]
    :multiplier 2.0}
   {:start-time (* 60 10)
    :enemies [:skeleton :ghoul :elite-zombie]
    :multiplier 2.5
    :boss :boss-giant}
   {:start-time (* 60 12)
    :enemies [:ghoul :elite-zombie]
    :multiplier 3.0}
   {:start-time (* 60 15)
    :enemies [:ghoul :elite-zombie]
    :multiplier 4.0}
   {:start-time (* 60 18)
    :enemies [:elite-zombie]
    :multiplier 5.0}
   {:start-time (* 60 20)
    :enemies [:elite-zombie]
    :multiplier 6.0
    :boss :boss-reaper}
   {:start-time (* 60 25)
    :enemies [:elite-zombie :ghoul]
    :multiplier 8.0}])

;; ============================================================================
;; GAME STATE INITIALIZATION
;; ============================================================================

(defn make-player []
  {:position {:x (/ WORLD_WIDTH 2)
              :y (/ WORLD_HEIGHT 2)}
   :velocity {:x 0
              :y 0}
   :facing {:x 1
            :y 0}                                      ;; Direction player is facing
   :health PLAYER_BASE_HP
   :max-health PLAYER_BASE_HP
   :level 1
   :xp 0
   :xp-to-next BASE_XP_TO_LEVEL
   :invincibility-timer 0

   ;; Player stats (base values, modified by passives)
   :stats {:move-speed 1.0
           :damage-multiplier 1.0
           :armor 0
           :pickup-radius 1.0
           :area 1.0
           :cooldown-reduction 1.0
           :projectile-speed 1.0
           :max-hp 1.0}

   ;; Weapons and passives
   :weapons [{:type :whip
              :level 1
              :cooldown-timer 0}]
   :passives []})

(defn make-camera [player]
  {:x (- (:x (:position player)) (/ SCREEN_WIDTH 2))
   :y (- (:y (:position player)) (/ SCREEN_HEIGHT 2))})

(defn init-game []
  (let [player (make-player)]
    {:state :playing                                        ;; :playing :paused :level-up :game-over
     :player player
     :camera (make-camera player)
     :enemies []
     :projectiles []
     :xp-gems []
     :damage-numbers []
     :game-time 0.0
     :spawn-timer 0
     :enemies-killed 0
     :current-wave 0
     :boss-spawned-10 false
     :boss-spawned-20 false
     :level-up-choices []
     :screen-shake 0}))

;; ============================================================================
;; ID GENERATION
;; ============================================================================

(def ^:private id-counter (atom 0))

(defn next-id []
  (swap! id-counter inc))

;; ============================================================================
;; PLAYER STATS CALCULATION
;; ============================================================================

(defn calculate-player-stats [player]
  (let [base-stats {:move-speed 1.0
                    :damage-multiplier 1.0
                    :armor 0
                    :pickup-radius 1.0
                    :area 1.0
                    :cooldown-reduction 1.0
                    :projectile-speed 1.0
                    :max-hp 1.0}
        passives (:passives player)]
    (reduce
     (fn [stats passive]
       (let [passive-def (get passive-definitions (:type passive))
             stat-key (:stat passive-def)
             bonus (* (:level passive) (:bonus-per-level passive-def))]
         (if (= stat-key :armor)
           (update stats stat-key + bonus)
           (update stats stat-key + bonus))))
     base-stats
     passives)))

(defn get-effective-pickup-radius [player]
  (* PLAYER_PICKUP_RADIUS (get-in player [:stats :pickup-radius])))

(defn get-effective-max-hp [player]
  (* PLAYER_BASE_HP (get-in player [:stats :max-hp])))

(defn get-effective-move-speed [player]
  (* PLAYER_BASE_SPEED (get-in player [:stats :move-speed])))

;; ============================================================================
;; WEAPON CALCULATIONS
;; ============================================================================

(defn get-weapon-stats [weapon player-stats]
  (let [weapon-def (get weapon-definitions (:type weapon))
        level-bonus (get weapon-level-bonuses (:level weapon))
        cooldown-reduction (get player-stats :cooldown-reduction 1.0)]
    {:damage (* (:base-damage weapon-def)
                (:damage level-bonus)
                (get player-stats :damage-multiplier 1.0))
     :cooldown (* (:base-cooldown weapon-def)
                  (:cooldown level-bonus)
                  (- 2.0 cooldown-reduction))               ;; Convert reduction to multiplier
     :area (* (:base-area weapon-def)
              (:area level-bonus)
              (get player-stats :area 1.0))
     :projectile-count (:projectile-count level-bonus)
     :pierce (:pierce weapon-def)
     :projectile-speed (* (get weapon-def :projectile-speed 300)
                          (get player-stats :projectile-speed 1.0))}))

;; ============================================================================
;; ENEMY SPAWNING
;; ============================================================================

(defn get-current-wave [game-time]
  (last (filter #(<= (:start-time %) game-time) spawn-waves)))

(defn spawn-position-around-player [player-pos]
  (let [angle (rand-range 0 (* 2 Math/PI))
        dist (+ SPAWN_DISTANCE (rand-range 0 100))]
    {:x (+ (:x player-pos) (* (Math/cos angle) dist))
     :y (+ (:y player-pos) (* (Math/sin angle) dist))}))

(defn create-enemy [enemy-type position game-time]
  (let [enemy-def (get enemy-definitions enemy-type)
        time-multiplier (+ 1.0 (* (/ game-time 60.0) 0.02))] ;; Enemies scale over time
    {:id (next-id)
     :type enemy-type
     :position position
     :velocity {:x 0
                :y 0}
     :health (* (:base-hp enemy-def) time-multiplier)
     :max-health (* (:base-hp enemy-def) time-multiplier)
     :damage (* (:base-damage enemy-def) time-multiplier)
     :speed (:base-speed enemy-def)
     :xp-value (:xp-value enemy-def)
     :size (:size enemy-def)
     :color (:color enemy-def)
     :knockback {:x 0
                 :y 0}
     :hit-flash 0
     :boss? (get enemy-def :boss? false)
     :elite? (get enemy-def :elite? false)}))

(defn spawn-enemies [game dt]
  (let [{:keys [player game-time spawn-timer]} game
        current-wave (get-current-wave game-time)
        spawn-interval (/ BASE_SPAWN_INTERVAL (get current-wave :multiplier 1.0))
        new-spawn-timer (+ spawn-timer dt)]
    (if (>= new-spawn-timer spawn-interval)
      (let [enemies-to-spawn (int (* BASE_ENEMIES_PER_SPAWN
                                     (get current-wave :multiplier 1.0)))
            enemy-types (:enemies current-wave)
            new-enemies (vec (for [_ (range enemies-to-spawn)]
                               (create-enemy
                                (rand-nth enemy-types)
                                (spawn-position-around-player (:position player))
                                game-time)))]
        (-> game
            (update :enemies into new-enemies)
            (assoc :spawn-timer 0)))
      (assoc game :spawn-timer new-spawn-timer))))

(defn maybe-spawn-boss [game]
  (let [{:keys [game-time player boss-spawned-10 boss-spawned-20]} game]
    (cond
      (and (>= game-time BOSS_TIME_1) (not boss-spawned-10))
      (-> game
          (update :enemies conj (create-enemy :boss-giant
                                              (spawn-position-around-player (:position player))
                                              game-time))
          (assoc :boss-spawned-10 true))

      (and (>= game-time BOSS_TIME_2) (not boss-spawned-20))
      (-> game
          (update :enemies conj (create-enemy :boss-reaper
                                              (spawn-position-around-player (:position player))
                                              game-time))
          (assoc :boss-spawned-20 true))

      :else game)))

;; ============================================================================
;; PROJECTILE CREATION
;; ============================================================================

(defn create-projectile [weapon-type position velocity damage area pierce]
  {:id (next-id)
   :type weapon-type
   :position position
   :velocity velocity
   :damage damage
   :area area
   :pierce pierce
   :lifetime 5.0
   :hit-enemies #{}
   :returning? false
   :return-target nil})

(defn create-orbit-projectile [weapon-type position damage area orbit-angle orbit-radius]
  {:id (next-id)
   :type weapon-type
   :position position
   :velocity {:x 0
              :y 0}
   :damage damage
   :area area
   :pierce -1
   :lifetime 3.0
   :hit-enemies #{}
   :orbit? true
   :orbit-angle orbit-angle
   :orbit-radius orbit-radius})

;; ============================================================================
;; WEAPON ATTACKS
;; ============================================================================

(defn find-nearest-enemy [position enemies]
  (when (seq enemies)
    (reduce
     (fn [nearest enemy]
       (let [dist-nearest (distance position (:position nearest))
             dist-enemy (distance position (:position enemy))]
         (if (< dist-enemy dist-nearest) enemy nearest)))
     (first enemies)
     (rest enemies))))

(defn fire-whip [player weapon-stats]
  (let [pos (:position player)
        facing (:facing player)
        base-width 120
        width (* base-width (:area weapon-stats))
        damage (:damage weapon-stats)
        projectile-count (:projectile-count weapon-stats)
        angle-start (- (Math/atan2 (:y facing) (:x facing)) (/ Math/PI 6))
        angle-step (/ (/ Math/PI 3) (max 1 (dec projectile-count)))]
    (vec (for [i (range projectile-count)]
           (let [angle (+ angle-start (* i angle-step))
                 offset {:x (* (Math/cos angle) (/ width 2))
                         :y (* (Math/sin angle) (/ width 2))}]
             {:id (next-id)
              :type :whip
              :position (vec-add pos offset)
              :velocity {:x 0
                         :y 0}
              :damage damage
              :area (* 40 (:area weapon-stats))
              :pierce -1
              :lifetime 0.15
              :hit-enemies #{}
              :melee? true})))))

(defn fire-magic-wand [player enemies weapon-stats]
  (let [pos (:position player)
        nearest (find-nearest-enemy pos enemies)
        projectile-count (:projectile-count weapon-stats)]
    (if nearest
      (let [base-dir (normalize (vec-sub (:position nearest) pos))
            angle-spread (/ Math/PI 8)]
        (vec (for [i (range projectile-count)]
               (let [angle-offset (* (- i (/ (dec projectile-count) 2)) angle-spread)
                     base-angle (Math/atan2 (:y base-dir) (:x base-dir))
                     final-angle (+ base-angle angle-offset)
                     dir (rotate-vec final-angle)
                     vel (vec-scale dir (:projectile-speed weapon-stats))]
                 (create-projectile :magic-wand pos vel (:damage weapon-stats)
                                    (* 12 (:area weapon-stats)) (:pierce weapon-stats))))))
      [])))

(defn fire-knife [player weapon-stats]
  (let [pos (:position player)
        facing (:facing player)
        projectile-count (:projectile-count weapon-stats)
        angle-spread (/ Math/PI 12)]
    (vec (for [i (range projectile-count)]
           (let [angle-offset (* (- i (/ (dec projectile-count) 2)) angle-spread)
                 base-angle (Math/atan2 (:y facing) (:x facing))
                 final-angle (+ base-angle angle-offset)
                 dir (rotate-vec final-angle)
                 vel (vec-scale dir (:projectile-speed weapon-stats))]
             (create-projectile :knife pos vel (:damage weapon-stats)
                                (* 10 (:area weapon-stats)) (:pierce weapon-stats)))))))

(defn fire-garlic [player weapon-stats]
  ;; Garlic creates an aura effect - we'll handle this specially
  [{:id (next-id)
    :type :garlic
    :position (:position player)
    :velocity {:x 0
               :y 0}
    :damage (:damage weapon-stats)
    :area (* 80 (:area weapon-stats))
    :pierce -1
    :lifetime 0.1
    :hit-enemies #{}
    :aura? true
    :follow-player? true}])

(defn fire-bible [player weapon-stats]
  (let [pos (:position player)
        projectile-count (:projectile-count weapon-stats)
        base-radius (* 100 (:area weapon-stats))]
    (vec (for [i (range projectile-count)]
           (let [angle (* (/ (* 2 Math/PI) projectile-count) i)]
             {:id (next-id)
              :type :bible
              :position (vec-add pos {:x (* (Math/cos angle) base-radius)
                                      :y (* (Math/sin angle) base-radius)})
              :velocity {:x 0
                         :y 0}
              :damage (:damage weapon-stats)
              :area (* 20 (:area weapon-stats))
              :pierce -1
              :lifetime 3.0
              :hit-enemies #{}
              :orbit? true
              :orbit-angle angle
              :orbit-radius base-radius
              :orbit-speed 3.0})))))

(defn fire-cross [player enemies weapon-stats]
  (let [pos (:position player)
        nearest (find-nearest-enemy pos enemies)
        projectile-count (:projectile-count weapon-stats)]
    (if nearest
      (let [dir (normalize (vec-sub (:position nearest) pos))]
        (vec (for [i (range projectile-count)]
               (let [angle-offset (* (- i (/ (dec projectile-count) 2)) 0.2)
                     base-angle (Math/atan2 (:y dir) (:x dir))
                     final-dir (rotate-vec (+ base-angle angle-offset))]
                 {:id (next-id)
                  :type :cross
                  :position pos
                  :velocity (vec-scale final-dir (:projectile-speed weapon-stats))
                  :damage (:damage weapon-stats)
                  :area (* 18 (:area weapon-stats))
                  :pierce -1
                  :lifetime 4.0
                  :hit-enemies #{}
                  :boomerang? true
                  :start-pos pos
                  :max-distance (* 300 (:area weapon-stats))
                  :returning? false}))))
      [])))

(defn fire-axe [player weapon-stats]
  (let [pos (:position player)
        projectile-count (:projectile-count weapon-stats)
        facing-x (if (pos? (:x (:facing player))) 1 -1)]
    (vec (for [i (range projectile-count)]
           (let [angle-offset (* (- i (/ (dec projectile-count) 2)) 0.3)
                 base-vel-x (* facing-x (:projectile-speed weapon-stats) 0.7)
                 base-vel-y (- (* (:projectile-speed weapon-stats) 1.2))]
             {:id (next-id)
              :type :axe
              :position pos
              :velocity {:x (+ base-vel-x (* angle-offset 100))
                         :y base-vel-y}
              :damage (:damage weapon-stats)
              :area (* 22 (:area weapon-stats))
              :pierce -1
              :lifetime 3.0
              :hit-enemies #{}
              :arc? true
              :gravity 600})))))

(defn fire-weapon [weapon player enemies]
  (let [weapon-stats (get-weapon-stats weapon (:stats player))]
    (case (:type weapon)
      :whip (fire-whip player weapon-stats)
      :magic-wand (fire-magic-wand player enemies weapon-stats)
      :knife (fire-knife player weapon-stats)
      :garlic (fire-garlic player weapon-stats)
      :bible (fire-bible player weapon-stats)
      :cross (fire-cross player enemies weapon-stats)
      :axe (fire-axe player weapon-stats)
      [])))

;; ============================================================================
;; PLAYER UPDATE
;; ============================================================================

(defn update-player-movement [player dt]
  (let [move-speed (get-effective-move-speed player)
        dx (cond
             (or (rck/is-key-down? (:a enums/keyboard-key))
                 (rck/is-key-down? (:left enums/keyboard-key))) -1
             (or (rck/is-key-down? (:d enums/keyboard-key))
                 (rck/is-key-down? (:right enums/keyboard-key))) 1
             :else 0)
        dy (cond
             (or (rck/is-key-down? (:w enums/keyboard-key))
                 (rck/is-key-down? (:up enums/keyboard-key))) -1
             (or (rck/is-key-down? (:s enums/keyboard-key))
                 (rck/is-key-down? (:down enums/keyboard-key))) 1
             :else 0)
        dir (normalize {:x dx
                        :y dy})
        vel (vec-scale dir move-speed)
        new-pos (vec-add (:position player) (vec-scale vel dt))
        ;; Clamp to world bounds
        clamped-pos {:x (clamp (:x new-pos) PLAYER_SIZE (- WORLD_WIDTH PLAYER_SIZE))
                     :y (clamp (:y new-pos) PLAYER_SIZE (- WORLD_HEIGHT PLAYER_SIZE))}
        ;; Update facing direction
        new-facing (if (and (= dx 0) (= dy 0))
                     (:facing player)
                     dir)]
    (-> player
        (assoc :position clamped-pos)
        (assoc :velocity vel)
        (assoc :facing new-facing))))

(defn update-player-invincibility [player dt]
  (update player :invincibility-timer #(max 0 (- % dt))))

(defn update-player-weapons [player enemies dt]
  (let [weapons (:weapons player)
        player-stats (:stats player)]
    (reduce
     (fn [[player new-projectiles] weapon-idx]
       (let [weapon (get weapons weapon-idx)
             weapon-stats (get-weapon-stats weapon player-stats)
             new-timer (- (:cooldown-timer weapon) dt)]
         (if (<= new-timer 0)
            ;; Fire weapon
           (let [projectiles (fire-weapon weapon player enemies)
                 updated-weapon (assoc weapon :cooldown-timer (:cooldown weapon-stats))]
             [(assoc-in player [:weapons weapon-idx] updated-weapon)
              (into new-projectiles projectiles)])
            ;; Just update timer
           [(assoc-in player [:weapons weapon-idx :cooldown-timer] new-timer)
            new-projectiles])))
     [player []]
     (range (count weapons)))))

(defn update-player [game dt]
  (let [{:keys [player enemies]} game
        ;; Recalculate stats based on passives
        new-stats (calculate-player-stats player)
        player (assoc player :stats new-stats)
        player (update-player-movement player dt)
        player (update-player-invincibility player dt)
        [player new-projectiles] (update-player-weapons player enemies dt)]
    (-> game
        (assoc :player player)
        (update :projectiles into new-projectiles))))

;; ============================================================================
;; ENEMY UPDATE
;; ============================================================================

(defn update-enemy [enemy player dt]
  (let [dir (normalize (vec-sub (:position player) (:position enemy)))
        ;; Apply knockback
        kb (:knockback enemy)
        kb-decay 0.9
        new-kb (vec-scale kb kb-decay)

        ;; Movement towards player
        move-vel (vec-scale dir (:speed enemy))
        total-vel (vec-add move-vel new-kb)

        new-pos (vec-add (:position enemy) (vec-scale total-vel dt))

        ;; Update hit flash
        new-flash (max 0 (- (:hit-flash enemy) dt))]
    (-> enemy
        (assoc :position new-pos)
        (assoc :knockback new-kb)
        (assoc :hit-flash new-flash))))

(defn update-enemies [game dt]
  (let [{:keys [player]} game]
    (update game :enemies
            (fn [enemies]
              (mapv #(update-enemy % player dt) enemies)))))

;; ============================================================================
;; PROJECTILE UPDATE
;; ============================================================================

(defn update-projectile [proj player dt]
  (let [{:keys [orbit? arc? boomerang? follow-player?]} proj]
    (cond
      ;; Follow player (garlic aura)
      follow-player?
      (-> proj
          (assoc :position (:position player))
          (update :lifetime - dt))

      ;; Orbiting projectile (bible)
      orbit?
      (let [new-angle (+ (:orbit-angle proj) (* (:orbit-speed proj 3.0) dt))
            orbit-pos {:x (* (Math/cos new-angle) (:orbit-radius proj))
                       :y (* (Math/sin new-angle) (:orbit-radius proj))}]
        (-> proj
            (assoc :position (vec-add (:position player) orbit-pos))
            (assoc :orbit-angle new-angle)
            (update :lifetime - dt)))

      ;; Arc projectile (axe) - apply gravity
      arc?
      (let [new-vel-y (+ (:y (:velocity proj)) (* (:gravity proj) dt))
            new-vel (assoc (:velocity proj) :y new-vel-y)
            new-pos (vec-add (:position proj) (vec-scale new-vel dt))]
        (-> proj
            (assoc :velocity new-vel)
            (assoc :position new-pos)
            (update :lifetime - dt)))

      ;; Boomerang projectile (cross)
      boomerang?
      (let [dist (distance (:start-pos proj) (:position proj))
            returning? (or (:returning? proj) (>= dist (:max-distance proj)))
            new-vel (if returning?
                      (vec-scale (normalize (vec-sub (:position player) (:position proj)))
                                 (vec-len (:velocity proj)))
                      (:velocity proj))
            new-pos (vec-add (:position proj) (vec-scale new-vel dt))]
        (-> proj
            (assoc :velocity new-vel)
            (assoc :position new-pos)
            (assoc :returning? returning?)
            (update :lifetime - dt)))

      ;; Normal projectile
      :else
      (let [new-pos (vec-add (:position proj) (vec-scale (:velocity proj) dt))]
        (-> proj
            (assoc :position new-pos)
            (update :lifetime - dt))))))

(defn update-projectiles [game dt]
  (let [{:keys [player]} game]
    (update game :projectiles
            (fn [projectiles]
              (->> projectiles
                   (mapv #(update-projectile % player dt))
                   (filterv #(> (:lifetime %) 0)))))))

;; ============================================================================
;; XP GEM UPDATE
;; ============================================================================

(defn update-xp-gem [gem player dt]
  (let [pickup-radius (get-effective-pickup-radius player)
        player-pos (:position player)
        gem-pos (:position gem)
        dist (distance gem-pos player-pos)
        magnetized? (< dist (* pickup-radius 2))]
    (if magnetized?
      (let [dir (normalize (vec-sub player-pos gem-pos))
            speed 400
            new-pos (vec-add gem-pos (vec-scale dir (* speed dt)))]
        (assoc gem :position new-pos :magnetized? true))
      gem)))

(defn update-xp-gems [game dt]
  (let [{:keys [player]} game]
    (update game :xp-gems
            (fn [gems]
              (mapv #(update-xp-gem % player dt) gems)))))

;; ============================================================================
;; COLLISION DETECTION
;; ============================================================================

(defn check-circle-collision [pos1 r1 pos2 r2]
  (< (distance pos1 pos2) (+ r1 r2)))

(defn create-damage-number [position damage critical?]
  {:id (next-id)
   :position position
   :damage damage
   :lifetime DAMAGE_NUMBER_LIFETIME
   :velocity {:x (rand-range -20 20)
              :y -50}
   :critical? critical?})

(defn create-xp-gem [position value]
  {:id (next-id)
   :position position
   :xp-value value
   :magnetized? false})

;; Projectile-Enemy collisions
(defn handle-projectile-enemy-collision [game]
  (let [{:keys [projectiles enemies player]} game
        player-pos (:position player)]
    ;; Process each projectile against all enemies
    (loop [remaining-projectiles projectiles
           remaining-enemies enemies
           new-damage-numbers []
           new-xp-gems []
           proj-idx 0
           killed-count 0]
      (if (>= proj-idx (count remaining-projectiles))
        (-> game
            (assoc :projectiles (vec remaining-projectiles))
            (assoc :enemies (vec remaining-enemies))
            (update :damage-numbers into new-damage-numbers)
            (update :xp-gems into new-xp-gems)
            (update :enemies-killed + killed-count))
        (let [proj (nth remaining-projectiles proj-idx)
              proj-pos (:position proj)
              proj-radius (:area proj)
              ;; Process this projectile against all enemies using filter-based approach
              [updated-proj surviving-enemies dmg-nums gems killed]
              (reduce
               (fn [[p surviving dmg-nums gems killed] enemy]
                 (let [enemy-id (:id enemy)]
                   (if (and (not (contains? (:hit-enemies p) enemy-id))
                            (check-circle-collision proj-pos proj-radius
                                                    (:position enemy) (:size enemy)))
                      ;; Hit!
                     (let [damage (:damage p)
                           new-enemy-health (- (:health enemy) damage)
                           kb-dir (normalize (vec-sub (:position enemy) player-pos))
                           kb-force (vec-scale kb-dir 200)

                            ;; Update projectile pierce
                           new-pierce (if (= (:pierce p) -1)
                                        -1
                                        (dec (:pierce p)))
                           updated-p (-> p
                                         (update :hit-enemies conj enemy-id)
                                         (assoc :pierce new-pierce))

                            ;; Create damage number
                           dmg-num (create-damage-number (:position enemy) damage false)]
                       (if (<= new-enemy-health 0)
                          ;; Enemy died - don't add to surviving, create gem
                         (let [gem (create-xp-gem (:position enemy) (:xp-value enemy))]
                           [updated-p
                            surviving
                            (conj dmg-nums dmg-num)
                            (conj gems gem)
                            (inc killed)])
                          ;; Enemy damaged - add updated enemy to surviving
                         [updated-p
                          (conj surviving (-> enemy
                                              (assoc :health new-enemy-health)
                                              (update :knockback vec-add kb-force)
                                              (assoc :hit-flash HIT_FLASH_DURATION)))
                          (conj dmg-nums dmg-num)
                          gems
                          killed]))
                      ;; No collision - keep enemy as-is
                     [p (conj surviving enemy) dmg-nums gems killed])))
               [proj [] new-damage-numbers new-xp-gems killed-count]
               remaining-enemies)
              ;; Check if projectile should be removed (pierce exhausted)
              keep-proj? (or (= (:pierce updated-proj) -1)
                             (> (:pierce updated-proj) 0))
              new-remaining (if keep-proj?
                              (assoc remaining-projectiles proj-idx updated-proj)
                              (vec (concat (subvec remaining-projectiles 0 proj-idx)
                                           (subvec remaining-projectiles (inc proj-idx)))))]
          (recur new-remaining
                 surviving-enemies
                 dmg-nums
                 gems
                 (if keep-proj? (inc proj-idx) proj-idx)
                 killed))))))

;; Player-Enemy collision (damage to player)
(defn handle-player-enemy-collision [game]
  (let [{:keys [player enemies]} game
        player-pos (:position player)
        invincible? (> (:invincibility-timer player) 0)]
    (if invincible?
      game
      (let [colliding-enemy (first (filter
                                    #(check-circle-collision player-pos (/ PLAYER_SIZE 2)
                                                             (:position %) (:size %))
                                    enemies))]
        (if colliding-enemy
          (let [raw-damage (:damage colliding-enemy)
                armor (get-in player [:stats :armor] 0)
                actual-damage (max 1 (- raw-damage armor))
                new-health (- (:health player) actual-damage)]
            (-> game
                (assoc-in [:player :health] new-health)
                (assoc-in [:player :invincibility-timer] PLAYER_INVINCIBILITY_TIME)
                (update :damage-numbers conj
                        (create-damage-number player-pos actual-damage false))
                (assoc :screen-shake 0.2)))
          game)))))

;; Player-XP gem collection
(defn handle-xp-collection [game]
  (let [{:keys [player xp-gems]} game
        player-pos (:position player)
        pickup-radius (get-effective-pickup-radius player)
        [collected remaining]
        (reduce
         (fn [[collected remaining] gem]
           (if (< (distance player-pos (:position gem)) (/ pickup-radius 2))
             [(+ collected (:xp-value gem)) remaining]
             [collected (conj remaining gem)]))
         [0 []]
         xp-gems)]
    (if (> collected 0)
      (-> game
          (update-in [:player :xp] + collected)
          (assoc :xp-gems remaining))
      game)))

;; ============================================================================
;; LEVELING UP
;; ============================================================================

(defn xp-to-next-level [level]
  (int (* BASE_XP_TO_LEVEL (Math/pow XP_LEVEL_MULTIPLIER (dec level)))))

(defn generate-level-up-choices [player]
  (let [current-weapons (set (map :type (:weapons player)))
        current-passives (into {} (map (fn [p] [(:type p) (:level p)]) (:passives player)))

        ;; Available weapon upgrades
        weapon-upgrades (for [w (:weapons player)
                              :let [wdef (get weapon-definitions (:type w))]
                              :when (< (:level w) (:max-level wdef))]
                          {:type :weapon-upgrade
                           :weapon-type (:type w)
                           :current-level (:level w)})

        ;; Available new weapons
        new-weapons (for [[wtype _wdef] weapon-definitions
                          :when (and (not (contains? current-weapons wtype))
                                     (< (count current-weapons) 6))]
                      {:type :new-weapon
                       :weapon-type wtype})

        ;; Available passive upgrades
        passive-upgrades (for [[ptype plevel] current-passives
                               :let [pdef (get passive-definitions ptype)]
                               :when (< plevel (:max-level pdef))]
                           {:type :passive-upgrade
                            :passive-type ptype
                            :current-level plevel})

        ;; Available new passives
        new-passives (for [[ptype _pdef] passive-definitions
                           :when (not (contains? current-passives ptype))]
                       {:type :new-passive
                        :passive-type ptype})

        all-choices (concat weapon-upgrades new-weapons passive-upgrades new-passives)]
    (vec (take 4 (shuffle all-choices)))))

(defn check-level-up [game]
  (let [{:keys [player]} game
        {:keys [xp level]} player
        xp-needed (xp-to-next-level level)]
    (if (>= xp xp-needed)
      (let [new-player (-> player
                           (update :xp - xp-needed)
                           (update :level inc))
            choices (generate-level-up-choices new-player)]
        (-> game
            (assoc :player new-player)
            (assoc :state :level-up)
            (assoc :level-up-choices choices)))
      game)))

(defn apply-level-up-choice [game choice-idx]
  (let [{:keys [player level-up-choices]} game
        choice (get level-up-choices choice-idx)]
    (if choice
      (let [new-player
            (case (:type choice)
              :weapon-upgrade
              (let [weapon-idx (first (keep-indexed
                                       (fn [i w] (when (= (:type w) (:weapon-type choice)) i))
                                       (:weapons player)))]
                (update-in player [:weapons weapon-idx :level] inc))

              :new-weapon
              (update player :weapons conj {:type (:weapon-type choice)
                                            :level 1
                                            :cooldown-timer 0})

              :passive-upgrade
              (let [passive-idx (first (keep-indexed
                                        (fn [i p] (when (= (:type p) (:passive-type choice)) i))
                                        (:passives player)))]
                (update-in player [:passives passive-idx :level] inc))

              :new-passive
              (update player :passives conj {:type (:passive-type choice)
                                             :level 1})

              player)]
        (-> game
            (assoc :player new-player)
            (assoc :state :playing)
            (assoc :level-up-choices [])))
      game)))

;; ============================================================================
;; CAMERA UPDATE
;; ============================================================================

(defn update-camera [game]
  (let [{:keys [player camera screen-shake]} game
        target-x (- (:x (:position player)) (/ SCREEN_WIDTH 2))
        target-y (- (:y (:position player)) (/ SCREEN_HEIGHT 2))
        ;; Smooth camera follow
        lerp-speed 0.1
        new-x (lerp (:x camera) target-x lerp-speed)
        new-y (lerp (:y camera) target-y lerp-speed)
        ;; Clamp to world bounds
        clamped-x (clamp new-x 0 (- WORLD_WIDTH SCREEN_WIDTH))
        clamped-y (clamp new-y 0 (- WORLD_HEIGHT SCREEN_HEIGHT))
        ;; Apply screen shake
        shake-x (if (> screen-shake 0) (rand-range -5 5) 0)
        shake-y (if (> screen-shake 0) (rand-range -5 5) 0)]
    (-> game
        (assoc :camera {:x (+ clamped-x shake-x)
                        :y (+ clamped-y shake-y)})
        (update :screen-shake #(max 0 (- % 0.016))))))

;; ============================================================================
;; DAMAGE NUMBERS UPDATE
;; ============================================================================

(defn update-damage-numbers [game dt]
  (update game :damage-numbers
          (fn [nums]
            (->> nums
                 (mapv (fn [n]
                         (-> n
                             (update :lifetime - dt)
                             (update :position vec-add (vec-scale (:velocity n) dt)))))
                 (filterv #(> (:lifetime %) 0))))))

;; ============================================================================
;; GAME OVER CHECK
;; ============================================================================

(defn check-game-over [game]
  (if (<= (get-in game [:player :health]) 0)
    (assoc game :state :game-over)
    game))

;; ============================================================================
;; MAIN UPDATE LOOP
;; ============================================================================

(defn update-playing [game dt]
  (-> game
      (update :game-time + dt)
      (update-player dt)
      (update-enemies dt)
      (update-projectiles dt)
      (update-xp-gems dt)
      (spawn-enemies dt)
      (maybe-spawn-boss)
      (handle-projectile-enemy-collision)
      (handle-player-enemy-collision)
      (handle-xp-collection)
      (update-damage-numbers dt)
      (check-level-up)
      (check-game-over)
      (update-camera)))

(defn update-game [game]
  (debug-stats/update!)

  ;; Handle F11 fullscreen toggle
  (when (rck/is-key-pressed? (:f11 enums/keyboard-key))
    (rcw/toggle-borderless-windowed!))

  (let [dt (rct/get-frame-time)]
    (case (:state game)
      :playing
      (if (rck/is-key-pressed? (:escape enums/keyboard-key))
        (assoc game :state :paused)
        (update-playing game dt))

      :paused
      (if (rck/is-key-pressed? (:escape enums/keyboard-key))
        (assoc game :state :playing)
        game)

      :level-up
      (let [choice-key (cond
                         (rck/is-key-pressed? (:one enums/keyboard-key)) 0
                         (rck/is-key-pressed? (:two enums/keyboard-key)) 1
                         (rck/is-key-pressed? (:three enums/keyboard-key)) 2
                         (rck/is-key-pressed? (:four enums/keyboard-key)) 3
                         :else nil)]
        (if choice-key
          (apply-level-up-choice game choice-key)
          game))

      :game-over
      (if (rck/is-key-pressed? (:enter enums/keyboard-key))
        (init-game)
        game)

      game)))

;; ============================================================================
;; DRAWING HELPERS
;; ============================================================================

(defn world-to-screen [pos camera]
  {:x (- (:x pos) (:x camera))
   :y (- (:y pos) (:y camera))})

(defn on-screen? [screen-pos size]
  (and (> (:x screen-pos) (- size))
       (< (:x screen-pos) (+ SCREEN_WIDTH size))
       (> (:y screen-pos) (- size))
       (< (:y screen-pos) (+ SCREEN_HEIGHT size))))

;; ============================================================================
;; DRAWING - BACKGROUND
;; ============================================================================

(defn draw-background [camera]
  (let [grid-size 100
        start-x (- (mod (:x camera) grid-size))
        start-y (- (mod (:y camera) grid-size))
        dark-bg {:r 15
                 :g 15
                 :b 25
                 :a 255}
        grid-color {:r 30
                    :g 30
                    :b 45
                    :a 255}]
    ;; Background
    #_(rsb/draw-rectangle! 0 0 SCREEN_WIDTH SCREEN_HEIGHT dark-bg)
    ;; Grid lines
    (doseq [x (range start-x (+ SCREEN_WIDTH grid-size) grid-size)]
      (rsb/draw-line! x 0 x SCREEN_HEIGHT grid-color))
    (doseq [y (range start-y (+ SCREEN_HEIGHT grid-size) grid-size)]
      (rsb/draw-line! 0 y SCREEN_WIDTH y grid-color))))

;; ============================================================================
;; DRAWING - ENTITIES
;; ============================================================================

(defn draw-player [player camera]
  (let [screen-pos (world-to-screen (:position player) camera)
        x (int (:x screen-pos))
        y (int (:y screen-pos))
        invincible? (> (:invincibility-timer player) 0)
        flash? (and invincible? (even? (int (* (:invincibility-timer player) 10))))
        color (if flash?
                {:r 255
                 :g 255
                 :b 255
                 :a 150}
                {:r 100
                 :g 150
                 :b 255
                 :a 255})]
    ;; Body
    (rsb/draw-circle! x y PLAYER_SIZE color)
    ;; Direction indicator
    (let [facing (:facing player)
          indicator-pos {:x (+ x (* (:x facing) 25))
                         :y (+ y (* (:y facing) 25))}]
      (rsb/draw-circle! (int (:x indicator-pos)) (int (:y indicator-pos)) 6
                        {:r 255
                         :g 255
                         :b 100
                         :a 255}))))

(defn draw-enemies [enemies camera]
  (doseq [enemy enemies]
    (let [screen-pos (world-to-screen (:position enemy) camera)]
      (when (on-screen? screen-pos (:size enemy))
        (let [x (int (:x screen-pos))
              y (int (:y screen-pos))
              size (:size enemy)
              base-color (:color enemy)
              ;; Flash white when hit
              color (if (> (:hit-flash enemy) 0)
                      colors/white
                      base-color)
              ;; Health bar for bosses/elites
              show-health? (or (:boss? enemy) (:elite? enemy))]
          ;; Enemy body
          (rsb/draw-circle! x y size color)
          ;; Eyes
          (let [eye-offset (* size 0.3)]
            (rsb/draw-circle! (int (- x eye-offset)) (int (- y (* size 0.2))) 3 colors/red)
            (rsb/draw-circle! (int (+ x eye-offset)) (int (- y (* size 0.2))) 3 colors/red))
          ;; Health bar
          (when show-health?
            (let [bar-width (* size 2)
                  bar-height 6
                  health-pct (/ (:health enemy) (:max-health enemy))
                  bar-x (int (- x (/ bar-width 2)))
                  bar-y (int (- y size 10))]
              (rsb/draw-rectangle! bar-x bar-y bar-width bar-height colors/darkgray)
              (rsb/draw-rectangle! bar-x bar-y (int (* bar-width health-pct)) bar-height colors/red))))))))

(defn draw-projectiles [projectiles camera]
  (doseq [proj projectiles]
    (let [screen-pos (world-to-screen (:position proj) camera)]
      (when (on-screen? screen-pos (:area proj))
        (let [x (int (:x screen-pos))
              y (int (:y screen-pos))
              size (:area proj)
              color (case (:type proj)
                      :whip {:r 200
                             :g 150
                             :b 50
                             :a 200}
                      :magic-wand {:r 150
                                   :g 100
                                   :b 255
                                   :a 255}
                      :knife {:r 200
                              :g 200
                              :b 200
                              :a 255}
                      :garlic {:r 200
                               :g 255
                               :b 200
                               :a 100}
                      :bible {:r 255
                              :g 255
                              :b 100
                              :a 255}
                      :cross {:r 255
                              :g 255
                              :b 255
                              :a 255}
                      :axe {:r 150
                            :g 100
                            :b 50
                            :a 255}
                      colors/white)]
          ;; Different shapes for different weapons
          (case (:type proj)
            :garlic (rsb/draw-circle! x y size {:r 200
                                                :g 255
                                                :b 200
                                                :a 50})
            :whip (rsb/draw-rectangle! (int (- x size)) (int (- y 5)) (int (* size 2)) 10 color)
            (rsb/draw-circle! x y size color)))))))

(defn draw-xp-gems [gems camera]
  (doseq [gem gems]
    (let [screen-pos (world-to-screen (:position gem) camera)]
      (when (on-screen? screen-pos 10)
        (let [x (int (:x screen-pos))
              y (int (:y screen-pos))
              size (+ 6 (* (:xp-value gem) 0.5))
              glow-size (+ size 3)
              color (cond
                      (>= (:xp-value gem) 10) {:r 100
                                               :g 255
                                               :b 100
                                               :a 255}
                      (>= (:xp-value gem) 5) {:r 100
                                              :g 200
                                              :b 255
                                              :a 255}
                      :else {:r 100
                             :g 150
                             :b 255
                             :a 255})]
          ;; Glow
          (rsb/draw-circle! x y glow-size {:r (:r color)
                                           :g (:g color)
                                           :b (:b color)
                                           :a 100})
          ;; Gem
          (rsb/draw-circle! x y size color))))))

(defn draw-damage-numbers [damage-numbers camera]
  (doseq [num damage-numbers]
    (let [screen-pos (world-to-screen (:position num) camera)
          alpha (int (* 255 (/ (:lifetime num) DAMAGE_NUMBER_LIFETIME)))
          color {:r 255
                 :g (if (:critical? num) 100 255)
                 :b 100
                 :a alpha}]
      (rtd/draw-text! (str (int (:damage num)))
                      (int (:x screen-pos)) (int (:y screen-pos))
                      20 color))))

;; ============================================================================
;; DRAWING - UI
;; ============================================================================
(defn draw-health-bar [player]
  (let [max-hp (get-effective-max-hp player)
        current-hp (max 0 (:health player))
        hp-pct (/ current-hp max-hp)
        bar-x 20
        bar-y 20
        bar-width 250
        bar-height 25]
    ;; Background
    (rsb/draw-rectangle! bar-x bar-y bar-width bar-height {:r 60
                                                           :g 20
                                                           :b 20
                                                           :a 255})
    ;; Health
    (rsb/draw-rectangle! bar-x bar-y (int (* bar-width hp-pct)) bar-height
                         {:r 200
                          :g 50
                          :b 50
                          :a 255})
    ;; Border
    (rsb/draw-line! bar-x bar-y (+ bar-x bar-width) bar-y colors/white)
    (rsb/draw-line! bar-x (+ bar-y bar-height) (+ bar-x bar-width) (+ bar-y bar-height) colors/white)
    (rsb/draw-line! bar-x bar-y bar-x (+ bar-y bar-height) colors/white)
    (rsb/draw-line! (+ bar-x bar-width) bar-y (+ bar-x bar-width) (+ bar-y bar-height) colors/white)
    ;; Text
    (rtd/draw-text! (str (int current-hp) "/" (int max-hp))
                    (+ bar-x 10) (+ bar-y 3) 20 colors/white)))

(defn draw-xp-bar [player]
  (let [current-xp (:xp player)
        xp-needed (xp-to-next-level (:level player))
        xp-pct (/ current-xp xp-needed)
        bar-x 20
        bar-y 50
        bar-width 250
        bar-height 15]
    ;; Background
    (rsb/draw-rectangle! bar-x bar-y bar-width bar-height {:r 20
                                                           :g 20
                                                           :b 60
                                                           :a 255})
    ;; XP
    (rsb/draw-rectangle! bar-x bar-y (int (* bar-width xp-pct)) bar-height
                         {:r 80
                          :g 80
                          :b 200
                          :a 255})
    ;; Level text
    (rtd/draw-text! (str "LV " (:level player))
                    (+ bar-x bar-width 10) (+ bar-y -2) 18 colors/white)))

(defn format-time [seconds]
  (let [mins (int (/ seconds 60))
        secs (int (mod seconds 60))]
    (format "%02d:%02d" mins secs)))

(defn draw-timer [game-time]
  (let [time-str (format-time game-time)]
    (rtd/draw-text! time-str (- (/ SCREEN_WIDTH 2) 30) 20 30 colors/white)))

(defn draw-weapon-icons [player]
  (let [weapons (:weapons player)
        start-x 20
        start-y 80
        icon-size 40
        spacing 5]
    (doseq [[idx weapon] (map-indexed vector weapons)]
      (let [x (+ start-x (* idx (+ icon-size spacing)))
            color (case (:type weapon)
                    :whip {:r 200
                           :g 150
                           :b 50
                           :a 255}
                    :magic-wand {:r 150
                                 :g 100
                                 :b 255
                                 :a 255}
                    :knife {:r 200
                            :g 200
                            :b 200
                            :a 255}
                    :garlic {:r 150
                             :g 255
                             :b 150
                             :a 255}
                    :bible {:r 255
                            :g 255
                            :b 100
                            :a 255}
                    :cross {:r 255
                            :g 255
                            :b 255
                            :a 255}
                    :axe {:r 150
                          :g 100
                          :b 50
                          :a 255}
                    colors/gray)]
        ;; Icon background
        (rsb/draw-rectangle! x start-y icon-size icon-size {:r 40
                                                            :g 40
                                                            :b 40
                                                            :a 255})
        ;; Weapon symbol
        (rsb/draw-circle! (+ x (/ icon-size 2)) (+ start-y (/ icon-size 2)) 12 color)
        ;; Level
        (rtd/draw-text! (str (:level weapon))
                        (+ x icon-size -12) (+ start-y icon-size -14) 12 colors/white)))))

(defn draw-kill-counter [enemies-killed]
  (rtd/draw-text! (str "Kills: " enemies-killed)
                  (- SCREEN_WIDTH 120) 20 20 colors/white))

;; ============================================================================
;; DRAWING - GAME STATES
;; ============================================================================

(defn draw-pause-menu []
  (let [overlay-color {:r 0
                       :g 0
                       :b 0
                       :a 180}]
    (rsb/draw-rectangle! 0 0 SCREEN_WIDTH SCREEN_HEIGHT overlay-color)
    (let [text "PAUSED"
          size 60
          width (rtd/measure-text text size)]
      (rtd/draw-text! text (int (- (/ SCREEN_WIDTH 2) (/ width 2)))
                      (int (- (/ SCREEN_HEIGHT 2) 50)) size colors/white))
    (let [text "Press ESC to continue"
          size 20
          width (rtd/measure-text text size)]
      (rtd/draw-text! text (int (- (/ SCREEN_WIDTH 2) (/ width 2)))
                      (int (+ (/ SCREEN_HEIGHT 2) 20)) size colors/gray))))

(defn draw-level-up-menu [choices _player]
  (let [overlay-color {:r 0
                       :g 0
                       :b 50
                       :a 200}]
    (rsb/draw-rectangle! 0 0 SCREEN_WIDTH SCREEN_HEIGHT overlay-color)

    ;; Title
    (let [text "LEVEL UP!"
          size 50
          width (rtd/measure-text text size)]
      (rtd/draw-text! text (int (- (/ SCREEN_WIDTH 2) (/ width 2))) 80 size colors/gold))

    ;; Choices
    (let [choice-height 120
          choice-width 280
          start-y 180
          total-width (* (count choices) choice-width)
          start-x (- (/ SCREEN_WIDTH 2) (/ total-width 2))]
      (doseq [[idx choice] (map-indexed vector choices)]
        (let [x (+ start-x (* idx choice-width) 10)
              y start-y
              box-width (- choice-width 20)]
          ;; Box background
          (rsb/draw-rectangle! (int x) (int y) (int box-width) choice-height
                               {:r 40
                                :g 40
                                :b 60
                                :a 255})
          ;; Number key
          (rtd/draw-text! (str "[" (inc idx) "]")
                          (int (+ x 10)) (int (+ y 10)) 24 colors/yellow)
          ;; Choice info
          (let [[name desc]
                (case (:type choice)
                  :weapon-upgrade
                  (let [wdef (get weapon-definitions (:weapon-type choice))]
                    [(:name wdef)
                     (str "Level " (:current-level choice) " -> " (inc (:current-level choice)))])

                  :new-weapon
                  (let [wdef (get weapon-definitions (:weapon-type choice))]
                    [(str "NEW: " (:name wdef))
                     (:description wdef)])

                  :passive-upgrade
                  (let [pdef (get passive-definitions (:passive-type choice))]
                    [(:name pdef)
                     (str "Level " (:current-level choice) " -> " (inc (:current-level choice)))])

                  :new-passive
                  (let [pdef (get passive-definitions (:passive-type choice))]
                    [(str "NEW: " (:name pdef))
                     (:description pdef)])

                  ["Unknown" ""])]
            (rtd/draw-text! name (int (+ x 10)) (int (+ y 45)) 20 colors/white)
            (rtd/draw-text! desc (int (+ x 10)) (int (+ y 75)) 14 colors/lightgray)))))))

(defn draw-game-over [game]
  (let [overlay-color {:r 100
                       :g 0
                       :b 0
                       :a 200}]
    (rsb/draw-rectangle! 0 0 SCREEN_WIDTH SCREEN_HEIGHT overlay-color)

    (let [text "GAME OVER"
          size 60
          width (rtd/measure-text text size)]
      (rtd/draw-text! text (int (- (/ SCREEN_WIDTH 2) (/ width 2)))
                      (int (- (/ SCREEN_HEIGHT 2) 100)) size colors/red))

    ;; Stats
    (let [stats-y (int (- (/ SCREEN_HEIGHT 2) 20))]
      (rtd/draw-text! (str "Time Survived: " (format-time (:game-time game)))
                      (int (- (/ SCREEN_WIDTH 2) 100)) stats-y 24 colors/white)
      (rtd/draw-text! (str "Level Reached: " (get-in game [:player :level]))
                      (int (- (/ SCREEN_WIDTH 2) 100)) (+ stats-y 35) 24 colors/white)
      (rtd/draw-text! (str "Enemies Killed: " (:enemies-killed game))
                      (int (- (/ SCREEN_WIDTH 2) 100)) (+ stats-y 70) 24 colors/white))

    (let [text "Press ENTER to play again"
          size 24
          width (rtd/measure-text text size)]
      (rtd/draw-text! text (int (- (/ SCREEN_WIDTH 2) (/ width 2)))
                      (int (+ (/ SCREEN_HEIGHT 2) 100)) size colors/gray))))

;; ============================================================================
;; MAIN DRAW
;; ============================================================================

(defn draw-game [game]
  (rcd/begin-drawing!)

  (when game
    (let [{:keys [camera player enemies projectiles xp-gems damage-numbers state]} game]
      (rcd/clear-background! colors/black)
      ;; Background
      (draw-background camera)

      ;; Game entities (when not paused, still show them)
      (draw-xp-gems xp-gems camera)
      (draw-projectiles projectiles camera)
      (draw-enemies enemies camera)
      (draw-player player camera)
      (draw-damage-numbers damage-numbers camera)

      ;; UI
      (draw-health-bar player)
      (draw-xp-bar player)
      (draw-timer (:game-time game))
      (draw-weapon-icons player)
      (draw-kill-counter (:enemies-killed game))

      ;; State-specific overlays
      (case state
        :paused (draw-pause-menu)
        :level-up (draw-level-up-menu (:level-up-choices game) player)
        :game-over (draw-game-over game)
        nil)

      ;; Controls info
      (rtd/draw-text! "WASD: Move | ESC: Pause | F1: Debug | F11: Fullscreen"
                      10 (- SCREEN_HEIGHT 25) 14 colors/darkgray)

      ;; Debug stats
      (debug-stats/draw!)))

  (rcd/end-drawing!))

;; ============================================================================
;; MAIN
;; ============================================================================

(def game-state (atom nil))

(defn -main [& _args]
  (nrepl/start {:port 7888})
  (rcw/set-config-flags :flag/window-resizable :flag/vsync-hint)
  (rcw/init-window! SCREEN_WIDTH SCREEN_HEIGHT "Vampire Survivors Clone")
  (rct/set-target-fps! 155)

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
