(ns lssl.emitter
  (:require [clojure.string :as str]))

(defn- emit-comment
  [text]
  (str "; " text))

(defn- emit-header
  [header]
  (concat
   [(emit-comment "SPIR-V")]
   (map (fn [[k v]] (emit-comment (str k ": " v))) header)))

(defn- label-prefix
  [label]
  (if label
    (str "%" (name label) " = ")))

(defmulti ^:private literal class)

(defmethod literal
  java.lang.String
  [v]
  (str "\"" v "\""))

(defmethod literal
  java.lang.Number
  [v]
  (str v))

(defmethod literal
  clojure.lang.Keyword
  [v]
  (str "%" (name v)))

(defmethod literal
  clojure.lang.Symbol
  [v]
  (name v))

(defn- opcode
  [{:lssl.opcodes/keys [opcode args label] :as _opc}]
  (str (label-prefix label)
       (name opcode)
       (when args
         (str " " (str/join " " (map literal args))))))

(defn emit
  "FIXME: fix this garbage"
  [{:keys [header
           capabilities
           extensions
           ext-inst-imports
           memory-model
           entry-points
           execution-modes
           annotations
           types
           fn-definitions]
    {:keys [strings+sources
            names]} :debug-info :as _module}]
  (str/join "\n"
            (concat
             (emit-header header)
             (map opcode capabilities)
             (map opcode extensions)
             (map opcode ext-inst-imports)
             [(opcode memory-model)]
             (map opcode entry-points)
             (map opcode execution-modes)
             (map opcode strings+sources)
             (map opcode names)
             (map opcode annotations)
             (map opcode types)
             (mapcat #(map opcode %) fn-definitions)
             [""])))

(comment
  (do
    (require '[lssl.opcodes :as op])
    (spit "/tmp/emitter.spv.asm"
          (emit
           {:header {"Version" 1.0
                     "Generator" "lsslc; v0.1.0-SNAPSHOT"
                     "Bound" 18
                     "Schema" 0}
            :capabilities [(op/capability 'Shader)]
            :extensions []
            :ext-inst-imports [(op/add-label (op/ext-inst-imports "GLSL.std.450") :1)]
            :memory-model (op/memory-model 'Logical 'GLSL450)
            :entry-points [(op/entry-point 'Fragment :main "main" :FragColor)]
            :execution-modes [(op/execution-mode :main 'OriginUpperLeft)]
            :debug-info {:strings+sources
                         [(op/source 'GLSL 460)]
                         :names
                         [(op/name- :main "main")
                          (op/name- :FragColor "FragColor")
                          (op/name- :Inputs "Inputs")
                          (op/member-name :Inputs 0 "color")
                          (op/name- :inputs "inputs")]}
            :annotations [(op/decorate :FragColor 'Location 0)
                          (op/member-decorate :Inputs 0 'Offset 0)
                          (op/decorate :Inputs 'Block)
                          (op/decorate :inputs 'DescriptorSet 0)
                          (op/decorate :inputs 'Binding 0)]
            :types [(op/add-label (op/type-void) :void)
                    (op/add-label (op/type-function :void) :3)
                    (op/add-label (op/type-float 32) :float)
                    (op/add-label (op/type-vector :float 4) :v4float)
                    (op/add-label (op/type-pointer 'Output :v4float) :_ptr_Output_v4float)
                    (op/add-label (op/variable :_ptr_Output_v4float 'Output) :FragColor)
                    (op/add-label (op/type-struct :v4float) :Inputs)
                    (op/add-label (op/type-pointer 'Uniform :Inputs) :_ptr_Uniform_Inputs)
                    (op/add-label (op/variable :_ptr_Uniform_Inputs 'Uniform) :inputs)
                    (op/add-label (op/type-int 32 1) :int)
                    (op/add-label (op/constant :int 0) :int_0)
                    (op/add-label (op/type-pointer 'Uniform :v4float) :_ptr_Uniform_v4float)]
            :fn-declarations nil
            :fn-definitions [(op/function :void :main 'None :3
                                          (op/label :5)
                                          (op/add-label (op/access-chain :_ptr_Uniform_v4float :inputs :int_0) :16)
                                          (op/add-label (op/load :v4float :16) :17)
                                          (op/store :FragColor :17)
                                          (op/return))]})))

)
