(ns raylib.audio
  "Audio device and music stream functions"
  (:require
   [raylib.core]
   [raylib.structs :as rs]
   [coffi.mem :as mem :refer [defalias]]
   [coffi.ffi :refer [defcfn]]))

;; AudioStream struct (32 bytes on 64-bit)
;; { rAudioBuffer *buffer; rAudioProcessor *processor; uint sampleRate, sampleSize, channels; }
(defalias ::audio-stream
  [::mem/struct
   [[:buffer-lo ::mem/int] ; pointer low bits
    [:buffer-hi ::mem/int] ; pointer high bits  
    [:processor-lo ::mem/int] ; pointer low bits
    [:processor-hi ::mem/int] ; pointer high bits
    [:sample-rate ::mem/int]
    [:sample-size ::mem/int]
    [:channels ::mem/int]
    [:_pad ::mem/int]]]) ; padding to 32 bytes

;; Music struct (56 bytes on 64-bit)
;; { AudioStream stream; uint frameCount; bool looping; int ctxType; void *ctxData; }
(defalias ::music
  [::mem/struct
   [[:stream ::audio-stream] ; 32 bytes
    [:frame-count ::mem/int] ; 4 bytes
    [:looping ::mem/int] ; bool as int (4 bytes with padding)
    [:ctx-type ::mem/int] ; 4 bytes
    [:_pad2 ::mem/int] ; padding for pointer alignment
    [:ctx-data-lo ::mem/int] ; pointer low bits
    [:ctx-data-hi ::mem/int]]]) ; pointer high bits

;; Audio device management
(defcfn init-audio-device!
  "Initialize audio device and context"
  {:arglists '([])}
  "InitAudioDevice"
  [] ::mem/void)

(defcfn close-audio-device!
  "Close the audio device and context"
  {:arglists '([])}
  "CloseAudioDevice"
  [] ::mem/void)

(defcfn is-audio-device-ready?
  "Check if audio device has been initialized successfully"
  {:arglists '([])}
  "IsAudioDeviceReady"
  [] ::mem/int)

(defcfn set-master-volume!
  "Set master volume (listener)"
  {:arglists '([volume])}
  "SetMasterVolume"
  [::mem/float] ::mem/void)

;; Music stream functions
(defcfn load-music-stream
  "Load music stream from file"
  {:arglists '([filename])}
  "LoadMusicStream"
  [::mem/c-string] ::music)

(defcfn unload-music-stream!
  "Unload music stream"
  {:arglists '([music])}
  "UnloadMusicStream"
  [::music] ::mem/void)

(defcfn play-music-stream!
  "Start music playing"
  {:arglists '([music])}
  "PlayMusicStream"
  [::music] ::mem/void)

(defcfn is-music-stream-playing?
  "Check if music is playing"
  {:arglists '([music])}
  "IsMusicStreamPlaying"
  [::music] ::mem/int)

(defcfn update-music-stream!
  "Updates buffers for music streaming"
  {:arglists '([music])}
  "UpdateMusicStream"
  [::music] ::mem/void)

(defcfn stop-music-stream!
  "Stop music playing"
  {:arglists '([music])}
  "StopMusicStream"
  [::music] ::mem/void)

(defcfn pause-music-stream!
  "Pause music playing"
  {:arglists '([music])}
  "PauseMusicStream"
  [::music] ::mem/void)

(defcfn resume-music-stream!
  "Resume playing paused music"
  {:arglists '([music])}
  "ResumeMusicStream"
  [::music] ::mem/void)

(defcfn set-music-volume!
  "Set volume for music (1.0 is max level)"
  {:arglists '([music volume])}
  "SetMusicVolume"
  [::music ::mem/float] ::mem/void)

(defcfn set-music-pitch!
  "Set pitch for a music (1.0 is base level)"
  {:arglists '([music pitch])}
  "SetMusicPitch"
  [::music ::mem/float] ::mem/void)

(defcfn get-music-time-length
  "Get music time length (in seconds)"
  {:arglists '([music])}
  "GetMusicTimeLength"
  [::music] ::mem/float)

(defcfn get-music-time-played
  "Get current music time played (in seconds)"
  {:arglists '([music])}
  "GetMusicTimePlayed"
  [::music] ::mem/float)

(defcfn set-music-pan!
  "Set pan for a music (-1.0 left, 0.0 center, 1.0 right)"
  {:arglists '([music pan])}
  "SetMusicPan"
  [::music ::mem/float] ::mem/void)

;; Sound struct (40 bytes on 64-bit)
;; { AudioStream stream; uint frameCount; }
(defalias ::sound
  [::mem/struct
   [[:stream ::audio-stream] ; 32 bytes
    [:frame-count ::mem/int] ; 4 bytes
    [:_pad ::mem/int]]]) ; padding to 40 bytes

;; Sound loading/unloading functions
(defcfn load-sound
  "Load sound from file"
  {:arglists '([filename])}
  "LoadSound"
  [::mem/c-string] ::sound)

(defcfn load-sound-alias
  "Create a new sound that shares the same sample data as the source sound"
  {:arglists '([source])}
  "LoadSoundAlias"
  [::sound] ::sound)

(defcfn unload-sound!
  "Unload sound"
  {:arglists '([sound])}
  "UnloadSound"
  [::sound] ::mem/void)

(defcfn unload-sound-alias!
  "Unload a sound alias (does not deallocate sample data)"
  {:arglists '([alias])}
  "UnloadSoundAlias"
  [::sound] ::mem/void)

;; Sound control functions
(defcfn play-sound!
  "Play a sound"
  {:arglists '([sound])}
  "PlaySound"
  [::sound] ::mem/void)

(defcfn stop-sound!
  "Stop playing a sound"
  {:arglists '([sound])}
  "StopSound"
  [::sound] ::mem/void)

(defcfn pause-sound!
  "Pause a sound"
  {:arglists '([sound])}
  "PauseSound"
  [::sound] ::mem/void)

(defcfn resume-sound!
  "Resume a paused sound"
  {:arglists '([sound])}
  "ResumeSound"
  [::sound] ::mem/void)

(defcfn is-sound-playing?
  "Check if a sound is currently playing"
  {:arglists '([sound])}
  "IsSoundPlaying"
  [::sound] ::mem/int)

(defcfn set-sound-volume!
  "Set volume for a sound (1.0 is max level)"
  {:arglists '([sound volume])}
  "SetSoundVolume"
  [::sound ::mem/float] ::mem/void)

(defcfn set-sound-pitch!
  "Set pitch for a sound (1.0 is base level)"
  {:arglists '([sound pitch])}
  "SetSoundPitch"
  [::sound ::mem/float] ::mem/void)

(defcfn set-sound-pan!
  "Set pan for a sound (-1.0 left, 0.0 center, 1.0 right)"
  {:arglists '([sound pan])}
  "SetSoundPan"
  [::sound ::mem/float] ::mem/void)
