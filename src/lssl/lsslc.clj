(ns lssl.lsslc
  (:require [lssl.analyzer :as ana]
            [lssl.compiler :as c]
            [lssl.emitter :as em]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]))

(defn lsslc
  [source]
  (-> source
      ana/analyze
      c/compile-ast
      em/emit))

(defn- read-stdin
  []
  (throw (RuntimeException. "read from stdin not implemented")))

(defn- get-source
  [input]
  (cond
    (contains? #{"-" "" nil} input)
    (slurp *in*)
    (.exists (io/file input))
    (slurp input)
    :else
    input))

(defn- write-file
  [path data]
  (with-open [w (io/writer path)]
    (.write w data)))

(defn- output-writer
  [{:keys [output]}]
  (if output
    #(write-file output %)
    #(.write *out* %)))

(defn- write-output
  [target output]
  (.write target output))

(def cli-options
  [["-h" "--help"]
   ["-o" "--output PATH" "Target output file for SPIR-V assembly"]])

(defn- usage [options-summary]
  (->> ["Lisp Shading Language Compiler"
        ""
        "Usage: lsslc [options] path-to-source"
        ""
        "Options:"
        options-summary
        ""
        "For more info, see:"
        " https://github.com/markus-wa/lssl"]
       (str/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn- validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]}
        (cli/parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (= 1 (count arguments))
      {:input (first arguments)
       :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [input options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (-> input
          get-source
          (#(str "[\n" % "\n]\n")) ; turn the flat file into a vec
          read-string
          lsslc
          ((output-writer options))))))

(comment
  (do
    (require '[clojure.java.shell :as sh])

    (def lssl-src
      '[(defversion 460 core)

        (defout FragColor vec4
          {:layout {:location 0}})

        (defuniform inputs Inputs
          {:layout {:memory :std140
                    :binding 0}}
          (color vec4))

        (defun void main []
          (reset! FragColor (get inputs color)))])

    (let [out-file "dev/resources/shaders/lssl.frag.spv.asm"]
      (spit out-file
            (lsslc lssl-src))

      (sh/sh "spirv-as" out-file "-o" "dev/resources/shaders/lssl.frag.spv")))

  (-main "test/resources/shaders/test.frag.lssl")

)
