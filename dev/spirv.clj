(ns spirv
  (:require [clojure.java.io :as io])
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

(def fragment-shader-bin (-> "shaders/test.frag.spv"
                             io/resource
                             slurp-bytes
                             ->ByteBuffer))

(def vertex-shader-bin (-> "shaders/test.vert.spv"
                           io/resource
                           slurp-bytes
                           ->ByteBuffer))

(comment
  (opengl/go vertex-shader-bin fragment-shader-bin)
  )
