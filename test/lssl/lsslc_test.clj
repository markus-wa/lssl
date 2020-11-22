(ns lssl.lssc-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [lssl.lsslc :as c]))

(defn read-resource
  [path]
  (-> path
      io/resource
      slurp))

(deftest lsslc
  (c/-main "-o" "/tmp/frag.spv.asm" "dev/resources/shaders/lssl.frag.lssl")
  (is (= (read-resource "golden/frag.spv.asm") (slurp "/tmp/frag.spv.asm"))))
