(import org.dada.core.Update)
(import org.dada.core.Model)
(import org.dada.core.View)
(import org.dada.demo.Client)
(import org.springframework.context.support.ClassPathXmlApplicationContext)

(defn start-server [#^String name]
  (System/setProperty "server.name" name)
  (let [context (ClassPathXmlApplicationContext. "application-context.xml")]
    (.getBean context "metaModel")))

(defn start-client [#^String name]
  (Client/main (into-array String (list name))))

(defn insert [#^View view item]
  (.update view (list (Update. nil item)) '() '()))

(defn update [#^View view oldValue newValue]
  (.update view '() (list (Update. oldValue newValue)) '()))

(defn delete [#^View view value]
  (.update view '() '() (list (Update. value nil))))

(defn connect [#^Model model #^View view]
  (.update
   view
   (map
    #(Update. nil %) 
    (.getData 
     (.registerView model view)))
   '()
   '()))

(defn disconnect [#^Model model #^View view]
  (.deregisterView model view)
  (.update
   view
   '()
   '()
   (map
    #(Update. % nil)
    (.getData model))))

(import '(clojure.lang DynamicClassLoader))

;; e.g.
;;(make-class
;; factory
;; "org.dada.tmp.Amount"
;; ["int" "id"] ["int" "version"] ["double" "amount"])
(defn make-class2 [factory name & properties]
  (. #^DynamicClassLoader
     (deref clojure.lang.Compiler/LOADER)
     (defineClass name 
       (.create 
	factory
	name
	(into-array (map #(into-array String %) properties))))))

(defn make-class [factory name & properties]
  (. #^DynamicClassLoader
     (deref clojure.lang.Compiler/LOADER)
     (defineClass name 
       (.create 
	factory
	name
	(into-array (map (fn [pair] 
			     (let [array (make-array String 2)]
			       (aset array 0 (.getCanonicalName (first pair)))
			       (aset array 1 (first (rest pair)))
			       array))
			 properties))))))

;; select t0.f0 

;; 
;; fields => [model field]

(import org.dada.core.Getter)
(import org.dada.core.Transformer)
(import org.dada.core.Transformer$Transform)
(import org.dada.core.GetterMetadata)
(import org.dada.core.VersionedModelView)
(import org.dada.asm.ClassFactory)
(import java.beans.PropertyDescriptor)

(defn make-constructor [class & types]
  (let [ctor (symbol (str (.getCanonicalName class) "."))]
    (println ctor)
    (println types)
    (fn [& args] (apply ctor args))))

(defn make-instance [class & args]
  (eval (list* 'new class args)))

;; (defn make-instance [class & args]
;;   "'new' seems to be a macro which does not evaluate its 'class'
;; argument - so if you want to store the class in a variable and then
;; instantiate it, use (make-instance)."
;;   (apply 'new class args))

(require '[clojure.contrib.str-utils2 :as s])

;; TODO: how do we confirm that with-meta is doing the right thing for
;; both input and output types...

(defn make-getter-name [property-name]
  (str "get" (s/capitalize property-name)))

;; (defmacro make-lambda-getter [input-type output-type #^String property-name]
;;   (let [method-name (symbol (make-getter-name property-name))]
;;     `(fn [bean#] (. bean# ~method-name))))

(defn make-lambda-getter [input-type output-type #^String property-name]
  (let [method-name (symbol (make-getter-name property-name))]
    (eval (list 'fn '[bean] (list '. 'bean method-name)))))

;;(with-meta here (:tag output-type))

;; TODO : should this not be a a macro - use proper syntax...
(defn make-proxy-getter [input-type output-type property-name]
  (let [method (symbol (make-getter-name property-name))]
    (eval (list 'proxy '[Getter] '[] (list 'get '[bean] (list '. 'bean method))))
    ))

;; (defmacro make-proxy-getter [input-type output-type property-name]
;;   (let [method (symbol (make-getter-name property-name))]
;;     `(proxy [Getter] [] (get [bean#] (. bean# ~method)))
;;     ))

;; should [if required] connect a transformer between model and view...
;; each property should be able to register a converter fn as part of the transformation...
;; if  properties is null, don't do a transformation etc..

(defn make-transformer [getters view view-class]
  (new
   Transformer
   (list view)
   (proxy
    [Transformer$Transform]
    []
    (transform 
     [input]
     ;; TODO: this code needs to be FAST - executed online
     (apply make-instance view-class (map (fn [getter] (getter input)) getters)))
    )))

;; (transform input-model src-class property-map input-names view output-class)
(defn transform [model model-class property-map sel-getters tgt-names view view-class]
  (if (= tgt-names (keys property-map))
    ;; we are selecting all the fields in their original order - no
    ;; need for a transformation...
    ;; N.B. if just order has changed, could we just reorder metadata ?
    (connect model view)
    ;; we need some sort of transformation...
    (connect model (make-transformer sel-getters view view-class))
    ))

;; properties is a vector of property descriptions of the form:
;; [input-name output-type output-name]
;; [name & options :name string :type class :convert fn :default val/fn] - TODO: :key, :version
;; TODO :default not a good idea - would replace nulls
;; TODO what about type hints on lambdas ?
(defn expand-property [src-class src-type src-name & pvec]
  (let [pmap (apply array-map pvec)
	tgt-type (or (pmap :type) src-type)
	tgt-name (or (pmap :name) src-name)
	convert (or (pmap :convert) identity)
	default (or (pmap :default) ())
	defaulter (if (fn? default) default (fn [value] default))
	getter (make-proxy-getter src-class src-type src-name)
	retriever (fn [value] (convert (. getter get value)))]
    [tgt-type tgt-name retriever]
    ))

(defn make-getter-map [tgt-class fields]
  (apply
   conj
   (array-map)
   (map
    (fn [field]
	(let [type (nth field 0)
	      name (nth field 1)]
	  [name (make-proxy-getter tgt-class type name)]))
    fields)))

(defn make-fields [src-class src-map props]
  (map (fn [prop]
	   (let [src-name (first prop)
		 src-type (src-map src-name)]
	     (apply expand-property src-class src-type prop)))
       props))

(defn select [src-model tgt-class-name tgt-model-name key-name version-name props]
  (let [src-metadata (. src-model getMetadata)
	src-class (. src-metadata getValueClass)
	src-types (. src-metadata getAttributeTypes)
	src-names (. src-metadata getAttributeNames)
	src-map (apply array-map (interleave src-names src-types)) ; name:type
	fields (make-fields src-class src-map props) ; selection ([type name ...])
   	tgt-types (map (fn [field] (nth field 0)) fields)
   	tgt-names (map (fn [field] (nth field 1)) fields)
	src-getters (. src-metadata getAttributeGetters)
   	sel-getters (map (fn [field] (nth field 2)) fields)
	;; we should be able to take src-getters directly from metadata
	;; map them to name and then
	;;sel-getters (map (fn [name] (src-getter-map name)) tgt-names)
	tgt-props (map (fn [field] [(nth field 0) (nth field 1)]) fields) ; ([type name]..)
	class-factory (new ClassFactory)
  	tgt-class (apply make-class class-factory tgt-class-name tgt-props)
   	tgt-getter-map (make-getter-map tgt-class fields) ; name:getter
	tgt-getters (vals tgt-getter-map)
   	tgt-key-getter (tgt-getter-map key-name)
   	tgt-version-getter (tgt-getter-map version-name)
	tgt-metadata (new GetterMetadata tgt-class tgt-types tgt-names tgt-getters)
	view (VersionedModelView. tgt-model-name tgt-metadata tgt-key-getter tgt-version-getter)
	transformer (transform src-model src-class src-map sel-getters tgt-names view tgt-class)]
    view)
  )

(do

  (def metamodel (start-server "Server0"))
  (start-client "Server0")

  (import org.dada.asm.ClassFactory)
  (import org.dada.core.VersionedModelView)
  (import org.dada.core.GetterMetadata)
 
  (def factory (new ClassFactory))
  (def properties (list [(Integer/TYPE) "id"] [(Integer/TYPE) "version"] [(Long/TYPE) "time"] [(Double/TYPE) "amount"]))
  (def input-class (apply make-class factory "org.dada.tmp.InputItem" properties))
  (def input-types (map (fn [property] (nth property 0)) properties))
  (def input-names (map (fn [property] (nth property 1)) properties))
  (def input-getters (map (fn [property] (make-proxy-getter input-class (nth property 0) (nth property 1))) properties))
  (def input-key-getter (nth input-getters 0))
  (def input-version-getter (nth input-getters 1))
  (def input-metadata (new GetterMetadata input-class input-types input-names input-getters))
  (def input-model-name "InputModel")
  (def input-model (new VersionedModelView input-model-name input-metadata input-key-getter input-version-getter))
  (insert metamodel input-model)
  (def item (make-instance input-class 0 1 2 3.0))
  (insert input-model item)
  (. input-model getData)
  (. input-model getMetadata)

;; TODO: output item & model should be automatic unless specifically overridden..
;; & :class xxx :model yyy

  (def view
       (select
	input-model
	"org.dada.core.tmp.OutputItem"
	"OutputModel"
	"key"
	"version"
	(list ["id" :name "key"] ["version"] ["amount" :name "money"])
	))
  (insert metamodel view)

  (def item5 (make-instance input-class 3 2 2 6.0))
  (insert input-model item5)

  (def view2
       (select
	view
	"org.dada.core.tmp.OutputItem2"
	"OutputModel2"
	"key"
	"version"
	(list ["key"] ["version"] ["money" :name "amount"])
	))

  )

;; thoughts about select...

;; outputs: define new column names [and types?], name of new Java type and Model
;; inputs: define list of (Model, columns, column-mapping, (column-getter), column transform,...

;;where input type and output type differ (column-mappings have been
;;specified) connect a transformer to input-model, to prrform this.

;; connect input-model/transformer to output-model

;; start by transforming any models that need it
;; ensure all transforms types concur
;; connect all to output model




;; retrieve value class from Metadata
