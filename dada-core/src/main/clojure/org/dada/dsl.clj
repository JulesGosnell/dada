(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.dsl
 (:use [org.dada.core])
 (:use clojure.tools.logging)
 (:use org.dada.core.PivotModel)
 (:import [clojure.lang
	   ]
	  [java.math
	   ]
	  [java.util
	   Collection
	   ]
	  [java.util.concurrent
	   ConcurrentHashMap]
	  [org.dada.core
	   Attribute
	   Creator
	   Factory
	   Getter
	   LazyView
	   Metadata
	   Metadata$VersionComparator
	   Model
	   PivotModel
	   Reducer
	   Reducer$Strategy
	   SparseOpenLazyViewTable
	   Splitter
	   Splitter$StatelessStrategy
	   Transformer
	   Transformer$StatelessStrategy
	   Update
	   View
	   ]
	  [org.joda.time
	   LocalDate])
 )

;;--------------------------------------------------------------------------------
;; needs refactoring from here...

(defn apply-getters [getters value]
  "apply a list of getters to a value returning a list of their results"
  (map (fn [^Getter getter] (.get getter value)) getters))

(def ^Metadata$VersionComparator int-version-comparator
     (proxy [Metadata$VersionComparator][]
	    (higher [old new] (- (.getVersion old) (.getVersion new)))))

;;----------------------------------------
;; Filtration - should be implemented as a thin wrapper around Splitter
;;----------------------------------------

;;----------------------------------------
;; transformation
;; selection of a subset of attributes from one type into another of a different shape
;; TODO: creation of extra literal and composed values
;;----------------------------------------

(defn make-transformer [init-fns ^Metadata metadata ^View view]
  (let [^Creator creator (.getCreator metadata)]
    (new
     Transformer
     ^Collection
     (list view)
     ^Transformer$StatelessStrategy
     (proxy
      [Transformer$StatelessStrategy]
      []
      (transform 
       [input]
       ;; TODO: this code needs to be FAST - executed online
       (.create creator (into-array Object (map (fn [init-fn] (init-fn input)) init-fns))))
      ))))

;; returns [key type init-fn]
(defmulti do-transform-attribute (fn [descrip md] (class descrip)))

;; simple one-to-one mapping
;; key -> [key type fn]
(defmethod do-transform-attribute 
  clojure.lang.Keyword [key ^Metadata md]
  (let [^Attribute attribute (.getAttribute md key)
	type (.getType attribute)
	getter (.getGetter attribute)
	value-fn (fn [value] (.get getter value))]
    [key type value-fn]))

;; synthetic attribute
;; [key type keys fn] -> [key type fn]
(defmethod do-transform-attribute
  clojure.lang.PersistentList [attribute ^Metadata md]
  (let [[key type keys transform-fn] attribute
	getters (map #(.getGetter (.getAttribute md %)) keys)
	product-fn (fn [value] (map (fn [^Getter getter] (.get getter value)) getters))
	init-fn (fn [value] (apply transform-fn (product-fn value)))]
    (list key type init-fn)))

(defn third [s] (nth s 2))

(defn do-transform [^String suffix ^Model src-model & attribute-descrips]
  (let [^Metadata model-metadata (.getMetadata src-model)
	attribute-details (map #(do-transform-attribute % model-metadata) attribute-descrips)
	attribute-keys (map first attribute-details)
	attribute-types (map second attribute-details)
	init-fns (map third attribute-details)
	attributes (interleave attribute-keys attribute-types)
	class-name (name (gensym "org.dada.core.Transform"))
	superclass Object
	view-metadata (custom-metadata [(first attribute-keys)] class-name superclass attributes)
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
  [src-name-fn ^Model src-model key value-to-keys key-to-value view-hook]
  (let [src-metadata (.getMetadata src-model)
	mutable (.getMutable (.getAttribute src-metadata key))
	map (new ConcurrentHashMap)
	view-factory (proxy
		      [Factory]
		      []
		      (create [key]
			      (let [value (key-to-value key)
				    view (model (src-name-fn value) src-metadata)]
				(.decouple
				 *internal-view-service-factory*
				 (view-hook view value))
				view)
			      ))
	lazy-factory (proxy [Factory] [] (create [key] (new LazyView map key view-factory)))
	table (new SparseOpenLazyViewTable map lazy-factory)
	getter (.getGetter (.getAttribute src-metadata key))]
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
  [^Model src-model key value-to-keys key-to-value view-hook]
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
  [^String src-name ^Metadata src-metadata ^Metadata tgt-metadata reduction-key ^Collection extra-values strategy-fn value-key-fn]
  (let [strategy (strategy-fn src-metadata tgt-metadata reduction-key)]
    (Reducer. (str src-name "." (value-key-fn reduction-key))
	      tgt-metadata extra-values strategy)))

;; sum specific stuff - should be in its own file

(defn sum-value-key [key]
  (str "sum(" key ")"))

(defn ^Metadata sum-reducer-metadata
  [^Collection primary-keys sum-key ^Collection extra-attribute-specs]
  (custom-metadata 
   (name (gensym "org.dada.core.reducer.Sum"))
   Object
   primary-keys
   [:version]
   int-version-comparator
   (concat
    extra-attribute-specs
    [[:version Integer true]
     [(keyword (sum-value-key sum-key)) Number true]]))
  )

(defn make-sum-reducer-strategy [^Metadata src-metadata ^Metadata tgt-metadata sum-key]
  (let [getter (.getGetter (.getAttribute src-metadata sum-key))
	accessor (fn [value] (.get getter value))
	new-value (fn [^Update update] (accessor (.getNewValue update)))
	old-value (fn [^Update update] (accessor (.getOldValue update)))
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
	    ;;(info (str "SUM: " currentValue delta))
	    (+ currentValue delta))
     )))

(defn do-reduce-sum
  [^String src-name ^Metadata src-metadata ^Metadata tgt-metadata sum-key ^Collection extra-values]
  (reducer src-name src-metadata tgt-metadata sum-key extra-values make-sum-reducer-strategy sum-value-key))

;; count specific stuff - should be in its own file

(defn count-value-key [count-key]
  (str "count(" (or count-key "*")  ")"))

(defn ^Metadata count-reducer-metadata
  [^Collection primary-keys count-key ^Collection extra-attribute-specs]
  (custom-metadata
   (name (gensym "org.dada.core.reducer.Count"))
   Object
   primary-keys
   [:version]
   int-version-comparator
   (concat
    extra-attribute-specs
    [[:version Integer true]
     [(keyword (count-value-key count-key)) Number true]])))

(defn make-count-reducer-strategy [^Metadata src-metadata ^Metadata tgt-metadata & [count-key]]
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
  [^String src-name ^Metadata src-metadata ^Metadata tgt-metadata count-key ^Collection extra-values]
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
;; (defn expand-property [^Class src-type ^Getter src-getter src-key & pvec]
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
;;   (let [method (symbol (custom-getter-name property-name))]
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

;; (defn select [^Model src-model src-key-key src-version-key attrs & pvec]
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
;;   	tgt-class (apply custom-class tgt-class-name Object tgt-attrs)
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

;;--------------------------------------------------------------------------------
;; operations are:
;;  transform
;;  index-with-version
;;  filter
;;  split (related to filter)
;;  reduce(sum, count, ?as-percentage-within-range?[, min. max. avg, mean, median, mode, etc)
;;  collect/union
;;  rotate/pivot
;;  [intersect]
;;  [complement]

;; versioned-index is actually an efficient split (by pk), reduce (all
;; versions into latest), union all latest versions back into another
;; set...

;;----------------------------------------
;; still to refactor - also sum()

(defn dtransform [^Model src-model ^String suffix key-key version-key & attribute-descrips]
  ;; TODO: accept ready-made View ?
  ;; TODO: default key/version from src-model
  (insert *metamodel* (apply do-transform suffix src-model key-key version-key attribute-descrips)))

;;--------------------------------------------------------------------------------
;; query operators...

;; a query returns a tuple of two functions [metadata-fn direct-fn] :
;; metadata-fn - returns upstream metadata - returns a tuple of [metadata metaprefix extra-keys ...]
;; data-fn -  returns a tuple of [metamodel prefix extra-pairs...]
;;--------------------------------------------------------------------------------

(defn metamodel [^Model src-model]
  (let [prefix (.getName src-model)
	metaprefix (str "Meta-" prefix)]
    [ ;; metadata
     (fn [] [(.getMetadata src-model) metaprefix '()])
     ;; data
     (fn []
	 (let [metamodel (model metaprefix (seq-metadata 2))]
	   (insert metamodel [src-model '()])
	   (insert *metamodel* metamodel)
	   [metamodel prefix '()]))]))

(defn from [model-name]
  (metamodel (.find *metamodel* model-name)))

(defn dochain 
  "forces the aggressive evaluation of a chain"
  [[metadata-fn direct-fn]]
  (let [metadata (metadata-fn)
	direct (direct-fn)]
    [ ;; metadata
     (fn [] metadata)
     ;; direct
     (fn [] direct)
     ]))

(defn thread-chain
  ([chain model]
   (reduce (fn [results operator] (operator results)) model (reverse chain)))
  ([chain]
   (reduce (fn [results operator] (operator results)) (reverse chain))))

(defn ? [& chain]
  (dochain (thread-chain chain)))

(defn meta-view [^String suffix ^Model src-metamodel f]
  ;; create a metamodel into which t place our results...
  (let [tgt-metamodel (model (str (.getName src-metamodel) suffix) (.getMetadata src-metamodel))]
    ;; register it with the global metamodel
    (insert *metamodel* tgt-metamodel)
    ;; view upstream metamodel for arrival of results
    (connect
     src-metamodel
     (proxy [View] []
	    (update [insertions alterations deletions]
		    (doall (map (fn [^Update insertion]
				    (trace (str "INSERTION " (.getNewValue insertion)))
				    (apply f tgt-metamodel (.getNewValue insertion))) insertions)))))
    tgt-metamodel))

(defn union [& [model-name]]
  (fn [[metadata-fn direct-fn]]
      (let [[src-metadata metaprefix extra-keys] (metadata-fn)]
	[ ;; metadata
	 (fn []
	     [src-metadata (str metaprefix ".union()") extra-keys])
	 ;; direct
	 (fn []
	     (let [[^Model src-metamodel prefix ^Collection extra-pairs] (direct-fn)
		   tgt-model (model (or model-name (str prefix ".union()")) src-metadata)
		   tgt-metamodel (meta-view ".union()" src-metamodel (fn [tgt-metamodel src-model extra-pairs] (connect src-model tgt-model)))]
	       (insert *metamodel* tgt-model)
	       (insert tgt-metamodel [tgt-model extra-pairs])
	       [tgt-metamodel (str prefix ".union()") extra-pairs :union]))])))

;; extra keys are inserted into attribute list
;; extra values are carried in model's row in metamodel
;; each split adds an extra key/value downstream that we may need to unwrap upstream
(defn ccount [& [count-key]]
  (fn [[metadata-fn data-fn]]
      (let [[^Metadata src-metadata metaprefix extra-keys] (metadata-fn)
	    dummy (trace (str "COUNT METADATA " metaprefix " " extra-keys))
	    extra-attributes (map (fn [key] (.getAttribute src-metadata key)) extra-keys)
	    tgt-metadata (count-reducer-metadata extra-keys count-key extra-attributes)]
	[ ;; metadata
	 (fn []
	     [tgt-metadata (str metaprefix "." (count-value-key count-key)) extra-keys])
	 ;; direct
	 (if data-fn
	   (fn []
	       (let [[^Model src-metamodel prefix ^Collection extra-pairs] (data-fn)
		     dummy (trace (str "COUNT extra-pairs [0]: " extra-pairs))
		     new-prefix (str prefix "." (count-value-key count-key))
		     tgt-model (model new-prefix src-metadata)
		     tgt-metamodel (meta-view
				    (str "." (count-value-key count-key))
				    src-metamodel
				    (fn [tgt-metamodel ^Model src-model extra-pairs]
					(trace (str "COUNT extra-pairs [1]: " extra-pairs))
					(let [count-model (do-reduce-count 
							   (.getName src-model)
							   (.getMetadata src-model)
							   tgt-metadata
							   count-key
							   (map second extra-pairs))]
					  (insert *metamodel* count-model)
					  (insert tgt-metamodel [count-model extra-pairs])
					  (connect src-model count-model))))]
		 [tgt-metamodel new-prefix extra-pairs :count])))])))

(defn sum [sum-key]
  (fn [[metadata-fn data-fn]]
      (let [[^Metadata src-metadata metaprefix extra-keys] (metadata-fn)
	    dummy (trace (str "SUM METADATA " metaprefix " " extra-keys))
	    extra-attributes (map (fn [key] (.getAttribute src-metadata key)) extra-keys)
	    tgt-metadata (sum-reducer-metadata extra-keys sum-key extra-attributes)]
	[ ;; metadata
	 (fn []
	     [tgt-metadata (str metaprefix "." (sum-value-key sum-key)) extra-keys])
	 ;; direct
	 (if data-fn
	   (fn []
	       (let [[^Model src-metamodel prefix ^Collection extra-pairs] (data-fn)
		     dummy (trace (str "SUM extra-pairs [0]: " extra-pairs))
		     new-prefix (str prefix "." (sum-value-key sum-key))
		     tgt-model (model new-prefix src-metadata)
		     tgt-metamodel (meta-view
				    (str "." (sum-value-key sum-key))
				    src-metamodel
				    (fn [tgt-metamodel ^Model src-model extra-pairs]
					(trace (str "SUM extra-pairs [1]: " extra-pairs))
					(let [sum-model (do-reduce-sum 
							 (.getName src-model)
							 (.getMetadata src-model)
							 tgt-metadata
							 sum-key
							 (map second extra-pairs))]
					  (insert *metamodel* sum-model)
					  (insert tgt-metamodel [sum-model extra-pairs])
					  (connect src-model sum-model))))]
		 [tgt-metamodel new-prefix extra-pairs :sum])))])))

(defn split-key-value [key]
  (str "split(" (or key "") ")"))

(defn split [split-key & [split-key-fn subchain]]
  (fn [[src-metadata-fn direct-fn]]
      (let [src-metadata-tuple (src-metadata-fn)
	    [src-metadata src-metaprefix src-extra-keys] src-metadata-tuple
	    label (split-key-value split-key)
	    suffix (str "." label)
	    split-metadata-tuple [src-metadata (str src-metaprefix suffix) (concat src-extra-keys [split-key]) '(:split)]
	    [split-metadata split-metaprefix split-extra-keys] split-metadata-tuple
	    split-metadata-fn (fn [] split-metadata-tuple)
	    tgt-metadata-tuple (if subchain
				 (let [[sub-metadata-fn] (thread-chain subchain [split-metadata-fn nil])
				       sub-metadata-tuple (sub-metadata-fn)
				       [sub-metadata sub-metaprefix sub-extra-keys] sub-metadata-tuple]
				   (trace (str "SPLIT META SRC   " src-metadata-tuple))
				   (trace (str "SPLIT META SPLIT " split-metadata-tuple))
				   (trace (str "SPLIT META SUB   " sub-metadata-tuple))
				   sub-metadata-tuple
				   )
				 split-metadata-tuple)
	    [tgt-metadata tgt-metaprefix tgt-extra-keys] tgt-metadata-tuple
	    tgt-metadata-fn (fn [] tgt-metadata-tuple)
	    ]
	[ ;; metadata
	 tgt-metadata-fn ;reverse keys
	 ;; data
	 (fn []
	     (let [src-data-tuple (direct-fn)
		   [^Model src-metamodel src-prefix ^Collection src-extra-pairs] src-data-tuple
		   dummy (trace (str "SPLIT src-extra-pairs " src-extra-pairs))
		   tgt-metamodel (model (str (.getName src-metamodel) suffix) (.getMetadata src-metamodel))
		   tgt-prefix (str src-prefix "." split-key)
		   tgt-extra-pairs src-extra-pairs ;;(concat src-extra-pairs [[split-key "*"]])
		   dummy (trace (str "SPLIT tgt-extra-pairs " tgt-extra-pairs))
		   tgt-data-tuple [tgt-metamodel tgt-prefix tgt-extra-pairs :split]]
	       ;; register it with the global metamodel
	       (trace (str "SPLIT DATA SRC   " src-data-tuple))
	       (insert *metamodel* tgt-metamodel)
	       ;; view upstream metamodel for the arrival or results
	       (trace (str "SPLIT - watching " src-metamodel))
	       (connect
		src-metamodel
		(proxy [View] []
		       (update [insertions alterations deletions]
			       (doall
				(map
				 (fn [^Update insertion]
				     (let [[src-model src-extra-values] (.getNewValue insertion)
					   chain-fn (if subchain
					; plug split -> sub -> tgt
						      (fn [^Model split-model split-extra-value]
							  (let [split-metamodel (model (str (.getName src-metamodel) (str suffix "=" split-extra-value)) (.getMetadata src-metamodel))
								split-prefix (str tgt-prefix "=" split-extra-value)
								split-extra-pairs (concat src-extra-pairs [[split-key split-extra-value]])
								dummy (trace (str "SPLIT split-extra-pairs " split-extra-pairs))

								split-data-tuple [split-metamodel split-prefix split-extra-pairs]
								split-data-fn (fn [] split-data-tuple)
								[_ sub-data-fn] (thread-chain subchain [split-metadata-fn split-data-fn])
								sub-data-tuple (sub-data-fn)
								[sub-metamodel sub-prefix sub-extra-pairs] sub-data-tuple
								dummy (trace (str "SPLIT sub-extra-pairs " sub-extra-pairs))

								split-model-tuple [split-model sub-extra-pairs]]
							    
							    (trace (str "SPLIT DATA SPLIT " split-data-tuple))
							    (trace (str "SPLIT DATA SUB " split-data-tuple))
							    (insert *metamodel* split-metamodel) ;add to global metamodel
							    (insert *metamodel* split-model) ;add to global metamodel
							    (insert split-metamodel (doall split-model-tuple))
							    ;; collect results of calling this subchain and put them into our output metamodel
							    (connect
							     sub-metamodel
							     (proxy [View] []
								    (update [insertions alterations deletions]
									    (doall
									     (map
									      (fn [^Update insertion]
										  (let [sub-model-tuple (.getNewValue insertion)] ;; [sub-model sub-extra-pairs]
										    ;; UNCOMMENT HERE AND WORK IT OUT
										    (insert tgt-metamodel (doall sub-model-tuple))
										    ))
									      insertions)))))))
						      ;; plug split -> tgt
						      (fn [^Model split-model split-extra-value]
							  (trace (str "SPLIT - producing new model " split-model " " split-extra-value))
							  (let [split-model-tuple (list split-model (concat src-extra-values [[split-key split-extra-value]]))]
							    (insert *metamodel* split-model) ;add to global metamodel
							    (insert tgt-metamodel (doall split-model-tuple)) ;add to metamodel that has been passed downstream
							    ))
						      )]
				       ;; we have received a model from an upstream operation...
				       (trace ("SPLIT - receiving new model " src-model " " src-extra-values))
				       ;; plug src-> split -> [chain defined above]
				       (do-split src-model split-key (or split-key-fn list) identity chain-fn)))
				 insertions)))))
	       tgt-data-tuple))]))
  )

(defn != [lhs rhs] (not (= lhs rhs)))

(defn ffilter
  ([key predicate required]
   (let [yes (list required) no '()]
     (split key (fn [candidate] (if (predicate candidate required) yes no)))))
  ([key required]
   (ffilter key = required)))

;; pivot fn must supply such a closed list of values to be used as
;; attribute metadata for output model....
(defn pivot-metadata [^Metadata src-metadata ^Collection primary-keys ^Collection pivot-values value-key]
  (let [value-type (.getType (.getAttribute src-metadata value-key))]
    (custom-metadata 
     (name (gensym "org.dada.core.Pivot"))
     Object
     primary-keys
     [:version]
     int-version-comparator
     (concat
      (map #(.getAttribute src-metadata %) primary-keys)
      [[:version Integer true]]
      (map #(vector % value-type true) pivot-values)))))

;; pivot-key - e.g. :time
;; pivot-values - e.g. years
;; value-key - e.g. :count(*) - needed to find type of new columns

(defn pivot [pivot-key pivot-values value-key]
  (fn [[metadata-fn direct-fn]]
      (let [[src-metadata metaprefix extra-keys] (metadata-fn)
	    dummy (trace (str "PIVOT METADATA " pivot-key " " keys " " value-key))
	    extra-keys (remove #(= % pivot-key) extra-keys)
	    tgt-metadata (pivot-metadata src-metadata extra-keys pivot-values value-key)
	    tgt-name (str ".pivot(" value-key "/" pivot-key")")]
	[ ;; metadata
	 (fn []
	     [tgt-metadata (str metaprefix tgt-name) extra-keys '(:pivot)])
	 ;; direct
	 (fn []
	     (let [[^Model src-metamodel prefix ^Collection extra-pairs] (direct-fn)
		   dummy (trace (str "PIVOT extra-pairs before pivot: " extra-pairs))
		   extra-pairs (remove #(= (first %) pivot-key) extra-pairs)
		   dummy (trace (str "PIVOT extra-pairs after pivot:  " extra-pairs))
		   tgt-model (PivotModel. 
			      (str prefix tgt-name)
			      src-metadata
			      (map second extra-pairs)
			      value-key
			      pivot-values
			      tgt-metadata)
		   tgt-metamodel (meta-view pivot-key src-metamodel (fn [tgt-metamodel src-model extra-values] (connect src-model tgt-model)))]
	       (insert *metamodel* tgt-model)
	       (insert tgt-metamodel [tgt-model extra-pairs])
	       [tgt-metamodel (str prefix ".union()") extra-pairs]))])
      ))

;; types

(defn date [^Integer y ^Integer m ^Integer d] (LocalDate. y m d))
