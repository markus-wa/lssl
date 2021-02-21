(ns glsl
  (:require [clojure.java.io :as io]
            [opengl]))

(def vertex-shader-source (-> "shaders/glsl/test.vert.glsl"
                              io/resource
                              slurp))

(def fragment-shader-source (-> "shaders/glsl/test.frag.glsl"
                                io/resource
                                slurp))

(comment
  (opengl/go (constantly vertex-shader-source) (constantly fragment-shader-source))
 )
