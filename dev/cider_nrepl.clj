(ns cider-nrepl)

; REPL for MAC-OS due to weird behaviour with CIDER and OpenGL

(defn -main
  []
  (.start (Thread. (fn []
                     (println "Starting Cider Nrepl Server Port 7888")
                     (load-string (str "(require '[nrepl.server :as nrepl-server])"
                                       "(require '[cider.nrepl :as cider])"
                                       "(nrepl-server/start-server :port 7888 :handler cider/cider-nrepl-handler)"))))))
