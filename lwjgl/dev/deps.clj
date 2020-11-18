(ns deps
  (:require [clojure.pprint :as ppt]))

(def LWJGL_NS "org.lwjgl")

;; Edit this to change the version.
(def LWJGL_VERSION "3.2.3")

;; Edit this to add/remove packages.
(def LWJGL_MODULES ["lwjgl"
                    "lwjgl-assimp"
                    "lwjgl-bgfx"
                    "lwjgl-egl"
                    "lwjgl-glfw"
                    "lwjgl-jawt"
                    "lwjgl-jemalloc"
                    "lwjgl-lmdb"
                    "lwjgl-lz4"
                    "lwjgl-nanovg"
                    "lwjgl-nfd"
                    "lwjgl-nuklear"
                    "lwjgl-odbc"
                    "lwjgl-openal"
                    "lwjgl-opencl"
                    "lwjgl-opengl"
                    "lwjgl-opengles"
                    "lwjgl-openvr"
                    "lwjgl-par"
                    "lwjgl-remotery"
                    "lwjgl-rpmalloc"
                    "lwjgl-sse"
                    "lwjgl-stb"
                    "lwjgl-tinyexr"
                    "lwjgl-tinyfd"
                    "lwjgl-tootle"
                    "lwjgl-vulkan"
                    "lwjgl-xxhash"
                    "lwjgl-yoga"
                    "lwjgl-zstd"])

;; It's safe to just include all native dependencies, but you might
;; save some space if you know you don't need some platform.
(def LWJGL_PLATFORMS ["linux" "macos" "windows"])

;; These packages don't have any associated native ones.
(def no-natives? #{"lwjgl-egl"
                   "lwjgl-jawt"
                   "lwjgl-odbc"
                   "lwjgl-opencl"
                   "lwjgl-vulkan"})

(defn module->deps
  [m]
  (let [prefix [(symbol LWJGL_NS m) {:mvn/version LWJGL_VERSION}]]
    (into [prefix]
          (if (no-natives? m)
            []
            (for [p LWJGL_PLATFORMS]
              (into prefix [(symbol LWJGL_NS (str m "$natives-" p)) {:mvn/version LWJGL_VERSION}]))))))

(defn lwjgl-deps-with-natives []
  (reduce #(apply assoc %1 (apply concat (module->deps %2))) {} LWJGL_MODULES))

(def all-dependencies
  (merge
   '{org.clojure/clojure {:mvn/version "1.10.0"}
     org.clojure/tools.analyzer.jvm {:mvn/version "1.0.0"}}
   (lwjgl-deps-with-natives)))

(defn -main
  [& args]
  (spit "lwjgl/deps.edn"
        (with-out-str (ppt/pprint {:deps all-dependencies}))))
