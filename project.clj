(defproject b12n-raylib-clj "0.1.0-SNAPSHOT"
  :description "raylib game-development examples in Clojure, over coffi/Panama FFI"
  :url "https://github.com/burinc/b12n-raylib-clj"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"
            :comment "Inherited from ertugrulcetin/raylib-clojure-playground; see NOTICE"}

  :dependencies [[org.clojure/clojure "1.12.0"]
                 [org.clojure/tools.logging "1.3.0"]
                 ;; Override coffi to use JDK 22+ compatible version
                 [org.suskalo/coffi "1.0.615"]
                 ;; insn is a dependency of coffi
                 [insn/insn "0.5.4"]]

  :source-paths ["src"]
  :resource-paths ["resources"]

  ;; JVM options for native access
  ;; The bundled raylib library is loaded from libs/
  :jvm-opts ["--enable-native-access=ALL-UNNAMED"
             "-XstartOnFirstThread"  ; Required for macOS GUI/OpenGL
             "-Djava.library.path=libs:/opt/homebrew/opt/raylib/lib:/opt/homebrew/lib:/usr/local/lib:/usr/lib"]

  :profiles {:dev {:dependencies [[nrepl "1.3.0"]]}}

  :main examples.asteroids
  :aot [examples.asteroids]

  :repl-options {:init-ns examples.asteroids})
