(ns raylib.structs
  (:require
   [coffi.mem :as mem :refer [defalias]]
   [raylib.internals :as ri]))

(defalias ::color
  [::mem/struct
   [[:r ::ri/ubyte]
    [:g ::ri/ubyte]
    [:b ::ri/ubyte]
    [:a ::ri/ubyte]]])

(defalias ::vector-2
  [::mem/struct
   [[:x ::mem/float]
    [:y ::mem/float]]])

(defalias ::vector-3
  [::mem/struct
   [[:x ::mem/float]
    [:y ::mem/float]
    [:z ::mem/float]]])

(defalias ::vector-4
  [::mem/struct
   [[:x ::mem/float]
    [:y ::mem/float]
    [:z ::mem/float]
    [:w ::mem/float]]])

(defalias ::texture
  [::mem/struct
   [[:id ::mem/int]
    [:width ::mem/int]
    [:height ::mem/int]
    [:mipmaps ::mem/int]
    [:format ::mem/int]]])

(defalias ::render-texture
  [::mem/struct
   [[:id ::mem/int]
    [:texture ::texture]
    [:depth ::texture]]])
