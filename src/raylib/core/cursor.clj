(ns raylib.core.cursor
  "Cursor visibility and lock state.

   Mirrors raylib.h's own \"Cursor-related functions\" section, which sits
   between the window and drawing groups. Three of these previously lived
   in `raylib.core.camera3d`, where a first-person example had needed them
   first; they are not camera functions."
  (:require
   [raylib.core]
   [raylib.internals :as ri]
   [coffi.ffi :refer [defcfn]]
   [coffi.mem :as mem]))

(defcfn show-cursor!
  "Show cursor"
  "ShowCursor"
  [] ::mem/void)

(defcfn hide-cursor!
  "Hide cursor"
  "HideCursor"
  [] ::mem/void)

(defcfn is-cursor-hidden?
  "Check if cursor is not visible"
  "IsCursorHidden"
  [] ::ri/bool)

(defcfn enable-cursor!
  "Enable cursor (unlock cursor)"
  "EnableCursor"
  [] ::mem/void)

(defcfn disable-cursor!
  "Disable cursor (lock cursor)"
  "DisableCursor"
  [] ::mem/void)

(defcfn is-cursor-on-screen?
  "Check if cursor is on the screen"
  "IsCursorOnScreen"
  [] ::ri/bool)
