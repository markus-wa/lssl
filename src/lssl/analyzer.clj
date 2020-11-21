(ns lssl.analyzer
  (:require [clojure.tools.analyzer :as ana]
            [clojure.tools.analyzer.env :as env]
            [clojure.tools.analyzer.jvm :as ana.jvm]))

(defn analyze
  [s-expression]
  (ana.jvm/analyze s-expression
                   (ana.jvm/empty-env)
                   {:passes-opts {:validate/unresolvable-symbol-handler
                                  (fn [a b c] nil)}}))

(comment
  (defn analyze [form env]
    (binding [ana/macroexpand-1 macroexpand-1
              ana/create-var    create-var
              ana/parse         parse
              ana/var?          var?]
      (env/ensure (global-env)
                  (run-passes (-analyze form env))))))
