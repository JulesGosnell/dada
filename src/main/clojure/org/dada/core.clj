(ns org.dada.core
    (:import (clojure.lang DynamicClassLoader ISeq IFn)
	     (java.util ArrayList)
	     (java.beans PropertyDescriptor)
	     (org.dada.core
	      Creator
	      FilteredView
	      FilteredView$Filter
	      Getter
	      GetterMetadata
	      Metadata
	      Model
	      Transformer
	      Transformer$Transform
	      Update
	      VersionedModelView
	      View)
	     (org.dada.demo Client)
	     (org.springframework.context.support ClassPathXmlApplicationContext)
	     (org.dada.asm ClassFactory)))
 
(defn start-server [#^String name]
  (System/setProperty "server.name" name)
  (let [context (ClassPathXmlApplicationContext. "application-context.xml")]
    (def *spring-context* context)
    (.getBean context "metaModel")))

(defn start-client [#^String name]
  (Client/main (into-array String (list name))))

(defn insert [#^View view item]
  (.update view (list (Update. nil item)) '() '()))

(defn insert-n [#^View view #^ISeq items]
  (.update view (map (fn [item] (Update. nil item)) items) '() '()))

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
   '())
  view)

(defn disconnect [#^Model model #^View view]
  (.deregisterView model view)
  (.update
   view
   '()
   '()
   (map
    #(Update. % nil)
    (.getData model))
   view))

(defn collection [& args]
  (let [array-list (ArrayList. #^Integer (count args))]
    (.addAll array-list args)
    array-list))

(def *class-factory* (new ClassFactory))

;; e.g.
;;(make-class
;; factory
;; "org.dada.tmp.Amount"
;; :id int :version int :amount double)
(defn make-class [#^String class-name #^Class superclass & attribute-key-types]
  (let [attribute-map (apply array-map attribute-key-types)]
    (. #^DynamicClassLoader
       (deref clojure.lang.Compiler/LOADER)
       (defineClass class-name 
	 (.create 
	  *class-factory*
	  class-name
	  superclass
	  (if (empty? attribute-map)
	    nil
	    (into-array (map 
			 (fn [[#^Keyword key #^Class type]]
			     (into-array (list (.getCanonicalName type) (name key))))
			 attribute-map))))))))

;; fields => [model field]


;; see http://groups.google.com/group/clojure/browse_thread/thread/106a2f73fb49f492#

;; I'm going with directly invoking this ctor, because it seems to be
;; calling a ctor directly on a class, although the other option calls
;; new on a symbol... What I really want to do is come up with
;; something that compiles down to bytecode that calls the correct
;; ctor for the given args directly... - TODO - more thought...
(defn make-instance [#^Class class & args]
  (clojure.lang.Reflector/invokeConstructor class (to-array args)))

(require '[clojure.contrib.str-utils2 :as s])

;; TODO: how do we confirm that with-meta is doing the right thing for
;; both input and output types...

(defn make-getter-name [#^String property-name]
  (str "get" (s/capitalize property-name)))

(defn getter-2 [#^Class input-type #^Class output-type #^String method-name]
  (let [method# (symbol (str "." method-name))]
    (eval
     `(proxy [Getter] [] 
	     (#^{:tag ~output-type} get [#^{:tag ~input-type} bean#] (~method# bean#))))
    ))

(defn getter [#^Class input-type #^Class output-type #^Keyword key]
  "return a Getter taking input-type returning output-type and calling get<Key>"
  (getter-2 input-type output-type (make-getter-name (name key))))

(defn make-accessor-2 [#^Class input-type #^Class output-type #^String method-name]
  (let [method# (symbol (str "." method-name))]
    (eval `(#^{:tag ~output-type} fn [#^{:tag ~input-type} bean#] (~method# bean#)))))

(defn make-accessor [#^Class input-type #^Class output-type #^Keyword key]
  "return a function taking input-type returning output-type and calling get<Key>"
  (let [method-name (symbol (make-getter-name (name key)))]
    (make-accessor-2 input-type output-type method-name)))

(defn creator [#^Class class]
  "make a Creator for the given Class"
  (proxy [Creator] [] 
	 (#^{:tag class} create [#^{:tag (type (into-array Object []))} args]
		  (apply make-instance class args))))

(defn metadata [#^Class class #^Keyword key #^Keyword version & attribute-key-types]
  "make Metadata for a given class"
  (let [attribute-map (apply array-map attribute-key-types)]
    (new GetterMetadata 
	 (creator class)
	 (collection (name key) (name version))
	 (vals attribute-map)
	 (map name (keys attribute-map))
	 (map (fn [[key type]] (getter class type key)) attribute-map))))

(defn model [#^String name #^Metadata metadata]
  (let [names (.getAttributeNames metadata)
	getters (.getAttributeGetters metadata)
	getter-map (apply array-map (interleave names getters))
	key-names (.getKeyAttributeNames metadata)
	id-getter (getter-map (first key-names))
	version-getter (getter-map (second key-names))]
    (new VersionedModelView name metadata id-getter version-getter)))

(defn clone-model [#^Model model #^String name]
  (let [metadata (.getMetadata model)
	names (. metadata getAttributeNames)
	getters (. metadata getAttributeGetters)
	getter-map (apply array-map (interleave names getters))
	keys (. metadata getKeyAttributeNames)
	key (first keys)
	key-getter (getter-map key)
	version (second keys)
	version-getter (getter-map version)]
    (VersionedModelView. name metadata key-getter version-getter)))

(defn apply-getters [#^ISeq getters value]
  "apply a list of getters to a value returning a list of their results"
  (map (fn [#^Getter getter] (.get getter value)) getters))

;;----------------------------------------
;; filtration - could do with another pass through
;;----------------------------------------

(defn make-filter [#^IFn filter-fn #^View view]
  (FilteredView. 
   (proxy
    [FilteredView$Filter]
    []
    (filter 
     [value]
     ;; TODO: this code needs to be FAST - executed online
     (filter-fn value)))
   (list view)
   ))

;; ATTN: VIEW may be a name to use when making a View, or the View itself
(defn do-filter [#^Model model #^ISeq keys #^IFn function view]
  "Get the values for KEYS from each value in the MODEL and pass them to the FUNCTION..."
  (let [metadata (.getMetadata model)
	name-to-getter (apply
			array-map
			(interleave
			 (.getAttributeNames metadata)
			 (.getAttributeGetters metadata)))
	getters (map #(name-to-getter (name %)) keys)
	view (if (instance? String view)
	       (clone-model model (str (.getName model) "." view))
	       view)]
    (connect model (make-filter #(apply function (apply-getters getters %)) view))
    view))

;;----------------------------------------
;; refactored to here
;;----------------------------------------

;; should [if required] connect a transformer between model and view...
;; each property should be able to register a converter fn as part of the transformation...
;; if  properties is null, don't do a transformation etc..

(defn make-transformer [getters view view-class]
  (def *getters* getters) ;; TODO - debug
  (def *view-class* view-class)
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
(defn expand-property [#^Class src-type #^Getter src-getter #^Keyword src-key & pvec]
  (let [pmap (apply array-map pvec)
	tgt-type (or (pmap :type) src-type)
	tgt-key (or (pmap :name) src-key)
	convert (or (pmap :convert) identity)
	default (or (pmap :default) ())
	defaulter (if (fn? default) default (fn [value] default))
	retriever (fn [value] (convert (. src-getter get value)))]
    [tgt-type tgt-key retriever]
    ))

;; TODO : should this not be a a macro - use proper syntax...
(defn make-proxy-getter [input-type output-type property-name]
  (let [method (symbol (make-getter-name property-name))]
    (eval (list 'proxy '[Getter] '[] (list 'get '[bean] (list '. 'bean method))))
    ))

(defn make-getter-map [tgt-class fields]
  (let [types (map first fields)
	names (map second fields)
	getters (map (fn [type name] (make-proxy-getter tgt-class type name)) types (map name names))]
    (apply array-map (interleave names getters))))

(defn make-fields [src-type-map src-getter-map attrs]
  (map (fn [attr]
	   (let [src-key (first attr)
		 src-type (src-type-map src-key)
		 src-getter (src-getter-map src-key)]
	     (apply expand-property src-type src-getter attr)))
       attrs))

;; TODO
;; allow selection from a number of src-models
;; allow selection into a number of src views
;; allow splitting :split <split-fn> implemented by router - should provide fn for tgt-view construction...
;; abstract out tgt-view construction so it can be done from parameters, during select, or on-demand from router...

(defn select [#^Model src-model #^Keyword src-key-key #^Keyword src-version-key #^ISeq attrs & pvec]
  (let [pmap (apply array-map pvec)
	src-metadata (. src-model getMetadata)
	src-keys (map keyword (. src-metadata getAttributeNames))
	src-types (. src-metadata getAttributeTypes)
	src-getters (. src-metadata getAttributeGetters)
	src-type-map (apply array-map (interleave src-keys src-types)) ; key:type
	src-getter-map (apply array-map (interleave src-keys src-getters)) ; key:getter
	fields (make-fields src-type-map src-getter-map attrs) ; selection ([type name ...])
	;; test to see if transform is needed should be done somewhere here...
	;; what is an :into param was given...none of this needs calculating...
	tgt-class-name (or (pmap :class) (.toString (gensym "org.dada.tmp.OutputValue")))
	tgt-model-name (or (pmap :model) (.toString (gensym "OutputModel")))
	filter-fn (pmap :filter)
   	tgt-types (map (fn [field] (nth field 0)) fields)
   	tgt-keys (map (fn [field] (nth field 1)) fields)
   	tgt-names (map name tgt-keys)
   	sel-getters (map (fn [field] (nth field 2)) fields)
	tgt-attrs (interleave tgt-keys tgt-types)
  	tgt-class (apply make-class tgt-class-name Object tgt-attrs)
	tgt-creator (proxy [Creator] [] (create [& args] (apply make-instance tgt-class args)))
   	tgt-getter-map (make-getter-map tgt-class fields) ; name:getter
	tgt-getters (vals tgt-getter-map)
   	tgt-key-getter (tgt-getter-map src-key-key)
   	tgt-version-getter (tgt-getter-map src-version-key)
	tgt-metadata (new GetterMetadata  tgt-creator  (collection tgt-key-getter tgt-version-getter) tgt-types tgt-names tgt-getters)
	view (VersionedModelView. tgt-model-name tgt-metadata tgt-key-getter tgt-version-getter)
	transformer (make-transformer sel-getters view tgt-class)
	filter (make-filter filter-fn transformer)
	]
    (connect src-model filter)
    view)
  )
