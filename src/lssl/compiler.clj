(ns lssl.compiler
  (:require [lssl.opcodes :as op]))

(defn glsl-version
  [{[{:keys [form]}] :items :as _ast}]
  (if (= (first form) 'defversion)
    (second form)
    (throw (RuntimeException. "first statement must be GLSL version"))))


(defn init-symbols
  [ast]
  {:n-labels 0
   :header {"Version" 1.0
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
                [(op/source 'GLSL (glsl-version ast))]
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
   :types [(op/add-label (op/type-void) :void)]
   :fn-declarations nil
   :fn-definitions []})

;; TODO: add entry point link to FragColor

(defn compile-function
  [result-type name function-control fn-type & body]
  (concat [(op/add-label (op/function result-type function-control fn-type) name)]
          body
          [(op/function-end)]))

(defn compile-float-type
  [symbols width]
  (if-let [label (get-in symbols [:type-map 'float :type-label])]
    [symbols label]
    (let [label (keyword (str "_float_" width))
          type-def (op/add-label (op/type-float width) label)]
      [(-> symbols
           (update :types #(conj % type-def))
           (assoc-in [:type-map 'float :type-label] label))
       label])))

(defn compile-vec-type
  [symbols type size]
  (let [[symbols float-type-label]
        (compile-float-type symbols 32)
        label (keyword (str "v" size "float"))
        type-def (op/add-label (op/type-vector float-type-label size) label)]
    [(update symbols :types #(conj % type-def))
     label]))

(defn compile-type
  [symbols type]
  (if-let [label (get-in symbols [:type-map type :type-label])]
    [symbols label]
    (let [[symbols label]
          (case type
            void [symbols :void]
            vec4 (compile-vec-type symbols float 4))]
      [(assoc-in symbols [:type-map type :type-label] label)
       label])))

(defn compile-fun-type
  [symbols return-type-label]
  (let [label (keyword (str "fn_" (name return-type-label)))
        type-def (op/add-label (op/type-function return-type-label) label)
        symbols (update symbols :types #(conj % type-def))]
    [symbols label]))

(defn compile-args
  [symbols {:keys [form] :as ast}]
  (if (empty? form)
    'None
    (throw (RuntimeException. "fun args not implemented"))))

(declare compile
         compile-expression)

(defn label
  [symbols]
  [(update symbols :n-labels inc)
   (keyword (str "_x_" (:n-labels symbols)))])

(defn compile-reset-bang
  [symbols {[_ var-name] :form
            [_ val-ast] :args :as ast}]
  (let [[symbols compiled-val-form] (compile-expression symbols val-ast)
        [symbols val-label] (label symbols)
        labeled-val (op/add-label (last compiled-val-form) val-label)]
    [symbols
     (concat (conj (pop compiled-val-form) labeled-val)
             [(op/store (keyword var-name) val-label)])]))

(defn get-var-info
  [symbols var k]
  (get-in symbols [:globals-map var k]))

(defn get-var-label
  [symbols var]
  (get-var-info symbols var :label))

(defn get-field-info
  [symbols var field k]
  (let [type (get-var-info symbols var :type)]
    (get-in symbols [:type-map type field k])))

(defn get-pointer-type-label
  [symbols var field]
  (get-field-info symbols var field :pointer-type-label))

(defn get-field-index
  [symbols var field]
  (get-field-info symbols var field :index))

(defn get-field-type-label
  [symbols var field]
  (get-field-info symbols var field :type-label))

(defn compile-pointer-type
  [symbols var field]
  (let [label (get-pointer-type-label symbols var field)]
    (if false
      [symbols label]
      (let [var-kind (get-var-info symbols var :kind)
            field-type-label (get-field-info symbols var field :type-label)
            label (keyword (str "_ptr_" var-kind "_" (name field-type-label)))
            ptr-type (op/add-label (op/type-pointer var-kind field-type-label) label)]
        [(update symbols :types #(conj % ptr-type))
         label]))))

(defn compile-type-int
  [symbols sym width signed]
  (let [label (get-in symbols [:type-map sym :type-label])]
    (if label
      [symbols label]
      (let [label :int
            type (op/add-label (op/type-int width signed) label)]
        [(-> symbols
             (update :types #(conj % type))
             (assoc-in [:type-map sym :type-label] label))
         label]))))

(defn compile-const
  [symbols
   {:keys [tag val]}]
  (if (isa? tag Double)
    (let [[symbols type-label] (compile-float-type symbols 32)
          label (keyword (str "float_" val))
          const (op/add-label (op/constant type-label val) label)]
      [(update symbols :types #(conj % const))
       label])
    (throw (RuntimeException. "not implemented"))))

(defn compile-field-index
  [symbols var field]
  (let [idx (get-field-index symbols var field)
        [symbols type-label] (compile-type-int symbols 'int 32 1)
        label (keyword (str "int_" idx))
        const (op/add-label (op/constant type-label idx) label)]
    [(update symbols :types #(conj % const))
     label]))

(defn compile-get-in
  [symbols ast]
  #p ast
  [symbols [] #_label])

(defn compile-get
  [symbols
   {[[_ var field]] :raw-forms
    [_ key-ast] :args :as ast}]
  (let [[symbols pointer-type-label]
        (compile-pointer-type symbols var field)
        [symbols field-index-label]
        (compile-field-index symbols var field)
        [symbols val-label] (label symbols)
        access-chain (op/access-chain pointer-type-label
                                      (get-var-label symbols var)
                                      field-index-label)]
    [symbols
     [(op/add-label access-chain val-label)
      (op/load (get-field-type-label symbols var field) val-label)]]))

;; FIXME: check size against args len
(defn compile-vec-vals
  [size symbols
   {:keys [args] :as ast}]
  (loop [symbols symbols
         args args
         expressions []
         val-labels []]
    (if (empty? args)
      [symbols val-labels]
      (let [[symbols exp val-label]
            (compile-expression symbols (first args))]
        (recur symbols
               (rest args)
               (concat expressions exp)
               (conj val-labels val-label))))))

(defn compile-vec
  [size symbols ast]
  (let [[symbols type-label]
        (compile-type symbols (symbol (str "vec" size)))
        [symbols val-expressions val-labels]
        (compile-vec-vals size symbols ast)
        composite-construct
        (apply op/composite-construct type-label val-labels)
        [symbols val-label] (label symbols)]
    [symbols
     (conj val-expressions
           composite-construct)
     (op/add-label composite-construct val-label)]))

(defn dispatch-custom-expression
  [symbols {:keys [form] :as ast}]
  (case (first form)
    vec4 (compile-vec 4 symbols ast)
    get-in (compile-get-in symbols ast)))

(defn dispatch-static-call
  [symbols ast]
  (case (:method ast)
    get (compile-get symbols ast)))

(defn compile-expression
  [symbols ast]
  (case (:op ast)
    :invoke (dispatch-custom-expression symbols ast)
    :static-call (dispatch-static-call symbols ast)
    :const (compile-const symbols ast)))

(defn dispatch-custom-statement
  [symbols {:keys [form] :as ast}]
  (case (first form)
    reset! (compile-reset-bang symbols ast)))

(defn dispatch-static-call-statement
  [symbols ast]
  (throw (RuntimeException. "unimplemented")))

(defn compile-statement
  [symbols ast]
  (case (:op ast)
    :invoke (dispatch-custom-statement symbols ast)
    :static-call (dispatch-static-call-statement symbols ast)))

(defn compile-body
  [symbols name instruction-asts]
  (let [[symbols compiled-instructions]
        (reduce #(compile-statement (first %1) %2) [symbols []] instruction-asts)]
    [symbols
     (concat [(op/label (keyword (str "_" name "_entrypoint")))]
             compiled-instructions
             [(op/return)])]))

(defn compile-defun
  [symbols
   {[_ return-type name] :form
    [_ _ args-ast & body-ast] :args :as ast}]
  (let [[symbols return-type-label] (compile-type symbols return-type)
        [symbols fun-type-label] (compile-fun-type symbols return-type-label)
        [symbols compiled-body] (compile-body symbols name body-ast)
        fn-def (apply compile-function return-type-label (keyword name)
                      (compile-args symbols args-ast)
                      fun-type-label
                      compiled-body)]
    (update symbols :fn-definitions #(conj % fn-def))))

(defn compile-field-type
  [symbols type index path]
  (let [[symbols label]
        (compile-type symbols type)]
    [(-> symbols
         (assoc-in (concat [:type-map] path [:type-label]) label)
         (assoc-in (concat [:type-map] path [:index]) index))
     label]))

(defn compile-struct-fields-reducer
  [path [symbols labels index] [field type]]
  (let [[symbols label]
        (compile-field-type symbols type index (conj path field))]
    [symbols
     (conj labels label)
     (inc index)]))

(defn compile-struct
  [symbols name fields]
  (let [path [name] ;; FIXME: deep nesting
        [symbols field-type-labels]
        (reduce (partial compile-struct-fields-reducer path) [symbols [] 0] (partition 2 fields))
        label (keyword name)
        struct-def (op/add-label (apply op/type-struct field-type-labels) label)]
    [(update symbols :types #(conj % struct-def))
     label]))

(defn compile-defuniform
  [symbols
   {[_ var struct-name metadata fields] :form
    [_ _] :args :as ast}]
  (let [[symbols struct-label]
        (compile-struct symbols struct-name fields)
        ptr-label (keyword (str "_ptr_Uniform_" (name struct-name)))
        pointer-def (op/add-label (op/type-pointer 'Uniform struct-label) ptr-label)
        var-label (keyword var)
        var-def (op/add-label (op/variable ptr-label 'Uniform) var-label)]
    (-> symbols
        (update :types #(vec (concat % [pointer-def var-def])))
        (assoc-in [:globals-map var] {:label var-label
                                      :type struct-name
                                      :kind 'Uniform}))))

(defn compile-defout
  [symbols
   {[_ var type metadata] :form :as ast}]
  (let [[symbols type-label]
        (compile-type symbols type)
        ptr-label (keyword (str "_ptr_Output_" (name type-label)))
        pointer-def (op/add-label (op/type-pointer 'Output type-label) ptr-label)
        var-label (keyword var)
        var-def (op/add-label (op/variable ptr-label 'Output) var-label)]
    (-> symbols
        (update :types #(vec (concat % [pointer-def var-def])))
        (assoc-in [:globals-map var] {:label var-label
                                      :type type
                                      :kind 'Output}))))

(defn dispatch-custom
  [symbols
   {:keys [form] :as ast}]
  (case (first form)
    defversion symbols
    defout (compile-defout symbols ast)
    defuniform (compile-defuniform symbols ast)  ;; TODO: impl all of these
    defun (compile-defun symbols ast)
    ;; TODO: does this belong here? - reset! (compile-reset-bang symbols ast)
    ))

;; TODO: maybe we need to be dynamic like mage and avoid multimethods, let's seen
(defmulti compile (fn [_ ast] (class ast)))

(defmethod compile
  clojure.lang.IPersistentCollection
  [symbols ast]
  (if (empty? ast)
    symbols
    (compile (compile symbols (first ast))
             (rest ast))))

(defmethod compile
  clojure.lang.IPersistentMap
  [symbols ast]
  (case (:op ast)
    :vector (compile symbols (:items ast))
    :invoke (dispatch-custom symbols ast)))

(defn compile-ast
  [ast]
  (compile (init-symbols ast) ast))
