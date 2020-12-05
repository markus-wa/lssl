(ns spirv
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [lssl.lsslc :as c]
            [opengl]
            [util :refer [with-watcher]])
  (:import (java.io ByteArrayOutputStream)
           (java.nio ByteBuffer)
           (org.lwjgl BufferUtils)))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [in  (io/input-stream x)
              out (ByteArrayOutputStream.)]
    (io/copy in out)
    (.toByteArray out)))

(defn ->ByteBuffer
  [bytes]
  (-> (BufferUtils/createByteBuffer (count bytes))
      (.put bytes)
      (.flip)))

(defn load-bin
  [path]
  (-> path
      io/resource
      slurp-bytes
      ->ByteBuffer))

(defn compile&assemble
  []
  (println "recompiling ...")
  (let [asm-file "target/spv/example.frag.spv.asm"
        spv-file "target/resources/shaders/lssl/example.frag.spv"]
    (io/make-parents asm-file)
    (io/make-parents spv-file)
    (c/-main "dev/resources/shaders/lssl/example.frag.lssl" "-o" asm-file)
    (sh/sh "spirv-as" asm-file "-o" spv-file))
  (println "done"))

(defn go
  []
  (compile&assemble)
  (with-watcher (fn [_] (compile&assemble)) (io/file "dev/resources/shaders/lssl")
    (opengl/go #(load-bin "shaders/glsl/test.vert.spv") #(load-bin "shaders/lssl/example.frag.spv"))))

(comment
  (.start (Thread. go)))
