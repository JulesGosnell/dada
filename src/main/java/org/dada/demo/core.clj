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

(defn transform [model names view view-class]
  (let [metadata (. model getMetadata)
	property-names (. metadata getAttributeNames)]
    (if (= names property-names)
      ;; we are selecting all the fields in their original order - no
      ;; need for a transformation...
      ;; N.B. if just order has changed, could we just reorder metadata ?
      (do
	(connect model view)
	view)
      ;; we need some sort of transformation...
      (let [property-types (. metadata getAttributeTypes)
	    property-map (apply hash-map (interleave property-names property-types))
	    types (map property-map names)
	    model-class (. metadata getValueClass)
	    getters (map (fn [type name] (make-lambda-getter model-class type name)) types names)
	    transformer (make-transformer getters view view-class)]
	(connect model transformer)
	))))

;; properties is a vector of property descriptions of the form:
;; [input-name output-type output-name]
(defn select [input-model output-item-name output-model-name key-type key-name version-type version-name properties]
  (let [output-properties (map (fn [property] (vector (nth property 1) (nth property 2))) properties)
   	input-names (map (fn [property] (nth property 0)) properties)
   	output-types (map (fn [property] (nth property 1)) properties)
   	output-names (map (fn [property] (nth property 2)) properties)
	class-factory  (new ClassFactory)
  	output-class (apply make-class class-factory output-item-name output-properties)
   	key-getter (make-proxy-getter output-class key-type key-name)
   	version-getter (make-proxy-getter output-class version-type version-name)
   	output-getters (map (fn [property] (make-proxy-getter output-class (nth property 1) (nth property 2))) properties)
	output-metadata (new GetterMetadata output-class output-types output-names output-getters)
	view (new VersionedModelView output-model-name output-metadata key-getter version-getter)
	transformer (transform input-model input-names view output-class)]
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

  (def view (select input-model "org.dada.core.tmp.OutputItem" "OutputModel" (Integer/TYPE) "key" (Integer/TYPE) "version" (list ["id" (Integer/TYPE) "key"]["version" (Integer/TYPE) "version"]["amount" (Double/TYPE) "money"])))
  (insert metamodel view)

  (def item5 (make-instance input-class 3 2 2 6.0))
  (insert input-model item5)

  (def view2 (select view "org.dada.core.tmp.OutputItem2" "OutputModel2" Integer "key" Integer "version" (list ["key" Integer "key"]["version" Integer "version"]["money" Double "amount"])))

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
