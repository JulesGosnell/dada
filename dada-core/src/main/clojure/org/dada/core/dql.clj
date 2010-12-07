(ns 
 org.dada.core.dql
 (:use [org.dada.core])
 (:use clojure.contrib.logging)
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
	   Metadata$Comparator
	   MetaResult
	   Model
	   PivotModel
	   Reducer
	   Reducer$Strategy
	   Result
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

(def #^Metadata result-metadata
     (custom-metadata3 org.dada.core.Result
		       [:model]		;TODO - this is a BAD idea for a PK
		       [:model]		;TODO - we need a proper version - or code that can handle versionless data
		       (proxy [Metadata$Comparator][](higher [lhs rhs] true))
		       [[:model org.dada.core.Model false]
			[:prefix String false]
			[:pairs java.util.Collection false]
			[:operation java.util.Collection false]]))

;;--------------------------------------------------------------------------------
;; needs refactoring from here...

(defn apply-getters [#^ISeq getters value]
  "apply a list of getters to a value returning a list of their results"
  (map (fn [#^Getter getter] (.get getter value)) getters))

(def #^Metadata$Comparator int-version-comparator
     (proxy [Metadata$Comparator][]
	    (higher [old new] (> (.getVersion new) (.getVersion old)))))

;;----------------------------------------
;; Filtration - should be implemented as a thin wrapper around Splitter
;;----------------------------------------

;; ;;----------------------------------------
;; ;; transformation
;; ;; selection of a subset of attributes from one type into another of a different shape
;; ;; TODO: creation of extra literal and composed values
;; ;;----------------------------------------

;; (defn make-transformer [#^ISeq init-fns #^Metadata metadata #^View view]
;;   (let [#^Creator creator (.getCreator metadata)]
;;     (new
;;      Transformer
;;      #^Collection
;;      (list view)
;;      #^Transformer$StatelessStrategy
;;      (proxy
;;       [Transformer$StatelessStrategy]
;;       []
;;       (transform 
;;        [input]
;;        ;; TODO: this code needs to be FAST - executed online
;;        (.create creator (into-array Object (map (fn [#^IFn init-fn] (init-fn input)) init-fns))))
;;       ))))

;; ;; returns [key type init-fn]
;; (defmulti do-transform-attribute (fn [descrip md] (class descrip)))

;; ;; simple one-to-one mapping
;; ;; key -> [key type fn]
;; (defmethod do-transform-attribute 
;;   clojure.lang.Keyword [#^Keyword key #^Metadata md]
;;   (let [#^Attribute attribute (.getAttribute md key)
;; 	type (.getType attribute)
;; 	getter (.getGetter attribute)
;; 	value-fn (fn [value] (.get getter value))]
;;     [key type value-fn]))

;; ;; synthetic attribute
;; ;; [key type keys fn] -> [key type fn]
;; (defmethod do-transform-attribute
;;   clojure.lang.PersistentList [attribute #^Metadata md]
;;   (let [[key type keys transform-fn] attribute
;; 	getters (map #(.getGetter (.getAttribute md %)) keys)
;; 	product-fn (fn [value] (map (fn [#^Getter getter] (.get getter value)) getters))
;; 	init-fn (fn [value] (apply transform-fn (product-fn value)))]
;;     (list key type init-fn)))

;; (defn third [s] (nth s 2))

;; (defn do-transform [#^String suffix #^Model src-model & #^Collection attribute-descrips]
;;   (let [#^Metadata model-metadata (.getMetadata src-model)
;; 	attribute-details (map #(do-transform-attribute % model-metadata) attribute-descrips)
;; 	attribute-keys (map first attribute-details)
;; 	attribute-types (map second attribute-details)
;; 	init-fns (map third attribute-details)
;; 	attributes (interleave attribute-keys attribute-types)
;; 	class-name (name (gensym "org.dada.core.Transform"))
;; 	superclass Object
;; 	view-metadata (custom-metadata [(first attribute-keys)] class-name superclass attributes)
;; 	view-name (str (.getName src-model) "." suffix)
;; 	view (model view-name view-metadata)
;; 	transformer (make-transformer init-fns view-metadata view)]
;;     (connect src-model transformer)
;;     view))

;;----------------------------------------
;; splitting
;; split model into smaller models based on content of one attribute of each value
;; TODO: accept ready-made table, split on more than one attribute
;;----------------------------------------

(defn make-splitter
  [#^IFn src-name-fn #^Model src-model key #^IFn value-to-keys #^IFn key-to-value #^IFn view-hook]
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
  [#^Collection primary-keys sum-key #^Collection extra-attribute-specs]
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

(defn make-sum-reducer-strategy [#^Metadata src-metadata #^Metadata tgt-metadata sum-key]
  (let [getter (.getGetter (.getAttribute src-metadata sum-key))
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
	    ;;(trace "SUM:" currentValue delta)
	    (+ currentValue delta))
     )))

(defn do-reduce-sum
  [#^String src-name #^Metadata src-metadata #^Metadata tgt-metadata sum-key #^Collection extra-values]
  (reducer src-name src-metadata tgt-metadata sum-key extra-values make-sum-reducer-strategy sum-value-key))

;; count specific stuff - should be in its own file

(defn count-value-key [count-key]
  (str "count(" (or count-key "*")  ")"))

(defn #^Metadata count-reducer-metadata
  [#^Collection primary-keys count-key #^Collection extra-attribute-specs]
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

(defn make-count-reducer-strategy [#^Metadata src-metadata #^Metadata tgt-metadata & [count-key]]
  ;; TODO - use count-key
  (let [creator (.getCreator tgt-metadata)]
    (proxy
     [Reducer$Strategy]
     []
     (initialValue [] 0)
     (initialType [type] Integer)
     (currentValue [extra-values & values] ;TODO - should not need (into-array)
		   (trace (str "COUNT " extra-values " " values))
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

;; (defn dtransform [#^Model src-model #^String suffix #^Keyword key-key #^Keyword version-key & #^Collection attribute-descrips]
;;   ;; TODO: accept ready-made View ?
;;   ;; TODO: default key/version from src-model
;;   (insert *metamodel* (apply do-transform suffix src-model key-key version-key attribute-descrips)))

;;--------------------------------------------------------------------------------
;; query operators...

;; a query returns a tuple of two functions [metadata-fn direct-fn] :
;; metadata-fn - returns upstream metadata - returns a tuple of [metadata metaprefix extra-keys ...]
;; data-fn -  returns a tuple of [metamodel prefix extra-pairs...]
;;--------------------------------------------------------------------------------

(defn metamodel [#^Model src-model]
  (let [prefix (.getName src-model)
	metaprefix (str "Meta-" prefix)]
    [ ;; metadata
     (fn [] (MetaResult. result-metadata metaprefix '() [:from] (.getMetadata src-model)))
     ;; data
     (fn []
	 (let [metamodel (model metaprefix result-metadata)]
	   (insert metamodel (Result. src-model prefix '() [:from prefix]))
	   (insert *metamodel* metamodel)
	   (Result. metamodel metaprefix '() [:from metaprefix])))]))

(defn dfrom [model-name]
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

(defn meta-view [#^String suffix #^Model src-metamodel f]
  ;; create a metamodel into which to place our results...
  (let [tgt-metamodel (model (str (.getName src-metamodel) suffix) (.getMetadata src-metamodel))]
    ;; register it with the global metamodel
    (insert *metamodel* tgt-metamodel)
    ;; view upstream metamodel for arrival of results
    (connect
     src-metamodel
     (proxy [View] []
	    (update [insertions alterations deletions]
		    (doall (map (fn [#^Update insertion]
				    (f tgt-metamodel (.getNewValue insertion))) insertions)))))
    tgt-metamodel))

(defn dunion [& [model-name]]
  (fn [[metadata-fn direct-fn]]
      (let [[src-metadata metaprefix extra-keys _ child-metadata] (metadata-fn)
	    tgt-metaprefix (str metaprefix ".union()")]
	[ ;; metadata
	 (fn []
	     (MetaResult. src-metadata tgt-metaprefix [] [:union] child-metadata))
	 ;; direct
	 (fn []
	     (let [[#^Model src-metamodel #^String src-metaprefix #^Collection src-metapairs] (direct-fn)
		   src-prefix (.substring src-metaprefix 5) ;TODO - strips off "Meta-" - yeugh!
		   tgt-prefix (str src-prefix ".union()")
		   tgt-model (model (or model-name tgt-prefix) child-metadata)
		   tgt-metamodel (meta-view ".union()" src-metamodel (fn [tgt-metamodel insertion] (let [[src-model _ src-pairs] insertion] (connect src-model tgt-model))))]
	       (insert *metamodel* tgt-model)
	       (insert tgt-metamodel (Result. tgt-model tgt-prefix [] [:union]))
	       (Result. tgt-metamodel tgt-metaprefix [] [:union])))])))

;; extra keys are inserted into attribute list
;; extra values are carried in model's row in metamodel
;; each split adds an extra key/value downstream that we may need to unwrap upstream
(defn dcount [& [count-key]]
  (fn [[metadata-fn data-fn]]
      (let [[#^Metadata _ metaprefix extra-keys _ src-metadata] (metadata-fn)
	    extra-attributes (map (fn [key] (.getAttribute src-metadata key)) extra-keys)
	    tgt-metadata (count-reducer-metadata extra-keys count-key extra-attributes)]
	[ ;; metadata
	 (fn []
	     (MetaResult. tgt-metadata (str metaprefix "." (count-value-key count-key)) extra-keys [:count] tgt-metadata))
	 ;; direct
	 (if data-fn
	   (fn []
	       (let [[#^Model src-metamodel prefix #^Collection extra-pairs] (data-fn)
		     new-prefix (str prefix "." (count-value-key count-key))
		     tgt-model (model new-prefix src-metadata)
		     tgt-metamodel (meta-view
				    (str "." (count-value-key count-key))
				    src-metamodel
				    (fn [tgt-metamodel [#^Model src-model _ extra-pairs]]
					(let [count-model (do-reduce-count 
							   (.getName src-model)
							   (.getMetadata src-model)
							   tgt-metadata
							   count-key
							   (map second extra-pairs))]
					  (insert *metamodel* count-model)
					  (insert tgt-metamodel (Result. count-model new-prefix extra-pairs [:count count-key]))
					  (connect src-model count-model))))]
		 (Result. tgt-metamodel new-prefix extra-pairs [:count]))))])))

(defn dsum [sum-key]
  (fn [[metadata-fn data-fn]]
      (let [[#^Metadata _ metaprefix extra-keys _ src-metadata] (metadata-fn)
	    extra-attributes (map (fn [key] (.getAttribute src-metadata key)) extra-keys)
	    tgt-metadata (sum-reducer-metadata extra-keys sum-key extra-attributes)]
	[ ;; metadata
	 (fn []
	     (MetaResult. tgt-metadata (str metaprefix "." (sum-value-key sum-key)) extra-keys [:sum sum-key] tgt-metadata))
	 ;; data
	 (if data-fn
	   (fn []
	       (let [[#^Model src-metamodel prefix #^Collection extra-pairs] (data-fn)
		     new-prefix (str prefix "." (sum-value-key sum-key))
		     tgt-model (model new-prefix src-metadata)
		     tgt-metamodel (meta-view
				    (str "." (sum-value-key sum-key))
				    src-metamodel
				    (fn [tgt-metamodel [#^Model src-model _ extra-pairs]]
					(let [sum-model (do-reduce-sum 
							 (.getName src-model)
							 (.getMetadata src-model)
							 tgt-metadata
							 sum-key
							 (map second extra-pairs))]
					  (insert *metamodel* sum-model)
					  (insert tgt-metamodel (Result. sum-model new-prefix extra-pairs [:sum sum-key]))
					  (connect src-model sum-model))))]
		 (Result. tgt-metamodel new-prefix extra-pairs [:sum]))))])))

(defn split-key-value [key]
  (str "split(" (or key "") ")"))

(defn attach [model update-fn]
     (connect model (proxy [View] [] (update [insertions alterations deletions] (update-fn insertions alterations deletions)))))

(defn dsplit2 [split-key split-key-fn]
  (fn [[src-metadata-fn direct-fn]]
      (let [src-metadata-tuple (src-metadata-fn)
	    [src-metadata src-metaprefix src-extra-keys _ child-metadata] src-metadata-tuple
	    label (split-key-value split-key)
	    suffix (str "." label)
	    split-metadata-tuple (MetaResult. src-metadata (str src-metaprefix suffix) (concat src-extra-keys [split-key]) '(:split) child-metadata)
	    [split-metadata split-metaprefix split-extra-keys] split-metadata-tuple
	    split-metadata-fn (fn [] split-metadata-tuple)
	    tgt-metadata-tuple split-metadata-tuple
	    [tgt-metadata tgt-metaprefix tgt-extra-keys] tgt-metadata-tuple
	    tgt-metadata-fn (fn [] tgt-metadata-tuple)
	    ]
	[		 ;; metadata
	 tgt-metadata-fn ;reverse keys
	 ;; data
	 (fn []
	     (let [src-data-tuple (direct-fn)
		   [#^Model src-metamodel src-prefix #^Collection src-extra-pairs] src-data-tuple
		   tgt-metamodel (model (str (.getName src-metamodel) suffix) (.getMetadata src-metamodel))
		   tgt-prefix (str src-prefix "." split-key)
		   tgt-extra-pairs (concat src-extra-pairs [[split-key]])
		   tgt-data-tuple (Result. tgt-metamodel tgt-prefix tgt-extra-pairs [:split])]
	       ;; register it with the global metamodel
	       (insert *metamodel* tgt-metamodel)
	       ;; view upstream metamodel for the arrival or results
	       (attach
		src-metamodel
		(fn [insertions alterations deletions]
		    (doall
		     (map
		      (fn [#^Update insertion]
			  (let [[src-model prefix split-extra-pairs operation] (.getNewValue insertion)
				chain-fn (fn [#^Model split-model split-extra-value]
					     (let [split-model-tuple (Result. split-model prefix (concat split-extra-pairs [[split-key split-extra-value]]) [:split :TODO])]
					       (insert *metamodel* split-model) ;add to global metamodel
					       (insert tgt-metamodel split-model-tuple) ;add to metamodel that has been passed downstream
					       ))]
			    ;; we have received a model from an upstream operation...
			    ;; plug src-> split -> [chain defined above]
			    (do-split src-model split-key (or split-key-fn list) identity chain-fn)))
		      insertions))))
	       tgt-data-tuple))]))
  )

(defn dsplit [split-key & [split-key-fn subchain]]
  (fn [src-tuple]
      (let [[src-metadata-fn src-data-fn] src-tuple
	    ;; thread src into outer split
	    split-tuple (thread-chain [(dsplit2 split-key (or split-key-fn list))] src-tuple)]
	(if subchain
	  (let [
		[split-metadata-fn split-data-fn] split-tuple
		split-metadata-tuple (split-metadata-fn)
		[split-metadata split-metadata-prefix split-metadata-pairs split-operation split-child-metadata] split-metadata-tuple
		;; prepare metameta results
		metameta-metadata result-metadata
		metameta-metadata-prefix (str "Meta-" split-metadata-prefix)
		metameta-metadata-pairs split-metadata-pairs ;TODO - investigate..
		] 
	    [
	     (fn []
		 ;; the metametadata/metadata is pretty much the same as the metadata coming out of the split
		 ;; we just have to alter the model name to encode the extra level of meta-ness...
		 (MetaResult.
		  split-metadata
		  metameta-metadata-prefix
		  split-metadata-pairs
		  split-operation 
		  result-metadata))
	     (fn []
		 ;; the metadata/data 
		 (let [split-data-tuple (split-data-fn)
		       [split-data-metamodel split-data-prefix split-data-pairs split-data-operation] split-data-tuple
		       tgt-metametamodel (model metameta-metadata-prefix metameta-metadata) ;TODO model-name incomplete ?
		       tgt-prefix split-data-prefix
		       tgt-extra-pairs split-data-pairs ;TODO: does not seem to be coming through correctly
		       tgt-operation split-data-operation
		       ]
		   (attach 
		    split-data-metamodel
		    (fn [insertions alterations deletions]
			(doall
			 (map
			  (fn [#^Update insertion]
			      (let [new-value  (.getNewValue insertion)
				    [#^Model split-model _ split-pairs] new-value
				    ;; I need to produce a metamodel to
				    ;; surround the single split model
				    ;; and make it look like it is a
				    ;; singleton..
				    single-data-metamodel (model (.getName split-model) result-metadata)
				    single-data-fn (fn []
						       (Result.
							single-data-metamodel
							split-data-prefix
							split-pairs
							split-data-operation))
				    ;; plug this into subchain
				    dummy (insert single-data-metamodel (.getNewValue insertion))
				    [sub-metadata-fn sub-data-fn] (thread-chain subchain [split-metadata-fn single-data-fn])
				    ]
				;; and insert resulting metamodel into metametamodel
				(insert tgt-metametamodel (sub-data-fn))
				)
			      )
			  insertions))
			))
		   ;; return data-tuple
		   (Result. tgt-metametamodel tgt-prefix tgt-extra-pairs tgt-operation)))
	     ])
	  ;; else - return simple split results directly...
	  split-tuple))))

(defn != [lhs rhs] (not (= lhs rhs)))

(defn dfilter
  ([key predicate required]
   (let [yes (list required) no '()]
     (dsplit key (fn [candidate] (if (predicate candidate required) yes no)))))
  ([key required]
   (dfilter key = required)))

;; pivot fn must supply such a closed list of values to be used as
;; attribute metadata for output model....
(defn pivot-metadata [#^Metadata src-metadata #^Collection primary-keys #^Collection pivot-values value-key]
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

(defn dpivot [pivot-key pivot-values value-key]
  (fn [[metadata-fn direct-fn]]
      (let [[src-metadata metaprefix extra-keys] (metadata-fn)
	    extra-keys (remove #(= % pivot-key) extra-keys)
	    tgt-metadata (pivot-metadata src-metadata extra-keys pivot-values value-key)
	    tgt-name (str ".pivot(" value-key "/" pivot-key")")
	    tgt-metaprefix (str metaprefix tgt-name)]
	[ ;; metadata
	 (fn []
	     (MetaResult. result-metadata tgt-metaprefix extra-keys '(:pivot) tgt-metadata))
	 ;; direct
	 (fn []
	     (let [[#^Model src-metamodel prefix #^Collection extra-pairs] (direct-fn)
		   extra-pairs (remove #(= (first %) pivot-key) extra-pairs)
		   tgt-prefix (str prefix tgt-name)
		   tgt-model (PivotModel. 
			      tgt-prefix
			      src-metadata
			      (map second extra-pairs)
			      value-key
			      pivot-values
			      tgt-metadata)
		   tgt-metamodel (meta-view pivot-key src-metamodel (fn [tgt-metamodel [src-model _ extra-values]] (connect src-model tgt-model)))]
	       (insert *metamodel* tgt-model)
	       (insert tgt-metamodel (Result. tgt-model tgt-prefix extra-pairs [:pivot]))
	       (Result. tgt-metamodel tgt-metaprefix extra-pairs [:pivot])))])
      ))

;; types

(defn date [#^Integer y #^Integer m #^Integer d] (LocalDate. y m d))
