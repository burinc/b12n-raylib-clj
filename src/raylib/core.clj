(ns raylib.core
  (:require
   [coffi.ffi :as ffi]
   [clojure.string :as str])
  (:import [java.io File]))

;; OS Detection
(defn- get-os-name []
  (let [os (System/getProperty "os.name")]
    (cond
      (.contains os "Mac") :macos
      (.contains os "Linux") :linux
      (.contains os "Windows") :windows
      :else :unknown)))

;; Architecture Detection
(defn- get-arch []
  (let [arch (System/getProperty "os.arch")]
    (cond
      (contains? #{"amd64" "x86_64"} arch) :amd64
      (contains? #{"x86" "i386" "i686"} arch) :i386
      (= arch "aarch64") :aarch64
      :else :unknown)))

;; Get platform subdirectory name for libs/
(defn- get-platform-dir []
  (let [os (get-os-name)
        arch (get-arch)]
    (case os
      :macos "macos"
      :linux (case arch
               :amd64 "linux_amd64"
               :i386 "linux_i386"
               "linux_amd64")  ; default to amd64
      :windows (case arch
                 :amd64 "win64_msvc16"
                 :i386 "win32_msvc16"
                 "win64_msvc16")  ; default to win64
      nil)))

;; Get library filename for current OS
(defn- get-lib-name []
  (case (get-os-name)
    :macos "libraylib.6.0.0.dylib"
    :linux "libraylib.so.6.0.0"
    :windows "raylib.dll"
    nil))

;; Find bundled library path - check multiple locations
(defn- find-bundled-lib []
  (let [lib-name (get-lib-name)
        platform-dir (get-platform-dir)
        cwd (System/getProperty "user.dir")
        lib-path (System/getProperty "java.library.path")
        ;; Candidate directories to search
        search-dirs (concat
                     ;; java.library.path directories (for packaged apps)
                     (when lib-path (str/split lib-path (re-pattern File/pathSeparator)))
                     ;; Development path with platform subdirectory
                     [(str cwd "/libs/" platform-dir)])]
    (when lib-name
      (->> search-dirs
           (map #(str % "/" lib-name))
           (filter #(.exists (File. ^String %)))
           first))))

;; Load raylib - prefer bundled, fallback to system
(let [bundled-path (find-bundled-lib)]
  (if bundled-path
    (do
      (println "[raylib] Loading bundled library:" bundled-path)
      (ffi/load-library bundled-path))
    (do
      (println "[raylib] Loading system library")
      (ffi/load-system-library "raylib"))))
