(ns lssl.compiler
  (:require [lssl.opcodes :as op]
            [clojure.tools.analyzer.jvm :as ana.jvm]))

(defn analyse
  [s-expression]
  (ana.jvm/analyze s-expression
                   (ana.jvm/empty-env)
                   {:passes-opts {:validate/unresolvable-symbol-handler
                                  (fn [a b c] nil)}}))

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
                [(op/name :main "main")
                 (op/name :FragColor "FragColor")
                 (op/name :Inputs "Inputs")
                 (op/member-name :Inputs 0 "inputs")
                 (op/name :inputs "inputs")]}
   :annotations [(op/decorate :FragColor 'Location 0)
                 (op/member-decorate :Inputs 0 'Offset 0)
                 (op/decorate :Inputs 'Block)
                 (op/decorate :inputs 'DescriptorSet 0)
                 (op/decorate :inputs 'Binding 0)]
   :types [(op/add-label (op/type-void) :void)
           (op/add-label (op/type-float 32) :float)
           (op/add-label (op/type-vec :float 4) :v4float)
           (op/add-label (op/type-pointer 'Output :v4float) :_ptr_Output_v4float)
           (op/add-label (op/variable :_ptr_Output_v4float 'Output) :FragColor)
           (op/add-label (op/type-struct :v4float) :OurUniforms)
           (op/add-label (op/type-pointer 'Uniform :OurUniforms) :_ptr_Uniform_OurUniforms)
           (op/add-label (op/variable :_ptr_Uniform_OurUniforms 'Uniform) :ourUniforms)
           (op/add-label (op/type-int 32 1) :int)
           (op/add-label (op/constant :int 0) :int_0)
           (op/add-label (op/type-pointer 'Uniform :v4float) :_ptr_Uniform_v4float)]
   :fn-declarations nil
   :fn-definitions []})

; TODO: add entry point link to FragColor

(defn compile-type
  [symbols type]
  ; FIXME: update symbols
  (case type
    void :void))

(defn compile-fun-type
  [symbols return-type-label]
  (let [label (keyword (str "fn_" (name return-type-label)))
        type-def (op/add-label (op/type-fn return-type-label) label)
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
  [symbols {[var-name] :form
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

(defn compile-getx
  [symbols
   {[_ var field] :form
    [_ key-ast] :args :as ast}]
  (let [;field (:val key-ast)
        [symbols val-label] (label symbols)
        access-chain (op/access-chain (get-pointer-type-label symbols var field)
                                      (get-var-label symbols var)
                                      (get-field-index symbols var field))]
    [symbols
     [(op/add-label access-chain val-label)
      (op/load (get-field-type-label symbols var field) val-label)]]))

(defn dispatch-custom-expression
  [symbols {:keys [form] :as ast}]
  (case (first form)
    reset! (compile-reset-bang symbols ast)
    getx   (compile-getx symbols ast)))

(defn compile-expression
  [symbols ast]
  (case (:op ast)
    :invoke (dispatch-custom-expression symbols ast)))

(defn compile-body
  [symbols name instruction-asts]
  (let [[symbols compiled-instructions]
        (reduce #(compile-expression (first %1) %2) [symbols []] instruction-asts)]
    [symbols
     (concat [(op/label (keyword (str "_" name "_entrypoint")))]
             compiled-instructions
             [(op/return)])]))

(defn compile-defun
  [symbols
   {[_ return-type name] :form
    [_ _ args-ast & body-ast] :args :as ast}]
  (let [return-type-label (compile-type symbols return-type)
        [symbols fun-type-label] (compile-fun-type symbols return-type-label)
        [symbols compiled-body] (compile-body symbols name body-ast)
        fn-def (apply op/function return-type-label (keyword name)
                      (compile-args symbols args-ast)
                      fun-type-label
                      compiled-body)]
    (update symbols :fn-definitions #(conj % fn-def))))

(defn compile-float-type
  [symbols width]
  (let [label (keyword (str "_float_" width))
        type-def (op/add-label (op/type-float width) label)]
    [(update symbols :types #(conj % type-def))
     label]))

(defn compile-vec-type
  [symbols type size]
  (let [[symbols float-type-label]
        (compile-float-type symbols 32)
        label (keyword (str "v" size "float"))
        type-def (op/add-label (op/type-vec float-type-label size) label)]
    [(update symbols :types #(conj % type-def))
     label]))

(defn compile-field-type
  [symbols type index path]
  (let [[symbols label]
        (case type
          vec4 (compile-vec-type symbols float 4))]
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
  (let [path [name] ; FIXME: deep nesting
        [symbols field-type-labels]
        (reduce (partial compile-struct-fields-reducer path) [symbols [] 0] fields)
        label (keyword name)
        struct-def (op/add-label (apply op/type-struct field-type-labels) label)]
    [(update symbols :types #(conj % struct-def))
     label]))

(defn compile-defuniform
  [symbols
   {[_ var struct-name metadata & fields] :form
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
                                      :type struct-name}))))

(defn dispatch-custom
  [symbols
   {:keys [form] :as ast}]
  (case (first form)
    defversion symbols #_(compile-defversion (rest form))
    defout symbols
    defuniform (compile-defuniform symbols ast)  #_"TODO: impl all of these"
    defun (compile-defun symbols ast)
                                        ;TODO: does this belong here reset! (compile-reset-bang symbols ast)
    ))

                                        ; TODO: maybe we need to be dynamic like mage and avoid multimethods, let's seen
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

(defn sym
  [ast]
  (compile (init-symbols ast) ast))

(defn lsslc
  [source]
  (-> source
      analyse
      sym
      #_lssl/emit))

(comment
  (def lssl-src
    '[(defversion 460 core)

      (defout FragColor vec4
        {:layout {:location 0}})

      (defuniform inputs Inputs
        {:layout {:memory :std140
                  :binding 0}}
        (color vec4))

      (defun void main []
        (reset! FragColor (getx inputs color)))])

  (lsslc lssl-src)

  )
