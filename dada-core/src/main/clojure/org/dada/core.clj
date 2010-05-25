(ns org.dada.core
    (:use org.dada.core.UnionModel)
    (:import (clojure.lang DynamicClassLoader ISeq IFn Keyword)
	     (java.util
	      ArrayList
	      Collection
	      Date)
	     (java.util.concurrent
	      ConcurrentHashMap)
	     (java.beans
	      PropertyDescriptor)
	     (org.springframework.context.support ClassPathXmlApplicationContext)
	     (org.springframework.beans.factory BeanFactory)
	     (org.slf4j Logger LoggerFactory)
	     (org.dada.asm ClassFactory)
	     (org.dada.core
	      AbstractModel
	      Creator
	      Factory
	      FilteredView
	      FilteredView$Filter
	      Getter
	      LazyView
	      Metadata
	      Attribute
	      MetadataImpl
	      Model
	      UnionModel
	      Reducer
	      Reducer$Strategy
	      Splitter
	      Splitter$StatelessStrategy
	      SynchronousServiceFactory
	      ServiceFactory
	      SparseOpenLazyViewTable
	      Transformer
	      Transformer$StatelessStrategy
	      Tuple
	      Update
	      VersionedModelView
	      View
	      )
	     ;;(org.dada.demo Client)
	     ))

(defn debug [& foo]
  (println "DEBUG: " foo)
  foo)

(defn warn [& args]
  (println "WARN: " args)
  args)

(set! *warn-on-reflection* true)
 
(def #^Logger *logger* (LoggerFactory/getLogger "org.dada.core"))

;;default - no threading...
(def #^ServiceFactory *internal-view-service-factory* (new SynchronousServiceFactory))

(defn start-server [#^String name]
  (System/setProperty "server.name" name)
  (def #^ClassPathXmlApplicationContext *spring-context* (ClassPathXmlApplicationContext. "application-context.xml"))
  (def #^ServiceFactory *internal-view-service-factory* (.getBean #^BeanFactory *spring-context* "internalViewServiceFactory"))
  (.getBean #^BeanFactory *spring-context* "metaModel"))

;; (defn start-client [#^String name]
;;   (Client/main (into-array String (list name))))

(defn insert [#^View view item]
  (.update view (list (Update. nil item)) '() '())
  item)

(defn insert-n [#^View view #^ISeq items]
  (.update view (map (fn [item] (Update. nil item)) items) '() '()))

(defn update [#^View view oldValue newValue]
  (.update view '() (list (Update. oldValue newValue)) '()))

(defn delete [#^View view value]
  (.update view '() '() (list (Update. value nil))))

;; TODO: should we register before reading data or vice versa... - can we do this without a lock ?
(defn connect [#^Model model #^View view]
  (.registerView model view)
  (let [batch (map #(Update. nil %) (.getData #^AbstractModel model))] ;; TODO: getData should be on Model ?
    (if (not (empty? batch)) (.update view batch '() '())))
  view)

(defn disconnect [#^Model model #^View view]
  (.update
   view
   '()
   '()
   (map #(Update. % nil) (.deregisterView model view)))
   view)

(defn #^Collection collection [& args]
  (let [size (count args)
	array-list (ArrayList. size)]
    (if (> size 0) (.addAll array-list args))
    array-list))

(def #^ClassFactory *class-factory* (new ClassFactory))

;; e.g.
;;(make-class
;; factory
;; "org.dada.tmp.Amount"
;; :id int :version int :amount double)

(defmulti attribute-key (fn [arg] (class arg)))
(defmethod attribute-key Date [#^Date date] (attribute-key (str "attribute" date)))
(defmethod attribute-key clojure.lang.Keyword [#^Keyword keyword] (attribute-key (name keyword)))

(def string-replacements {
     "_" "_underscore_"
     " " "_space_"
     ":" "_colon_"
     "*" "_asterisk_"
     "+" "_plus_"
     "-" "_minus_"
     "/" "_divide_"
     "(" "_openroundbracket_"
     ")" "_closeroundbracket_"
     })

(defmethod attribute-key String [#^String string] 
  (reduce (fn [#^String string [#^String key #^String value]] (.replace string key value))
	  string
	  string-replacements))

(defn attribute-array [& attribute-key-types]
  (if (empty? attribute-key-types)
    nil
    (into-array (map 
		 (fn [[key #^Class type]]
		     (into-array (list (.getCanonicalName type) (attribute-key key))))
		 (apply array-map attribute-key-types)))))

(def *classloader* (deref clojure.lang.Compiler/LOADER))

(defn #^DynamicClassLoader classloader []
  (try
   (deref clojure.lang.Compiler/LOADER)
   (catch IllegalStateException _ (warn "could not retrieve ClassLoader") *classloader*)))

(defn make-class [#^String class-name #^Class superclass #^ISeq & attribute-key-types]
  (.
   (classloader)
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

(defn getter [#^Class input-type #^Class output-type key]
  "return a Getter taking input-type returning output-type and calling get<Key>"
  (getter-2 input-type output-type (make-getter-name (attribute-key key))))

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
		  ;;(println "CREATOR" class (map identity args))
		  (apply make-instance class args))))

(defn #^Metadata metadata [#^Class class keys attribute-specs]
  "make Metadata for a given class"
  (new MetadataImpl
       (creator class)
       keys
       (map
	(fn [[key type mutable]] (Attribute. key type mutable (getter class type key)))
	attribute-specs)))

  (defn #^Metadata class-metadata2
    "create metadata for a Model containing instances of a Class"
    [#^String class-name #^Class superclass #^Collection keys #^Collection attributes]
    (let [class-attributes (mapcat (fn [[key type _]] [key type]) attributes)]
      (metadata (apply make-class class-name superclass class-attributes) keys attributes)))

(let [class-metadata-cache (atom {})]

  (defn #^Metadata class-metadata
    "create metadata for a Model containing instances of a Class"
    [#^String class-name #^Class superclass #^Collection keys #^Collection attributes]
    (let [cache-key [superclass keys attributes]]
      ((swap!
	class-metadata-cache 
	(fn [cache key]
	    (if (contains? cache key) cache (assoc cache key (class-metadata2 class-name superclass keys attributes))))
	cache-key)
       cache-key)))

  )

(defn #^Metadata seq-metadata [length]
  (new MetadataImpl
       (proxy [Creator] [] (create [args] (apply collection args)))
       [0]
       (apply 
	collection
	(map
	 (fn [i] (Attribute. i Object (= i 0) (proxy [Getter] [] (get [s] (nth s i)))))
	 (range length)))))

(defn model [#^String model-name version-key #^Metadata metadata]
  (let [version-fn (if version-key
		     (let [version-getter (.getAttributeGetter metadata version-key)]
		       (fn [old new] (> (.get version-getter new) (.get version-getter old))))
		     (fn [_ new] new))]
    (UnionModel. model-name metadata version-fn))) ;; version-fn should be retrieved from metadata

;; this should really be collapsed into (model) above - but arity overloading is not sufficient...
(defn clone-model [#^Model model #^String name]
  (let [metadata (.getMetadata model)
	keys (.getKeyAttributeKeys metadata)
	key-getter (.getAttributeGetter metadata (first keys))
	version-getter (.getAttributeGetter metadata (second keys))]
    (UnionModel.
     name
     metadata
     (fn [old new] (> (.get version-getter new) (.get version-getter old))))))

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
	getters (map #(.getAttributeGetter metadata %) keys)
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
   #^Collection
   (list view)
   #^Transformer$StatelessStrategy
   (proxy
    [Transformer$StatelessStrategy]
    []
    (transform 
     [input]
     ;; TODO: this code needs to be FAST - executed online
     (.create metadata #^Collection (map (fn [#^IFn init-fn] (init-fn input)) init-fns)))
    )))

;; returns [key type init-fn]
(defmulti do-transform-attribute (fn [descrip md] (class descrip)))

;; simple one-to-one mapping
;; key -> [key type fn]
(defmethod do-transform-attribute 
  clojure.lang.Keyword [#^Keyword key #^Metadata md]
  (let [type(.getAttributeType md key)
	getter (.getAttributeGetter md key)
	value-fn (fn [value] (.get getter value))]
    [key type value-fn]))

;; synthetic attribute
;; [key type keys fn] -> [key type fn]
(defmethod do-transform-attribute
  clojure.lang.PersistentList [attribute #^Metadata md]
  (let [[key type keys transform-fn] attribute
	getters (map #(.getAttributeGetter md %) keys)
	product-fn (fn [value] (map (fn [#^Getter getter] (.get getter value)) getters))
	init-fn (fn [value] (apply transform-fn (product-fn value)))]
    (list key type init-fn)))

(defn third [s] (nth s 2))

(defn do-transform [#^String suffix #^Model src-model & #^Collection attribute-descrips]
  (let [#^Metadata model-metadata (.getMetadata src-model)
	attribute-details (map #(do-transform-attribute % model-metadata) attribute-descrips)
	attribute-keys (map first attribute-details)
	attribute-types (map second attribute-details)
	init-fns (map third attribute-details)
	attributes (interleave attribute-keys attribute-types)
	class-name (name (gensym "org.dada.core.Transform"))
	superclass Object
	view-metadata (class-metadata [(first attribute-keys)] class-name superclass attributes)
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
  [#^IFn src-name-fn #^Model src-model key #^IFn value-to-keys #^IFn key-to-value #^IFn view-hook]
  (let [src-metadata (.getMetadata src-model)
	src-keys (.getAttributeKeys src-metadata)
	src-key  (nth src-keys 0)	;TODO
	src-version (nth src-keys 1)	;TODO
	mutable (.getMutable (.getAttribute src-metadata key))
	map (new ConcurrentHashMap)
	view-factory (proxy
		      [Factory]
		      []
		      (create [key]
			      (let [value (key-to-value key)
				    view (model (src-name-fn value) src-version src-metadata)]
				(.decouple
				 #^ServiceFactory *internal-view-service-factory*
				 (view-hook view value))
				view)
			      ))
	lazy-factory (proxy [Factory] [] (create [key] (new LazyView map key view-factory)))
	table (new SparseOpenLazyViewTable map lazy-factory)
	getter (.getAttributeGetter src-metadata key)]
    (new
     Splitter
     (proxy
      [Splitter$StatelessStrategy]
      []
      (getMutable [] mutable)
      (getKeys [value] (value-to-keys (.get getter value)))
      (getViews [key] (list (. table get key)))
      ))))
    
(defn do-split
  [#^Model src-model #^Keyword key #^IFn value-to-keys #^IFn key-to-value #^IFn view-hook]
  (connect src-model (make-splitter
		      (fn [value] (str (.getName src-model) "." "split(" key "=" value")"))
		      src-model
		      key value-to-keys key-to-value view-hook))
  )

;;----------------------------------------
;; reduction
;; reduce all values in a model to a single one - by applying a fn - e.g. sum, count, etc...
;; TODO: count, average (sum/count), mean, mode, minimum, maximum - min/max more tricky, need to carry state
;;----------------------------------------

(defn reducer
  [#^String src-name #^Metadata src-metadata #^Metadata tgt-metadata reduction-key #^Collection extra-values #^IFn strategy-fn #^IFn value-key-fn]
  (let [strategy (strategy-fn src-metadata tgt-metadata reduction-key)]
    (Reducer. (str src-name "." (value-key-fn reduction-key))
	      tgt-metadata extra-values strategy)))

;; sum specific stuff - should be in its own file

(defn sum-value-key [key]
  (str "sum(" key ")"))
  
(defn #^Metadata sum-reducer-metadata
  [#^Collection keys sum-key #^Collection extra-attribute-specs]
  (class-metadata 
   (name (gensym "org.dada.core.reducer.Sum"))
   Object
   keys
   (concat
    extra-attribute-specs
    [[:version Integer true]
     [(keyword (sum-value-key sum-key)) Number true]]))
  )

(defn make-sum-reducer-strategy [#^Metadata src-metadata #^Metadata tgt-metadata sum-key]
  (let [getter (.getAttributeGetter src-metadata sum-key)
	accessor (fn [value] (.get getter value))
	new-value (fn [#^Update update] (accessor (.getNewValue update)))
	old-value (fn [#^Update update] (accessor (.getOldValue update)))
	creator (.getCreator tgt-metadata)]
    (proxy
     [Reducer$Strategy]
     []
     (initialValue [] 0)
     (initialType [type] type)
     (currentValue [keys & args] (.create creator (into-array Object (concat keys args))))
     (reduce [insertions alterations deletions]
	     (-
	      (+
	       (reduce #(+ %1 (or (new-value %2) 0)) 0 insertions)
	       (reduce #(+ %1 (- (or (new-value %2) 0) (or (old-value %2) 0))) 0 alterations))
	      (reduce #(+ %1 (or (old-value %2))) 0 deletions))
	     )
     (apply [currentValue delta]
	    ;;(println "SUM:" currentValue delta)
	    (+ currentValue delta))
     )))

(defn do-reduce-sum
  [#^String src-name #^Metadata src-metadata #^Metadata tgt-metadata sum-key #^Collection extra-values]
  (reducer src-name src-metadata tgt-metadata sum-key extra-values make-sum-reducer-strategy sum-value-key))

;; count specific stuff - should be in its own file

(defn count-value-key [count-key]
  (str "count(" (or count-key "*")  ")"))

(defn #^Metadata count-reducer-metadata
  [#^Collection keys count-key #^Collection extra-attribute-specs]
  (class-metadata
   (name (gensym "org.dada.core.reducer.Count"))
   Object
   keys
   (concat
    extra-attribute-specs
    [[:version Integer true]
     [(keyword (count-value-key count-key)) Number true]])))

(defn make-count-reducer-strategy [#^Metadata src-metadata #^Metadata tgt-metadata & [count-key]]
  ;; TODO - use count-key
  (let [creator (.getCreator tgt-metadata)]
    (proxy
     [Reducer$Strategy]
     []
     (initialValue [] 0)
     (initialType [type] Integer)
     (currentValue [extra-values & values] ;TODO - should not need (into-array)
		   (.create creator (into-array Object (concat extra-values values))))
     (reduce [insertions alterations deletions] (- (count insertions) (count deletions)))
     (apply [currentValue delta] (+ currentValue delta))
     )
    ))

(defn do-reduce-count
  [#^String src-name #^Metadata src-metadata #^Metadata tgt-metadata count-key #^Collection extra-values]
  (reducer src-name src-metadata tgt-metadata count-key extra-values make-count-reducer-strategy count-value-key))

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
;;     [Transformer$StatelessStrategy]
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
;; 	src-keys (map keyword (. src-metadata getAttributeKeys))
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