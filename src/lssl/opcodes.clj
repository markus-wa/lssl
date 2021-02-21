(ns lssl.opcodes)

(defn opcode
  [opcode & args]
  {::opcode opcode ::args args})

(defn add-label
  [m label]
  (assoc m ::label label))

(defmacro defop
  [fn-name op arg-names]
  (let [arg-names-no& (remove (partial = '&) arg-names)
        url (str "https://www.khronos.org/registry/spir-v/specs/1.0/SPIRV.html#" op)]
    `(defn ~fn-name
       ~url
       ~(vec arg-names)
       ~(if (= (count arg-names) (count arg-names-no&))
          `(opcode '~op ~@arg-names)
          `(apply opcode '~op ~@arg-names-no&)))))

(defmacro run-macro
  "Run the specified macro once for each arg"
  [root-macro & args]
  `(do
     ~@(for [item args]
         `(~root-macro ~(first item) ~(second item) ~(nth item 2)))))

(run-macro defop
           [capability OpCapability [name]]
           [ext-inst-imports OpExtInstImport [import]]
           [memory-model OpMemoryModel [model version]]
           [entry-point OpEntryPoint [exec-model label name & interfaces]]
           [execution-mode OpExecutionMode [label mode]]
           [source OpSource [type version]]
           [name- OpName [label name]]
           [member-name OpMemberName [label offset member-name]]
           [decorate OpDecorate [label decoration & literals]]
           [member-decorate OpMemberDecorate [label literal-num decoration & literals]]
           [type-void OpTypeVoid []]
           [type-function OpTypeFunction [return-type & params]]
           [type-float OpTypeFloat [width]]
           [type-vector OpTypeVector [component-label size]]
           [variable OpVariable [type-label storage-class]]
           [type-struct OpTypeStruct [& member-type-labels]]
           [type-pointer OpTypePointer [storage-class type]]
           [type-int OpTypeInt [width signedness]]
           [constant OpConstant [type-label & values]]
           [access-chain OpAccessChain [result-type base & indices]]
           [function OpFunction [result-type function-control fn-type-label]]
           [function-end OpFunctionEnd []]
           [return OpReturn []]
           [composite-construct OpCompositeConstruct [type-label & values]])

(defn label
  [label]
  (add-label (opcode 'OpLabel) label))

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

(comment
  ;; how defop works
  (defop entry-point OpEntryPoint [exec-model label name & interfaces])

  ;; ==>
  (defn entry-point
    [exec-model label name & interfaces]
    (apply opcode 'OpEntryPoint exec-model label name interfaces)))
