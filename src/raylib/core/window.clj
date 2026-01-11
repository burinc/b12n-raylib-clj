(ns raylib.core.window
  (:require
   [raylib.core]
   [raylib.internals :as ri]
   [coffi.mem :as mem]
   [coffi.ffi :refer [defcfn]]))

(defcfn init-window!
  "Initialize window and OpenGL context"
  {:arglists '([width height title])}
  "InitWindow"
  [::mem/int ::mem/int ::mem/c-string] ::mem/void)

(defcfn window-should-close?
  "Check if KEY_ESCAPE pressed or Close icon pressed"
  "WindowShouldClose"
  [] ::ri/bool)

(defcfn close-window!
  "Close window and unload OpenGL context"
  "CloseWindow" [] ::mem/void)

(defcfn is-window-ready?
  "Check if window has been initialized successfully"
  "IsWindowReady"
  [] ::ri/bool)

(defcfn is-window-fullscreen?
  "Check if window is currently fullscreen"
  "IsWindowFullscreen"
  [] ::ri/bool)

(defcfn is-window-hidden?
  "Check if window is currently hidden (only PLATFORM_DESKTOP)"
  "IsWindowHidden"
  [] ::ri/bool)

(defcfn is-window-minimized?
  "Check if window is currently minimized (only PLATFORM_DESKTOP)"
  "IsWindowMinimized"
  [] ::ri/bool)

(defcfn is-window-maximized?
  "Check if window is currently maximized (only PLATFORM_DESKTOP)"
  "IsWindowMaximized"
  [] ::ri/bool)

(defcfn is-window-focused?
  "Check if window is currently focused (only PLATFORM_DESKTOP)"
  "IsWindowFocused"
  [] ::ri/bool)

(defcfn is-window-resized?
  "Check if window has been resized last frame"
  "IsWindowResized"
  [] ::ri/bool)

; ...

(defcfn get-screen-width
  "Get current screen width"
  "GetScreenWidth"
  [] ::mem/int)

(defcfn get-screen-height
  "Get current screen height"
  "GetScreenHeight"
  [] ::mem/int)

(defcfn get-render-width
  "Get current render width (considers HiDPI)"
  "GetRenderWidth"
  [] ::mem/int)

(defcfn get-render-height
  "Get current render height (considers HiDPI)"
  "GetRenderHeight"
  [] ::mem/int)

(defcfn set-config-flags!
  "Setup init configuration flags (view FLAGS)"
  {:arglists '([flags])}
  "SetConfigFlags"
  [::mem/int] ::mem/void)

(def config-flag
  {:flag/vsync-hint 0x00000040
   :flag/fullscreen-mode 0x00000002
   :flag/window-resizable 0x00000004
   :flag/window-undecorated 0x00000008
   :flag/window-hidden 0x00000080
   :flag/window-minimized 0x00000200
   :flag/window-maximized 0x00000400
   :flag/window-unfocused 0x00000800
   :flag/window-topmost 0x00001000
   :flag/window-always-run 0x00000100
   :flag/window-transparent 0x00000010
   :flag/window-highdpi 0x00002000
   :flag/window-mouse-passthrough 0x00004000
   :flag/borderless-windowed-mode 0x00008000
   :flag/msaa-4x-hint 0x00000020
   :flag/interlaced-hint 0x00010000})

(defn set-config-flags [& flags]
  (if (= 1 (count flags))
    (set-config-flags! (->> flags first (get config-flag)))
    (set-config-flags! (apply bit-or (map config-flag flags)))))

(defcfn toggle-fullscreen!
  "Toggle window state: fullscreen/windowed (only PLATFORM_DESKTOP)"
  "ToggleFullscreen"
  [] ::mem/void)

(defcfn toggle-borderless-windowed!
  "Toggle window state: borderless windowed (only PLATFORM_DESKTOP)"
  "ToggleBorderlessWindowed"
  [] ::mem/void)

(defcfn set-window-size!
  "Set window dimensions"
  {:arglists '([width height])}
  "SetWindowSize"
  [::mem/int ::mem/int] ::mem/void)

(defcfn get-current-monitor
  "Get current connected monitor"
  "GetCurrentMonitor"
  [] ::mem/int)

(defcfn get-monitor-width
  "Get specified monitor width (current video mode used by monitor)"
  {:arglists '([monitor])}
  "GetMonitorWidth"
  [::mem/int] ::mem/int)

(defcfn get-monitor-height
  "Get specified monitor height (current video mode used by monitor)"
  {:arglists '([monitor])}
  "GetMonitorHeight"
  [::mem/int] ::mem/int)

; ...
