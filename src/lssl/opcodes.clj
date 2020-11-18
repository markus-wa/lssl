(ns lssl.opcodes
  (:require [clojure.string :as str]))

(def ast {})

(defn e-comment
  [text]
  (str "; " text))

(defn e-header
  [header]
  (concat
   [(e-comment "SPIR-V")]
   (map (fn [[k v]] (e-comment (str k ": " v))) header)))

(defn e-label
  [label]
  (if label
    (str "%" (name label) " = ")))

(defmulti e-literal class)

(defmethod e-literal
  java.lang.String
  [v]
  (str "\"" v "\""))

(defmethod e-literal
  java.lang.Number
  [v]
  (str v))

(defmethod e-literal
  clojure.lang.Keyword
  [v]
  (str "%" (name v)))

(defmethod e-literal
  clojure.lang.Symbol
  [v]
  (name v))

(defn e-opcode
  [{::keys [opcode args label] :as _opc}]
  (str (e-label label)
       (name opcode)
       (when args
         (str " " (str/join " " (map e-literal args))))))

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
             (e-header header)
             (map e-opcode capabilities)
             (map e-opcode extensions)
             (map e-opcode ext-inst-imports)
             [(e-opcode memory-model)]
             (map e-opcode entry-points)
             (map e-opcode execution-modes)
             (map e-opcode strings+sources)
             (map e-opcode names)
             (map e-opcode annotations)
             (map e-opcode types)
             (mapcat #(map e-opcode %) fn-definitions)
             [""])))

(defn opcode
  [opcode & args]
  {::opcode opcode ::args args})

(defn capability
  [name]
  (opcode 'OpCapability name))

(defn ext-inst-imports
  [import]
  (opcode 'OpExtInstImport import))

(defn add-label
  [m label]
  (assoc m ::label label))

(defn memory-model
  [model version]
  (opcode 'OpMemoryModel model version))

(defn entry-point
  [exec-model label name & interfaces]
  (apply (partial opcode 'OpEntryPoint exec-model label name) interfaces))

(defn execution-mode
  [label mode]
  (opcode 'OpExecutionMode label mode))

(defn source
  [type version]
  (opcode 'OpSource type version))

(defn name
  [label name]
  (opcode 'OpName label name))

(defn member-name
  [label offset member-name]
  (opcode 'OpMemberName label offset member-name))

(defn decorate
  [label decoration & literals]
  (apply (partial opcode 'OpDecorate label decoration) literals))

(defn member-decorate
  [label literal-num decoration & literals]
  (apply (partial opcode 'OpMemberDecorate label literal-num decoration) literals))

(defn type-void
  []
  (opcode 'OpTypeVoid))

(defn type-fn
  [return-type & params]
  (apply (partial opcode 'OpTypeFunction return-type) params))

(defn type-float
  [width]
  (opcode 'OpTypeFloat width))

(defn type-vec
  [component-label size]
  (opcode 'OpTypeVector component-label size))

(defn variable
  [type-label storage-class]
  (opcode 'OpVariable type-label storage-class))

(defn type-struct
  [& member-type-labels]
  (apply (partial opcode 'OpTypeStruct) member-type-labels))

(defn type-pointer
  [storage-class type]
  (opcode 'OpTypePointer storage-class type))

(defn type-int
  [width signedness]
  (opcode 'OpTypeInt width signedness))

(defn constant
  [type-label & values]
  (apply (partial opcode 'OpConstant type-label) values))

(defn function
  [result-type name function-control fn-type & body]
  (concat [(add-label (opcode 'OpFunction result-type function-control fn-type) name)]
          body
          [(opcode 'OpFunctionEnd)]))

(defn label
  [label]
  (add-label (opcode 'OpLabel) label))

(defn access-chain
  [result-type base & indices]
  (apply (partial opcode 'OpAccessChain result-type base) indices))

(defn load
  ([result-type pointer]
   (opcode 'OpLoad result-type pointer))
  ([result-type pointer memory-access]
   (opcode 'OpLoad result-type pointer memory-access)))

(defn store
  ([pointer object]
   (opcode 'OpStore pointer object))
  ([pointer object memory-access]
   (opcode 'OpStore pointer object memory-access)))

(defn return
  []
  (opcode 'OpReturn))

(spit "out.spv.dis"
      (emit
       {:header {"Version" 1.0
                 "Generator" "lsslc; v0.1.0-SNAPSHOT"
                 "Bound" 18
                 "Schema" 0}
        :capabilities [(capability 'Shader)]
        :extensions []
        :ext-inst-imports [(add-label (ext-inst-imports "GLSL.std.450") :1)]
        :memory-model (memory-model 'Logical 'GLSL450)
        :entry-points [(entry-point 'Fragment :main "main" :FragColor)]
        :execution-modes [(execution-mode :main 'OriginUpperLeft)]
        :debug-info {:strings+sources
                     [(source 'GLSL 460)]
                     :names
                     [(name :main "main")
                      (name :FragColor "FragColor")
                      (name :OurUniforms "OurUniforms")
                      (member-name :OurUniforms 0 "ourColor")
                      (name :ourUniforms "ourUniforms")]}
        :annotations [(decorate :FragColor 'Location 0)
                      (member-decorate :OurUniforms 0 'Offset 0)
                      (decorate :OurUniforms 'Block)
                      (decorate :ourUniforms 'DescriptorSet 0)
                      (decorate :ourUniforms 'Binding 0)]
        :types [(add-label (type-void) :void)
                (add-label (type-fn :void) :3)
                (add-label (type-float 32) :float)
                (add-label (type-vec :float 4) :v4float)
                (add-label (type-pointer 'Output :v4float) :_ptr_Output_v4float)
                (add-label (variable :_ptr_Output_v4float 'Output) :FragColor)
                (add-label (type-struct :v4float) :OurUniforms)
                (add-label (type-pointer 'Uniform :OurUniforms) :_ptr_Uniform_OurUniforms)
                (add-label (variable :_ptr_Uniform_OurUniforms 'Uniform) :ourUniforms)
                (add-label (type-int 32 1) :int)
                (add-label (constant :int 0) :int_0)
                (add-label (type-pointer 'Uniform :v4float) :_ptr_Uniform_v4float)]
        :fn-declarations nil
        :fn-definitions [(function :void :main 'None :3
                                   (label :5)
                                   (add-label (access-chain :_ptr_Uniform_v4float :ourUniforms :int_0) :16)
                                   (add-label (load :v4float :16) :17)
                                   (store :FragColor :17)
                                   (return))]}))

