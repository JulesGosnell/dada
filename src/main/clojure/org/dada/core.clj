(ns org.dada.core
    (:import (clojure.lang DynamicClassLoader ISeq IFn)
	     (java.util
	      ArrayList
	      Collection)
	     (java.util.concurrent
	      ConcurrentHashMap)
	     (java.beans
	      PropertyDescriptor)
	     (org.springframework.context.support ClassPathXmlApplicationContext)
	     (org.springframework.beans.factory BeanFactory)
	     (org.slf4j Logger LoggerFactory)
	     (org.dada.asm ClassFactory)
	     (org.dada.core
	      Creator
	      Factory
	      FilteredView
	      FilteredView$Filter
	      Getter
	      GetterMetadata
	      LazyView
	      Metadata
	      Model
	      Reducer
	      Reducer$Strategy
	      Splitter
	      Splitter$Strategy
	      ServiceFactory
	      SparseOpenLazyViewTable
	      Transformer
	      Transformer$Transform
	      Update
	      VersionedModelView
	      View
	      )
	     (org.dada.demo Client)
	     ))

(defn debug [foo]
  (println "DEBUG: " foo)
  foo)

(set! *warn-on-reflection* true)
 
(def #^Logger *logger* (LoggerFactory/getLogger "org.dada.core"))

(defn start-server [#^String name]
  (System/setProperty "server.name" name)
  (def #^ClassPathXmlApplicationContext *spring-context* (ClassPathXmlApplicationContext. "application-context.xml"))
  (def #^ServiceFactory *internal-view-service-factory* (.getBean #^BeanFactory *spring-context* "internalViewServiceFactory"))
  (.getBean #^BeanFactory *spring-context* "metaModel"))

(defn start-client [#^String name]
  (Client/main (into-array String (list name))))

(defn insert [#^View view item]
  (.update view (list (Update. nil item)) '() '())
  item)

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
  (.update
   view
   '()
   '()
   (map #(Update. % nil) (.deregisterView model view)))
   view)

(defn collection [& args]
  (let [array-list (ArrayList. #^Integer (count args))]
    (.addAll array-list args)
    array-list))

(def #^ClassFactory *class-factory* (new ClassFactory))

;; e.g.
;;(make-class
;; factory
;; "org.dada.tmp.Amount"
;; :id int :version int :amount double)
(defn attribute-array [& attribute-key-types]
  (if (empty? attribute-key-types)
    nil
    (into-array (map 
		 (fn [[#^Keyword key #^Class type]]
		     (into-array (list (.getCanonicalName type) (name key))))
		 (apply array-map attribute-key-types)))))

(defn make-class [#^String class-name #^Class superclass #^ISeq & attribute-key-types]
  (.
   #^DynamicClassLoader (deref clojure.lang.Compiler/LOADER)
   (defineClass class-name 
     (.create
      *class-factory*
      class-name
      superclass
      (apply attribute-array attribute-key-types)))))

;; fields => [model field]


;; see http://groups.google.com/group/clojure/browse_thread/thread/106a2f73fb49f492#

;; I'm going with directly invoking this ctor, because it seems to be
;; calling a ctor directly on a class, although the other option calls
;; new on a symbol... What I really want to do is come up with
;; something that compiles down to bytecode that calls the correct
;; ctor for the given args directly... - TODO - more thought...
(defn make-instance [#^Class class & args]
  (clojure.lang.Reflector/invokeConstructor class (to-array args)))

;; TODO: how do we confirm that with-meta is doing the right thing for
;; both input and output types...

(defn make-getter-name [#^String property-name]
  (str "get" (.toUpperCase (.substring property-name 0 1)) (.substring property-name 1 (.length property-name))))

(defn getter-2 [#^Class input-type #^Class output-type #^String method-name]
  (let [method-symbol (symbol (str "." method-name))
	arg-symbol (with-meta 's {:tag (.getCanonicalName input-type)})]
    (eval `(proxy [Getter] [] 
		  (#^{:tag ~output-type} get [~arg-symbol] (~method-symbol ~arg-symbol))))))

(defn getter [#^Class input-type #^Class output-type #^Keyword key]
  "return a Getter taking input-type returning output-type and calling get<Key>"
  (getter-2 input-type output-type (make-getter-name (name key))))

;; (defn accessor-2 [#^Class input-type #^Class output-type #^String method-name]
;;   (let [method-symbol (symbol (str "." method-name))
;; 	arg-symbol (with-meta 's {:tag (.getCanonicalName input-type)})]
;;     (eval `(fn [~arg-symbol] (~method-symbol ~arg-symbol)))))

;; (defn make-accessor-2 [#^Class input-type #^Class output-type #^String method-name]
;;   (let [method# (symbol (str "." method-name))]
;;     (eval `(#^{:tag ~output-type} fn [#^{:tag ~input-type} bean#] (~method# bean#)))))

;; (defn make-accessor [#^Class input-type #^Class output-type #^Keyword key]
;;   "return a function taking input-type returning output-type and calling get<Key>"
;;   (let [method-name (symbol (make-getter-name (name key)))]
;;     (make-accessor-2 input-type output-type method-name)))

(defn creator [#^Class class]
  "make a Creator for the given Class"
  (proxy [Creator] [] 
	 (#^{:tag class} create [#^{:tag (type (into-array Object []))} args]
		  (apply make-instance class args))))

(defn #^Metadata metadata [#^Class class #^Keyword key #^Keyword version & attribute-key-types]
  "make Metadata for a given class"
  (let [attribute-map (apply array-map attribute-key-types)]
    (new GetterMetadata 
	 (creator class)
	 (collection (name key) (name version))
	 (vals attribute-map)
	 (map name (keys attribute-map))
	 (map (fn [[key type]] (getter class type key)) attribute-map))))

(defn #^Metadata class-metadata
  "create metadata for a Model containing instances of a Class"
  [#^String class-name #^Class superclass #^Keyword key-key #^Keyword version-key #^ISeq attributes]
  (apply
   metadata
   (apply make-class class-name superclass attributes)
   key-key
   version-key
   attributes))

(defn model [#^String name #^Metadata metadata]
  (let [names (.getAttributeNames metadata)
	key-names (.getKeyAttributeNames metadata)
	id-getter (.getAttributeGetter metadata (first key-names))
	version-getter (.getAttributeGetter metadata (second key-names))]
    (new VersionedModelView name metadata id-getter version-getter)))

(defn clone-model [#^Model model #^String name]
  (let [metadata (.getMetadata model)
	keys (.getKeyAttributeNames metadata)
	key-getter (.getAttributeGetter metadata (first keys))
	version-getter (.getAttributeGetter metadata (second keys))]
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
(defn do-filter [view #^Model model #^ISeq keys #^IFn function]
  "Get the values for KEYS from each value in the MODEL and pass them to the FUNCTION..."
  (let [metadata (.getMetadata model)
	getters (map #(.getAttributeGetter metadata (name %)) keys)
	view (if (instance? String view)
	       (clone-model model (str (.getName model) "." view))
	       view)]
    (connect model (make-filter #(apply function (apply-getters getters %)) view))
    view))

;;----------------------------------------
;; transformation
;; selection of a subset of attributes from one type into another of a different shape
;; TODO: creation of extra literal and composed values
;;----------------------------------------

(defn make-transformer [#^ISeq init-fns #^Metadata metadata #^View view]
  (new
   Transformer
   (list view)
   (proxy
    [Transformer$Transform]
    []
    (transform 
     [input]
     ;; TODO: this code needs to be FAST - executed online
     (.create metadata #^Collection (map (fn [#^IFn init-fn] (init-fn input)) init-fns)))
    )))

;; returns [key type init-fn]
(defmulti do-transform-attribute (fn [descrip md] (class descrip)))

;; simple one-to-one mapping
(defmethod do-transform-attribute 
  clojure.lang.Keyword [#^Keyword key #^Metadata md]
  (let [key-name (name key)
	type(.getAttributeType md key-name)
	getter (.getAttributeGetter md key-name)
	value-fn (fn [value] (.get getter value))]
    [key type value-fn]))

;;(defmethod do-transform-attribute
;;  clojure.lang.PersistentList [#^ISeq attribute #^Metadata md]
;;  (apply do-transform-attribute attribute))

(defn third [s] (nth s 2))

(defn do-transform [#^String suffix #^Model src-model #^Keyword key-key #^Keyword version-key & 
		    #^ISeq attribute-keys]
  (let [#^Metadata model-metadata (.getMetadata src-model)
	key-details (do-transform-attribute key-key model-metadata)
	key-type (second key-details)
	version-details (do-transform-attribute version-key model-metadata)
	version-type (second version-details)
	attribute-details (map #(do-transform-attribute % model-metadata) attribute-keys)
	attribute-types (map second attribute-details)
	init-fns 
	(list*
	 (third key-details)
	 (third version-details)
	 (map third attribute-details))
	attributes (interleave attribute-keys attribute-types)
	class-name (name (gensym "org.dada.demo.whales.Transform"))
	superclass Object
	view-metadata (class-metadata class-name superclass key-key version-key (concat (list key-key key-type version-key version-type) attributes))
	view-name (str (.getName src-model) "." suffix)
	view (model view-name view-metadata)
	transformer (make-transformer init-fns view-metadata view)]
    (connect src-model transformer)
    view))

;;----------------------------------------
;; splitting
;; split model into smaller models based on content of one attribute of each value
;; TODO: accept ready-made table, split on more than one attribute
;;----------------------------------------

(defn make-splitter
  [#^Model src-model #^Symbol key #^boolean mutable #^IFn value-to-key #^IFn key-to-value #^IFn view-hook]
  (let [map (new ConcurrentHashMap)
	prefix (str (.getName src-model) "." (name key) "=")
	metadata (.getMetadata src-model)
	view-factory (proxy
		      [Factory]
		      []
		      (create [key]
			      (let [value (key-to-value key)
				    view (model (str prefix value) metadata)]
				(.decouple
				 #^ServiceFactory *internal-view-service-factory*
				 (view-hook view value)))
			      ))
	lazy-factory (proxy [Factory] [] (create [key] (new LazyView map key view-factory)))
	table (new SparseOpenLazyViewTable map lazy-factory)
	getter (.getAttributeGetter metadata (name key))]
    (new
     Splitter
     (proxy
      [Splitter$Strategy]
      []
      (getMutable [] mutable)
      (getKey [value] (value-to-key (.get getter value)))
      (getViews [key] (list (. table get key)))
      ))))
    
(defn do-split
  [#^Model src-model #^Keyword key #^boolean mutable #^IFn value-to-key #^IFn key-to-value #^IFn view-hook]
  (connect src-model (make-splitter src-model key mutable value-to-key key-to-value view-hook))
  )

;;----------------------------------------
;; reduction
;; reduce all values in a model to a single one - by applying a fn - e.g. sum, count, etc...
;; TODO: count, average (sum/count), mean, mode, minimum, maximum - min/max more tricky, need to carry state
;;----------------------------------------

(defn make-reducer
  [#^Model src-model #^Keyword key-key key-value #^Reducer$Strategy strategy #^Metadata tgt-metadata name-fn]
  (let [key-name (name key-key)
	view-name (str (.getName src-model) "." (name-fn key-name))]
    (Reducer. view-name tgt-metadata key-value strategy)))

;; sum specific stuff - should be in its own file

;; TODO - should just be a class, not a fn - but then we wouldn't be able to compile this file
(defn #^Metadata sum-reducer-metadata [#^Class key-type]
  (class-metadata (name (gensym "org.dada.core.reducer.Sum")) Object :key :version [:key key-type :version Integer :sum Number]))

(defn make-sum-reducer-strategy [#^Keyword attribute-key #^Metadata src-metadata #^Metadata tgt-metadata]
  (let [getter (.getAttributeGetter src-metadata (name attribute-key))
	accessor (fn [value] (.get getter value))
	new-value (fn [#^Update update] (accessor (.getNewValue update)))
	old-value (fn [#^Update update] (accessor (.getOldValue update)))
	creator (.getCreator tgt-metadata)]
    (proxy
     [Reducer$Strategy]
     []
     (initialValue [] 0)
     (initialType [type] type)
     (currentValue [& args] (.create creator (into-array Object args)))
     (reduce [insertions updates deletions]
	     (-
	      (+
	       (reduce #(+ %1 (new-value %2)) 0 insertions)
	       (reduce #(+ %1 (- (new-value %2) (old-value %2))) 0 updates))
	      (reduce #(+ %1 (old-value %2)) 0 deletions))
	     )
     (apply [currentValue delta] (+ currentValue delta))
     )))

(defn do-reduce-sum [#^Model model #^Keyword attribute-key attribute-type attribute-value]
  (let [tgt-metadata (sum-reducer-metadata attribute-type)
	strategy (make-sum-reducer-strategy attribute-key (.getMetadata model) tgt-metadata)
	view-name-fn #(str "sum(" % ")")
	reducer (make-reducer model attribute-key attribute-value strategy tgt-metadata view-name-fn)]
    (connect model reducer)))

;; count specific stuff - should be in its own file

;; TODO: pass through reduction key - e.g. count(weight) - will java allow this ?
(defn #^Metadata count-reducer-metadata [#^Class attribute-type]
  (class-metadata (name (gensym "org.dada.core.reducer.Count")) Object :key :version [:key attribute-type :version Integer :count Number]))

(defn make-count-reducer-strategy [#^Metadata src-metadata #^Metadata tgt-metadata]
  (let [creator (.getCreator tgt-metadata)]
    (proxy
     [Reducer$Strategy]
     []
     (initialValue [] 0)
     (initialType [type] Integer)
     (currentValue [& args] (.create creator (into-array Object args)))
     (reduce [insertions updates deletions] (- (count insertions) (count deletions)))
     (apply [currentValue delta] (+ currentValue delta))
     )
    ))

(defn do-reduce-count
  ([#^Model model #^Class key-type key-value]
   (do-reduce-count model key-type key-value (count-reducer-metadata key-type))
   )
  ([#^Model model key-type key-value #^Metadata tgt-metadata]
   (let [strategy (make-count-reducer-strategy (.getMetadata model) tgt-metadata)
	view-name-fn (fn [arg] "count()")
	reducer (make-reducer model :count key-value strategy tgt-metadata view-name-fn)]
    (connect model reducer))))

;;----------------------------------------
;; refactored to here
;;----------------------------------------

;; should [if required] connect a transformer between model and view...
;; each property should be able to register a converter fn as part of the transformation...
;; if  properties is null, don't do a transformation etc..

;; (defn make-transformer-old [getters view view-class]
;;   (def *getters* getters) ;; TODO - debug
;;   (def *view-class* view-class)
;;   (new
;;    Transformer
;;    (list view)
;;    (proxy
;;     [Transformer$Transform]
;;     []
;;     (transform 
;;      [input]
;;      ;; TODO: this code needs to be FAST - executed online
;;      (apply make-instance view-class (map (fn [getter] (getter input)) getters)))
;;     )))


;; ;; (transform input-model src-class property-map input-names view output-class)
;; (defn transform [model model-class property-map sel-getters tgt-names view view-class]
;;   (if (= tgt-names (keys property-map))
;;     ;; we are selecting all the fields in their original order - no
;;     ;; need for a transformation...
;;     ;; N.B. if just order has changed, could we just reorder metadata ?
;;     (connect model view)
;;     ;; we need some sort of transformation...
;;     (connect model (make-transformer-old sel-getters view view-class))
;;     ))

;; ;; properties is a vector of property descriptions of the form:
;; ;; [input-name output-type output-name]
;; ;; [name & options :name string :type class :convert fn :default val/fn] - TODO: :key, :version
;; ;; TODO :default not a good idea - would replace nulls
;; ;; TODO what about type hints on lambdas ?
;; (defn expand-property [#^Class src-type #^Getter src-getter #^Keyword src-key & pvec]
;;   (let [pmap (apply array-map pvec)
;; 	tgt-type (or (pmap :type) src-type)
;; 	tgt-key (or (pmap :name) src-key)
;; 	convert (or (pmap :convert) identity)
;; 	default (or (pmap :default) ())
;; 	defaulter (if (fn? default) default (fn [value] default))
;; 	retriever (fn [value] (convert (. src-getter get value)))]
;;     [tgt-type tgt-key retriever]
;;     ))

;; ;; TODO : should this not be a a macro - use proper syntax...
;; (defn make-proxy-getter [input-type output-type property-name]
;;   (.warn  *logger* "make-proxy-getter - DEPRECATED")
;;   (let [method (symbol (make-getter-name property-name))]
;;     (eval (list 'proxy '[Getter] '[] (list 'get '[bean] (list '. 'bean method))))
;;     ))

;; (defn make-getter-map [tgt-class fields]
;;   (let [types (map first fields)
;; 	names (map second fields)
;; 	getters (map (fn [type name] (make-proxy-getter tgt-class type name)) types (map name names))]
;;     (apply array-map (interleave names getters))))

;; (defn make-fields [src-type-map src-getter-map attrs]
;;   (map (fn [attr]
;; 	   (let [src-key (first attr)
;; 		 src-type (src-type-map src-key)
;; 		 src-getter (src-getter-map src-key)]
;; 	     (apply expand-property src-type src-getter attr)))
;;        attrs))

;; ;; TODO
;; ;; allow selection from a number of src-models
;; ;; allow selection into a number of src views
;; ;; allow splitting :split <split-fn> implemented by router - should provide fn for tgt-view construction...
;; ;; abstract out tgt-view construction so it can be done from parameters, during select, or on-demand from router...

;; (defn select [#^Model src-model #^Keyword src-key-key #^Keyword src-version-key #^ISeq attrs & pvec]
;;   (let [pmap (apply array-map pvec)
;; 	src-metadata (. src-model getMetadata)
;; 	src-keys (map keyword (. src-metadata getAttributeNames))
;; 	src-types (. src-metadata getAttributeTypes)
;; 	src-getters (. src-metadata getAttributeGetters)
;; 	src-type-map (apply array-map (interleave src-keys src-types)) ; key:type
;; 	src-getter-map (apply array-map (interleave src-keys src-getters)) ; key:getter
;; 	fields (make-fields src-type-map src-getter-map attrs) ; selection ([type name ...])
;; 	;; test to see if transform is needed should be done somewhere here...
;; 	;; what is an :into param was given...none of this needs calculating...
;; 	tgt-class-name (or (pmap :class) (name (gensym "org.dada.tmp.OutputValue")))
;; 	tgt-model-name (or (pmap :model) (name (gensym "OutputModel")))
;; 	filter-fn (pmap :filter)
;;    	tgt-types (map (fn [field] (nth field 0)) fields)
;;    	tgt-keys (map (fn [field] (nth field 1)) fields)
;;    	tgt-names (map name tgt-keys)
;;    	sel-getters (map (fn [field] (nth field 2)) fields)
;; 	tgt-attrs (interleave tgt-keys tgt-types)
;;   	tgt-class (apply make-class tgt-class-name Object tgt-attrs)
;; 	tgt-creator (proxy [Creator] [] (create [& args] (apply make-instance tgt-class args)))
;;    	tgt-getter-map (make-getter-map tgt-class fields) ; name:getter
;; 	tgt-getters (vals tgt-getter-map)
;;    	tgt-key-getter (tgt-getter-map src-key-key)
;;    	tgt-version-getter (tgt-getter-map src-version-key)
;; 	tgt-metadata (new GetterMetadata  tgt-creator  (collection tgt-key-getter tgt-version-getter) tgt-types tgt-names tgt-getters)
;; 	view (VersionedModelView. tgt-model-name tgt-metadata tgt-key-getter tgt-version-getter)
;; 	transformer (make-transformer-old sel-getters view tgt-class)
;; 	filter (make-filter filter-fn transformer)
;; 	]
;;     (connect src-model filter)
;;     view)
;;   )
