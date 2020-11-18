(ns opengl
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (java.nio ByteBuffer FloatBuffer IntBuffer)
           (org.lwjgl BufferUtils)
           (org.lwjgl.glfw GLFW GLFWErrorCallback)
           (org.lwjgl.opengl ARBGLSPIRV GL GL11 GL33 GL41 GL46)
           (org.lwjgl.system MemoryUtil)))

(def NULL MemoryUtil/NULL)

(defn ok?
  [f status log-fn x]
  (when-not (pos? (f x status))
    ; TODO: use log-fn and be more explicit
    (RuntimeException. "Failed to do thing")))

(defmulti ->shader (fn [_ shader] (class shader)))

(defmethod ->shader
  java.lang.String
  [type source]
  (let [shader (GL33/glCreateShader type)]
    (GL33/glShaderSource shader source)
    (GL33/glCompileShader shader)
    (ok? #(GL33/glGetShaderi %1 %2) GL33/GL_COMPILE_STATUS #(GL33/glGetShaderInfoLog %) shader)
    shader))

(defmethod ->shader
  ByteBuffer
  [type binary]
  (let [shader (GL33/glCreateShader type)
        shaders (-> (BufferUtils/createIntBuffer 1)
                    (.put (int-array [shader]))
                    (.flip))]
    (GL41/glShaderBinary shaders ARBGLSPIRV/GL_SHADER_BINARY_FORMAT_SPIR_V_ARB binary)
    (let [^IntBuffer nil-buf nil]
      (GL46/glSpecializeShader shader "main" (BufferUtils/createIntBuffer 0) (BufferUtils/createIntBuffer 0)))
    (ok? #(GL33/glGetShaderi %1 %2) GL33/GL_COMPILE_STATUS #(GL33/glGetShaderInfoLog %) shader)
    shader))

(defn ->vertex-shader
  [source]
  (->shader GL33/GL_VERTEX_SHADER source))

(defn ->fragment-shader
  [source]
  (->shader GL33/GL_FRAGMENT_SHADER source))

(defn link-program
  [vs fs]
  (let [program (GL33/glCreateProgram)]
    (GL33/glAttachShader program vs)
    (GL33/glAttachShader program fs)
    (GL33/glLinkProgram program)
    (ok? #(GL33/glGetProgrami %1 %2) GL33/GL_LINK_STATUS #(GL33/glGetProgramInfoLog %) program)
    program))

(defn shader-program
  [vs-source fs-source]
  (let [vs (->vertex-shader vs-source)
        fs (->fragment-shader fs-source)
        program (link-program vs fs)]
    (GL33/glDeleteShader vs)
    (GL33/glDeleteShader fs)

    program))

(defmacro with-glfw
  [& body]
  `(let [error-callback# (GLFWErrorCallback/createPrint System/err)]
     (GLFW/glfwSetErrorCallback error-callback#)

     (when-not (GLFW/glfwInit)
       (throw (IllegalStateException. "Unable to initialise GLFW")))
    (try
      ~@body
      (finally
        (GLFW/glfwTerminate)
        (.free error-callback#)))))

(defmacro with-window
  [width heigh title & body]
  `(let [window# (GLFW/glfwCreateWindow width heigh title NULL NULL)]
     (try
       ~@body
       (finally
         (GLFW/glfwDestroyWindow window)))))

(defmacro defer
  [f & body]
  `(try
     ~@body
     (finally
       (~f))))

(defn setup-vao
  []
  (let [vertices [0.5 -0.5 0.0
                  -0.5 -0.5 0.0
                  0.0 0.5 0.0]
        vertex-buf (-> (BufferUtils/createFloatBuffer (count vertices))
                       (.put (float-array vertices))
                       (.flip))
        vao (GL33/glGenVertexArrays)
        _ (GL33/glBindVertexArray vao)
        vbo (GL33/glGenBuffers)]
    (GL33/glBindBuffer GL33/GL_ARRAY_BUFFER vbo)
    (GL33/glBufferData GL33/GL_ARRAY_BUFFER ^FloatBuffer vertex-buf GL33/GL_STATIC_DRAW)

    (GL33/glVertexAttribPointer 0 3 GL33/GL_FLOAT false 0 0)
    (GL33/glEnableVertexAttribArray 0)
    vao))

(defn setup-data-buffer
  []
  (let [data [0.5 1.0 0.0]
        data-buf (-> (BufferUtils/createFloatBuffer (count data))
                       (.put (float-array data))
                       (.flip))
        bo (GL33/glGenBuffers)]
    (GL33/glBindBuffer GL33/GL_UNIFORM_BUFFER bo)
    (GL33/glBufferData GL33/GL_UNIFORM_BUFFER ^FloatBuffer data-buf GL33/GL_DYNAMIC_DRAW)
    (GL33/glBindBufferBase GL33/GL_UNIFORM_BUFFER 0 bo)
    bo))


(defn go
  [vs fs]
  (with-glfw
    (GLFW/glfwDefaultWindowHints)
    (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MAJOR 4)
    (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MINOR 6)
    (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_PROFILE GLFW/GLFW_OPENGL_CORE_PROFILE)
    (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_FORWARD_COMPAT GLFW/GLFW_TRUE)

    (let [window (GLFW/glfwCreateWindow 640 480 "Example" NULL NULL)]
      (when (= window NULL)
        (throw (RuntimeException. "Failed to create the GLFW window")))

      (defer #(GLFW/glfwDestroyWindow window)
        (GLFW/glfwMakeContextCurrent window)
        (GLFW/glfwSwapInterval 1)

        (GL/createCapabilities)
        (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))

        (let [shader-program (shader-program vs fs)
              vao (setup-vao)
              db (setup-data-buffer)]
          (while (not (GLFW/glfwWindowShouldClose window))
            (GL33/glClearColor 0.2 0.3 0.3 1.0)
            (GL33/glClear GL33/GL_COLOR_BUFFER_BIT)

            (GL33/glUseProgram shader-program)

            (let [time (GLFW/glfwGetTime)
                  green-value (+ (/ (Math/sin time) 2.0) 0.5)
                  data [0.0 green-value 0.0]
                  data-buf (-> (BufferUtils/createFloatBuffer (count data)) ; TODO: inefficient
                               (.put (float-array data))
                               (.flip))]
              (GL33/glBindBuffer GL33/GL_UNIFORM_BUFFER db)
              (GL33/glBufferSubData GL33/GL_UNIFORM_BUFFER 0 ^FloatBuffer data-buf))

            (GL33/glDrawArrays GL33/GL_TRIANGLES 0 3)

            (GLFW/glfwSwapBuffers window)
            (GLFW/glfwPollEvents)))))))
