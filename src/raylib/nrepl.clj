(ns raylib.nrepl
  (:require
   [clojure.tools.logging :as log]
   [nrepl.server :as nrepl]))

(defn start
  "Start a network repl for debugging on specified port followed by
  an optional parameters map. The :bind, :transport-fn, :handler,
  :ack-port and :greeting-fn will be forwarded to
  nrepl.server/start-server as they are.
  
  If the port is already in use, logs a warning and returns nil
  instead of throwing - this allows games to still run when another
  nREPL server is already using the port."
  [{:keys [port bind transport-fn handler ack-port greeting-fn]}]
  (try
    (log/info "starting nREPL server on port" port)
    (nrepl/start-server :port port
                        :bind bind
                        :transport-fn transport-fn
                        :handler handler
                        :ack-port ack-port
                        :greeting-fn greeting-fn)

    (catch java.net.BindException e
      (log/warn (str "nREPL port " port " already in use - continuing without embedded nREPL. "
                     "You can connect to the existing nREPL server if one is running."))
      nil)
    (catch Throwable t
      (log/error t "failed to start nREPL")
      (throw t))))

(defn stop
  [server]
  (when server
    (nrepl/stop-server server)
    (log/info "nREPL server stopped")))
