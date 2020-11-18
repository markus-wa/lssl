(ns lssl.analyser
  (:require [clojure.tools.analyzer :as ana]
            [clojure.tools.analyzer.env :as env]))

(defn analyze [form env]
  (binding [ana/macroexpand-1 macroexpand-1
            ana/create-var    create-var
            ana/parse         parse
            ana/var?          var?]
    (env/ensure (global-env)
                (run-passes (-analyze form env)))))
