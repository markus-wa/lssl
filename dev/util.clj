(ns util
  (:require [juxt.dirwatch :as watch]))

(defmacro with-watcher
  [f file & body]
  `(let [watcher# (watch/watch-dir ~f ~file)]
     (try
       ~@body
       (finally
         (watch/close-watcher watcher#)))))
