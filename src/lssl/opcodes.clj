(ns lssl.opcodes
  (:require [clojure.string :as str]))

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
