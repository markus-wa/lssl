(ns glsl
  (:require [clojure.java.io :as io]
            [opengl]))

(def vertex-shader-source (-> "shaders/test.vert.glsl"
                              io/resource
                              slurp))

(def fragment-shader-source (-> "shaders/test.frag.glsl"
                                io/resource
                                slurp))

(comment
  (opengl/go vertex-shader-source fragment-shader-source)
 )
