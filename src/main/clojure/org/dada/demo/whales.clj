(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use [org.dada.core])
 (:import [clojure.lang
	   ])
 (:import [java.math
	   ])
 (:import [java.util
	   Date
	   NavigableSet
	   TreeSet
	   ])
 (:import [org.dada.core
	   Batcher
	   Creator
	   Metadata
	   Model
	   ])
 )

;;----------------------------------------------------------------------------------------------------------------------
;; TODO - this will have to wait until we have an extended ClassFactory

;; (do
;;   (def Cetacea (make-class factory "org.dada.whales.Cetacea" Object [(Float/TYPE) "length"] [(Float/TYPE) "weight"]))
;;   (def Mysticeti (make-class factory "org.dada.whales.Mysticeti" Cetacea))
;;   (def Balaenopteridae (make-class factory "org.dada.whales.Balaenopteridae" Mysticeti))
;;   (def Eschrichtiidae (make-class factory "org.dada.whales.Eschrichtiidae" Mysticeti))
;;   (def Neobalaenidae (make-class factory "org.dada.whales.Neobalaenidae" Mysticeti))
;;   (def Balaenidae (make-class factory "org.dada.whales.Balaenidae" Mysticeti))
;;   (def Odontoceti (make-class factory "org.dada.whales.Odontoceti" Cetacea))
;;   (def Physeteroidea (make-class factory "org.dada.whales.Physeteroidea" Odontoceti))
;;   (def Zyphiidae (make-class factory "org.dada.whales.Zyphiidae" Odontoceti))
;;   (def Delphinidae (make-class factory "org.dada.whales.Delphinidae" Odontoceti))
;;   )


;; mysticeti				; Baleen Whales
;;  balaenidae                          ; Right, Bowhead Whales
;;  balaenopteridae                     ; Rorqual Whales-Blue, Minke, Bryde's, Eden, Humpback
;;  eschrichtiidae                      ; Gray Whales
;;  neobalaenidae                       ; Pygmy Right Wale
;; odontoceti				; Toothed Whales
;;  physeteroidea			; Sperm Whales
;;  zyphiidae                           : Beaked Whales
;;  delphinidae                         ; Orca, Dolphins
;;----------------------------------------------------------------------------------------------------------------------

(def types [
	    "blue whale"
	    "sei whale"
	    "bottlenose dolphin"
	    "killer whale"
	    "false killer whale"
	    "amazon river dolphin"
	    "narwhal"
	    "sperm whale"
	    "pilot whale"
	    "beluga whale"
	    "humpback whale"
	    "fin whale"
	    "right whale"
	    "bowhead whale"
	    "brydes whale"
	    "minke whale"
	    "gray whale"
	    "spinner dolphin"
	    "melon headed whale"
	    "beaked whale"
	    ])

(def max-weight 172) ;; metric tons
(def max-length 32.9) ;; metres

(defn rnd [seq] (nth seq (rand-int (count seq))))

(if false
  (def *metamodel*
       (new org.dada.core.MetaModelImpl
	    "Cetacea"
	    (new org.dada.core.StringMetadata "type")
	    (new org.dada.core.SynchronousServiceFactory)))
  (do
    (def *metamodel* (start-server "Cetacea"))
    (start-client "Cetacea")))

(def attributes
     (list
      :id (Integer/TYPE)
      :version (Integer/TYPE)
      :time Date
      :type String
      :length (Float/TYPE)
      :weight (Float/TYPE)))

;; could try using deftype thus:
;; (deftype Cetacean [#^String key #^Integer version])
;; you can use dates as field names !
;;  user=> (def field-symbol (symbol (.toString (java.util.Date.))))
;;  #'user/field-symbol
;;  user=> (eval (list 'deftype 'Test [field-symbol]))
;;  #'user/Test
;;  user=> (def myTest (Test "foo"))
;;  #'user/myTest
;;  user=> myTest
;;  #:Test{:Wed Apr 07 08:26:46 BST 2010 "foo"}
;;  user=> (def field-key (keyword field-symbol))
;;  #'user/field-key
;;  user=> (field-key myTest)
;;  "foo"
;;  user=> 
;; but clojure does NOT define accessors only:
;;  public clojure.lang.IObj user.Test__254.withMeta(clojure.lang.IPersistentMap)
;;  public clojure.lang.IPersistentMap user.Test__254.meta()
;;  public java.lang.Object user.Test__254.valAt(java.lang.Object)
;;  public java.lang.Object user.Test__254.valAt(java.lang.Object,java.lang.Object)
;;  public clojure.lang.ILookupThunk user.Test__254.getLookupThunk(clojure.lang.Keyword)
;;  public clojure.lang.Keyword user.Test__254.getDynamicType()
;;  public java.lang.Object user.Test__254.getDynamicField(clojure.lang.Keyword,java.lang.Object)
;;  public clojure.lang.IPersistentMap user.Test__254.getExtensionMap()
;; interestingly, clojure does define the following instance fields:
;;  public final java.lang.Object user.Test__254.Wed Apr 07 08_COLON_26_COLON_46 BST 2010
;;  public final java.lang.Object user.Test__254.__meta
;;  public final java.lang.Object user.Test__254.__extmap

;; a deftyped instance has slower access times - see below -
;; getter1/test1 uses a deftyped object, whereas getter2/test2 uses a
;; ClassFactory-ed object:

;; user=> (time (dotimes [n 100000000] (getter1 test1)))
;; "Elapsed time: 8615.820346 msecs"
;; nil
;; user=> (time (dotimes [n 100000000] (getter2 test2)))
;; "Elapsed time: 5298.08904 msecs"
;; nil

(def #^Class Whale (apply make-class "org.dada.demo.whales.Whale" Object attributes))
(def #^Metadata whale-metadata (apply metadata Whale :id :version attributes))
(def #^Model whales (model "Whales" whale-metadata))

(def num-whales 1000)

(insert *metamodel* whales)

;;----------------------------------------

(defn dfilter [#^Model src-model #^String tgt-view #^Collection attr-keys #^IFn filter-fn]
  ;; TODO: accept ready-made View
  (insert *metamodel* (do-filter tgt-view src-model attr-keys filter-fn)))

(defn dtransform [#^Model src-model #^String suffix #^Keyword key-key #^Keyword version-key & #^Collection attribute-descrips]
  ;; TODO: accept ready-made View ?
  ;; TODO: default key/version from src-model
  
  (insert *metamodel* (apply do-transform suffix src-model key-key version-key attribute-descrips)))

(defn dcount 
  ([#^Model src-model key-value]
   (dcount src-model (type key-value) key-value))
  ([#^Model src-model #^Class key-type key-value]
   (dcount src-model key-type key-value (count-reducer-metadata key-type)))
  ([#^Model src-model #^Class key-type key-value #^Metadata metadata]
   (insert *metamodel* (do-reduce-count src-model key-type key-value metadata)))
  )

(defn dsum
  ([#^Model src-model #^Keyword attribute-key key-value]
   (dsum src-model attribute-key (type key-value) key-value))
  ([#^Model src-model #^Keyword attribute-key #^Class key-type key-value]
   (insert *metamodel* (do-reduce-sum src-model attribute-key key-type key-value)))
  ([#^Model src-model #^Keyword attribute-key #^Class key-type key-value #^Metadata metadata]
   (insert *metamodel* (do-reduce-sum src-model attribute-key key-type key-value metadata)))
  )

;; TODO: try to lose key-to-value and vice-versa then define dpivot...
;; TODO: should mutability be part of metadata ?
(defn dsplit
  ([#^Model src-model #^Keyword key #^boolean mutable]
   (dsplit
    src-model
    key
    mutable
    list
    identity
    (fn [#^Model model value]
	(insert *metamodel* model)
	model
	)
    ))
  ([#^Model src-model #^Keyword key #^boolean mutable #^IFn tgt-hook]
   (dsplit
    src-model
    key
    mutable
    list
    identity
    (fn [#^Model model value]
	(insert *metamodel* model)
	(tgt-hook model value)
	)
    ))
  ([#^Model src-model #^Keyword key #^boolean mutable #^IFn value-to-keys #^IFn key-to-value #^IFn tgt-hook]
   (do-split
    src-model
    key
    mutable
    value-to-keys
    key-to-value
    (fn [#^Model model value]
	(insert *metamodel* model)
	(tgt-hook model value)
	)
    ))
  )

(defn do-model [#^Model src-model #^String suffix #^Keyword key #^Keyword version]
  (connect src-model (model (str (.getName src-model) "." suffix) key version (.getMetadata  src-model))))

;; TODO
;; dindex

;;----------------------------------------

;; demonstrate filtration - only selecting a subset of the values from
;; one model into another - based on their contents.

;;----------------------------------------

;; (def median-length (/ max-length 2))
;; (def median-weight (/ max-weight 2))

;; (def longer-whales 
;;      (dfilter whales (str "length>" median-length) '(:length) #(> % median-length)))

;; (def heavier-whales
;;      (dfilter whales (str "weight>" median-weight) '(:weight) #(> % median-weight)))

;; ;;----------------------------------------
;; ;; demonstrate transformation - only selecting a subset of the
;; ;; attributes from one model into another. We may support synthetic
;; ;; attributes...
;; ;;----------------------------------------

;; (def longer-whales-weight
;;      (dtransform longer-whales "weight" :time :version :time :version :weight))

;; (def #^Model heavier-whales-length
;;      (dtransform heavier-whales "length" :time :version :time :version :length))

;; ;;----------------------------------------
;; ;; demonstrate transformation - a synthetic field - metric tons per metre
;; ;;----------------------------------------

;; (dtransform
;;  whales
;;  "tonsPerMetre"
;;  :time
;;  :version 
;;  :time
;;  :version 
;;  (list
;;   :tonsPerMetre
;;   Number
;;   '(:weight :length)
;;   (fn [weight length] (if (= length 0) 0 (/ weight length))))
;;  )

;; ;;----------------------------------------
;; ;; try a transformation on top of a filtration
;; ;; e.g. select time, version, length from whales where type="narwhal"
;; ;;----------------------------------------

;; (def narwhals-length
;;      (dtransform 
;;       (dfilter 
;;        whales
;;        "type=narwhal" 
;;        '(:type)
;;        #(= "narwhal" %))
;;       "length"
;;       :time
;;       :version
;;       :time
;;       :version
;;       :length)
;;      )

;; ;;----------------------------------------
;; ;; demonstrate reduction (sum)
;; ;;----------------------------------------

;; (dcount whales "whales")
;; (dsum whales :length "whales")
;; (dsum whales :weight "whales")
;; (dcount heavier-whales-length "heavier-whales")
;; (dsum heavier-whales-length :length "heavier-whales")
;; (dsum longer-whales-weight :weight "longer-whales")
;; (dcount longer-whales-weight "longer-whales")

;; ;;----------------------------------------
;; ;; demonstrate splitting [and more reduction] - a pivot...
;; ;;----------------------------------------

;; ;; PROBLEM - 
;; ;; tgt-model values must be same shape as intermediate model values...
;; ;; EITHER: we prearrange the shape (preferred - complex - how do do this?)
;; ;; OR: we could construct the tgt mode lazily (simpler - but concurrency issues)

;; ;; (defn dpivot [#^Model src-model #^Keyword split-key #^boolean mutable #^String tgt-model-name #^IFn tgt-model-hook
;; ;; 	      #^Keyword key-key #^Keyword version-key & attribute-key-types]
;; ;;   (let [tgt-metadata (class-metadata
;; ;; 		      (name (gensym "org.dada.demo.core.Pivot"))
;; ;; 		      Object
;; ;; 		      key-key
;; ;; 		      version-key
;; ;; 		      attribute-key-types)
;; ;; 	tgt-model (model tgt-model-name tgt-metadata)]
;; ;;     (insert *metamodel* tgt-model)
;; ;;     (dsplit
;; ;;      src-model
;; ;;      split-key
;; ;;      mutable
;; ;;      (fn [#^Model model value]
;; ;; 	 (connect (dcount model String value tgt-metadata) tgt-model)
;; ;; 	 (tgt-model-hook model value)
;; ;; 	 )
;; ;;      ))
;; ;;   )

;; (let [union-metadata (class-metadata
;; 		      "org.dada.demo.whales.TypeCount"
;; 		      Object
;; 		      :key
;; 		      :version
;; 		      [:key String :version Integer :count Integer])
;;       union-model (model "Whales.union(count(split(type)))" union-metadata)
;;       ]
;;   (insert *metamodel* union-model)
;;   (dsplit
;;    whales
;;    :type
;;    false
;;    (fn [#^Model model value]
;;        (connect (dcount model String value union-metadata) union-model)
;;        (dsum model :length value)
;;        (dsum model :weight value)
;;        model ; TODO: do we really need to be able to override the model ? should threading go here ?
;;        )
;;    ))

;; (def #^NavigableSet pivot-years
;;      (TreeSet.
;;       (list
;;        (Date. 0 0 1)
;;        (Date. 1 0 1)
;;        (Date. 2 0 1)
;;        (Date. 3 0 1)
;;        (Date. 4 0 1)
;;        (Date. 5 0 1)
;;        (Date. 6 0 1)
;;        (Date. 7 0 1)
;;        (Date. 8 0 1)
;;        (Date. 9 0 1))))

;; (def pivot-key-keys (collection "type" "version"))
;; (def pivot-keys (apply collection (concat pivot-key-keys pivot-years)))
;; (def pivot-types (apply collection String Integer (repeat 10 Date)))
;; (def pivot-names (map (fn [key](.toString key)) pivot-keys))
;; (def pivot-symbols (map (fn [name] (symbol name)) pivot-names))
;; (def pivot-keywords (map (fn [name] (keyword name)) pivot-names))

;; (eval (list 'deftype 'pivot-type (apply vector pivot-symbols)))

;; (import org.dada.core.Creator)
;; (import org.dada.core.Getter)
;; (import org.dada.core.GetterMetadata)

;; (def pivot-metadata
;;      (GetterMetadata.
;;       (proxy [Creator] [] (create [args] (apply pivot-type args)))
;;       pivot-key-keys ;; key-attribute-keys
;;       pivot-types ;; attribute-types
;;       pivot-keys
;;       (apply
;;        collection
;;        (map
;; 	(fn [keyword] (proxy [Getter] [] (get [instance] (keyword instance))))
;; 	pivot-keywords))))

;; (def survey-metadata (class-metadata
;; 		      (name (gensym "org.dada.demo.whales.Survey"))
;; 		      Object
;; 		      :time
;; 		      :version
;; 		      [:time Date :version Integer :count Integer]))

;; (def pivot-model (model (str (.getName whales) ".pivot(count(time))") pivot-metadata))
;; (insert *metamodel* pivot-model)

;; (dsplit
;;  whales
;;  :type
;;  false
;;  (fn [#^Model type-model type]
;;      (let [survey-model (model (str (.getName type-model) ".count(time)") survey-metadata)]
;;        (insert *metamodel* survey-model)
;;        ;; need some sort of transform here...'
;;        ;;(connect survey-model pivot-model)
;;        (dsplit
;; 	type-model
;; 	:time
;; 	false
;; 	(fn [time] (list (or (.lower pivot-years time) time)))
;; 	identity
;; 	(fn [#^Model time-model time]
;; 	    (connect (dcount time-model Date time survey-metadata) survey-model)
;; 	    time-model))
;;        )
;;      type-model)
;;  )

;;--------------------------------------------------------------------------------
;; lets try improving the sytax using ?monads?

;; I want to say e.g. (munion (mcount (msplit whales :type))

;; return a fn which when applied to a model, value and a chain, applies
;; itself to the model then applies the chain to this...

;; example queries:

;; summarise a table where the row is :type, column is :time and cell
;; contains number of whales of this type sighted this year...

;; (union (count (split :time

;; this won't do it...

(defn mlift [model key]
  (fn []
      [;; get metadata / key
       (fn [] [(.getMetadata model) key])
       ;; apply
       (fn [hook] (hook model key))
       ]))

(defn munion [chain tgt-key]
  (fn []
      (let [[downstream-metadata-accessor applicator] (chain)
	    [src-metadata src-key] (downstream-metadata-accessor)]
	[ ;; get metadata / key
	 (fn [] [src-metadata tgt-key])
	 ;; apply
	 (fn [src-name hook]
	     (println "UNION[1]: " src-name)
	     (let [tgt-name (str src-name ".union(" tgt-key ")")
		   tgt-model (model tgt-name src-metadata)]
	       (insert *metamodel* tgt-model)
	       (hook tgt-model tgt-key)
	       (applicator
		(fn [src-model src-key]
		    (println "UNION[2]: " src-key src-model)
		    (connect src-model tgt-model)
		    tgt-model))))
	 ])
      ))

(defn mcount [chain]
  (fn []
      (let [[downstream-metadata-accessor applicator] (chain)
	    src-key-type Object ;; TODO: problem - we don't know the src-key yet
	    tgt-metadata (count-reducer-metadata src-key-type)
	    tgt-key "count"]
	[ ;; get metadata / key
	 (fn [] [tgt-metadata tgt-key])
	 ;; apply
	 (fn [hook]
	     (applicator
	      (fn [src-model src-key]
		  ;;(println "COUNT: " src-key src-model)
		  (let [tgt-model (do-reduce-count src-model src-key-type src-key tgt-metadata tgt-key)]
		    (insert *metamodel* tgt-model)
		    ;;(connect src-model tgt-model)
		    (hook tgt-model tgt-key)
		    tgt-model)))
	     )
	 "count"
	 ]
	)))

(defn msplit
  ([chain split-key mutable]
   (msplit chain split-key mutable list))
  ([chain split-key mutable split-fn]
   (fn []
       (let [[downstream-metadata-accessor applicator] (chain)
	     [src-metadata src-key] (downstream-metadata-accessor)]
	 [ ;; get metadata / key
	  (fn [] [src-metadata split-key])
	  ;; apply
	  (fn [hook]
	      (applicator
	       (fn [src-model src-key]
		   ;;(println "SPLIT: " src-key src-model)
		   (dsplit src-model split-key mutable split-fn identity
			   (fn [tgt-model tgt-key]
			       (insert *metamodel* tgt-model)
			       (hook tgt-model tgt-key)
			       tgt-model))
		   src-model)))
	  "split"
	  ])
       )))


(import org.dada.core.AbstractModel)
(import org.dada.core.Creator)
(import org.dada.core.Getter)
(import org.dada.core.GetterMetadata)
(import org.dada.core.View)

(defmulti attribute-name (fn [arg] (class arg)))
(defmethod attribute-name Date [#^Date date] (str "attribute_" (.getTime date)))
(defmethod attribute-name String [#^String string] (.replace string "_" "_underscore_"))

;; (defn mpivot [chain pivot-key pivot-values]
;;   (fn []
;;       (let [[downstream-metadata-accessor applicator] (chain)
;; 	    [src-metadata src-key] (downstream-metadata-accessor)
;; 	    pivot-key-keys (collection "type" "version")
;; 	    pivot-key-types (collection String Integer)
;; 	    pivot-value-type (.getAttributeType src-metadata (name pivot-key))
;; 	    pivot-value-types (repeat (count pivot-values) pivot-value-type)
;; 	    pivot-names (map (fn [key](attribute-name key)) pivot-values)
;; 	    pivot-symbols (map (fn [name] (symbol name)) pivot-names)
;; 	    pivot-keywords (map (fn [name] (keyword name)) pivot-names)
;; 	    pivot-keys (apply collection (concat pivot-key-keys pivot-names))
;; 	    pivot-types (apply collection (concat pivot-key-types pivot-value-types))
;; 	    tgt-model-name (str "pivot(" pivot-key ")")
;; 	    attributes (interleave pivot-keywords pivot-types)
;; 	    classname (name (gensym "org.dada.demo.core.Pivot"))
;; 	    tgt-metadata (class-metadata classname Object :type :version attributes)
;; 	    ]
;; 	[ ;; get metadata / key
;; 	 (fn [] [tgt-metadata pivot-key])
;; 	 ;; apply
;; 	 (fn [hook]
	      
;; 	     ;; call downstream installing connection and PivotModelView
;; 	     (let [src-keys (.getKeyAttributeKeys src-metadata)
;; 		   src-key-getter (.getAttributeGetter src-metadata (first src-keys))
;; 		   src-version-getter (.getAttributeGetter src-metadata (second src-keys))
;; 		   tgt-data (atom [{}{}]) ;; [input output]
;; 		   years (rand-int 10)
;; 		   tgt-model (PivotModelView. "pivot()" :key :count years src-metadata tgt-metadata)
;; 		   ]
;; 		   (applicator
;; 		    (fn [src-model src-key]
;; 			(insert *metamodel* tgt-model)
;; 			(println "PIVOT: " src-key src-model "->" pivot-key tgt-model)
;; 			(connect src-model tgt-model)
;; 			;; TODO: how do we do our transformation ?
;; 			(hook tgt-model pivot-key)
;; 			))
;; 		   tgt-model))
;; 	     ])
;; 	))
  
;; next step e.g. (select [a <model.field> b <model.field> c <model.field>] from [model....] where [(= a 10)...]....)

(defn execute [monad]
  ((second (monad)) (fn [model key])))

;;--------------------------------------------------------------------------------

(def my-whales (mlift whales "my-whales"))

;; count up whales by type...
;; (execute
;;  (munion
;;   (mcount 
;;    (msplit my-whales :type false))
;;   "union(count(split(whales, :type)))")
;;  )

;; count up whales by time by type
(def #^NavigableSet years
     (TreeSet.
      (list
       (Date. 0 0 1)
       (Date. 1 0 1)
       (Date. 2 0 1)
       (Date. 3 0 1)
       (Date. 4 0 1)
       (Date. 5 0 1)
       (Date. 6 0 1)
       (Date. 7 0 1)
       (Date. 8 0 1)
       (Date. 9 0 1))))

;; (execute
;;  ;; (msplit
;;  (munion
;;   (mcount
;;    (msplit my-whales :time false (fn [time] (list (or (.lower years time) time)))))
;;   "union(count(split(whales, :time)))")
;;  ;;  :type
;;  ;;  false)
;;  )

;; (execute
;;  ;;(mpivot
;; ;; (munion
;;   (mcount
;;    (msplit
;;     (msplit my-whales :type false)
;;     :time
;;     false
;;     (fn [time] (list (or (.lower years time) time))) ;; TODO: fencepost error...
;;     )
;;    )
;; ;;  "union(count(split(whales, :time)))")
;;  ;; :key years)
;;  )


;; try raising abstraction level
;; e.g. (mgroupby whales [:type :time] mcount ...)

;; do equivalent of :
;; (munion
;;  (<reduction>
;;   (msplit
;;    <dimension-1>
;;    ...
;;    (msplit
;;     <dimension-n>
;;     <chain>
;;     <dimension-n-args>
;;     )
;;    ...
;;    <dimension-1-args>
;;    )
;;   <reduction-args>)
;;  )

;;--------------------------------------------------------------------------------

;; TODO: Reducers need to support variable length set of attribute keys

;;----------------------------------------
;; THOUGHTS

;; primitives: a select is a combination of : filter, transform,
;; group, aggregate and join functions...

;; a (group) is a (filter) followed by a ?homogenous/symmetric join? ???

;; not sure yet about how a heterogenous/assymetric join works


;; IDEAS

;; spotting - whale-id, time, coordinates, weight, length
;; birth - whale-id time, weight, length, location
;; death - id, time, weight, length


;;(def factory (new ClassFactory))

;; TODO - factory comes from core

;;----------------------------------------
;; TODO
;; simplify splitting - Sparse splitting should use Object keys, not only int
;; transform needs to suppport synthetic attributes

;; create some whales...
(time
 (doall
  (let [batcher (Batcher. 999 1000 (list whales))
	#^Creator creator (.getCreator whale-metadata)
	max-length-x-100 (* max-length 100)
	max-weight-x-100 (* max-weight 100)]
    (pmap
     (fn [id]
	 (insert
	  whales ;; batcher
	  (.create
	   creator
	   (into-array
	    Object
	    [id
	     0
	     (Date. (rand-int 10)
		    (rand-int 12)
		    (+ (rand-int 28) 1))
	     (rnd types)
	     (/ (rand-int max-length-x-100) 100)
	     (/ (rand-int max-weight-x-100) 100)]))))
     (range num-whales)))))

(println "LOADED: " num-whales)


;; need some form of pluggable sorting algorithm

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

;; a group-by is a tree-structure where each level represents a
;; grouping by a further column. Difference subtrees should be
;; parallelisable.

;; initially group-by can be implemented as split/reduce/union - later, for efficiencies sake this may need to become a single operation - particularly if we want to express collapse-by-index-and-version as a group-by(key) 


;; versioned-index is actually an efficient split (by pk), reduce (all
;; versions into latest), union all latest versions back into another
;; set...

;; rotate/pivot

;; can only pivot when pivoted dimension consists of a closed set of
;; values - e.g. a projection, since if the set were open, the
;; insertion of new members would herald the restructuring of all
;; members of pivoted dataset - possible but nasty...

;; we could ensure this by prefiltering/pretransforming, but nicest
;; way is by a single split with clever statelss strategy-fn.

;; so pivot fn must supply such a closed list of values to be used as
;; attribute metadata for output model....

;; how about the application of fns vertically as well as horizontally
;; during transforms.



;;--------------------------------------------------------------------------------
;; THOUGHTS:

;; union fn needs access-to/to-coordinate metadata in subsequent layer of models
;; when a union is used, the class-related md etc used in these models should be shared

;; when sum fn is used must interrogate src-model md for details of chosed attribute

;; do I need type - e.g. Operation and Query<Operation> with a
;; lifecycle, with e.g. plan() and execute() phases ?
;; plan would allow the arrangement of metadata
;; execute would run the models... - maybe creating more and connecting them...
;; a metamodel could be optionally injected at the top of the query...

;; would our syntax be expressive enough ?

;; do we have to go the whole sql hog and have e.g.

;;(select/query :from <src-model> :where <filter> :groupby <split/union>)


;; testsuite should insert whales into db and model then run analagous
;; queries on both and compare result sets...
;; then run more data into both and compare result of db query and existing model state

;;--------------------------------------------------------------------------------

(defn mgroup [chain split-specs reduction-monad & reduction-args]
  (fn []
      (let [[chain-metadata-accessor chain-applicator] (chain)
	    [chain-metadata chain-key] (chain-metadata-accessor)
	    tgt-key (interpose "," (map (fn [[split-key & _]] (name split-key)) split-specs))]
	[ ;; get metadata / key
	 (fn [] [chain-metadata tgt-key])
	 ;; apply
	 (fn [hook]
	     (chain-applicator
	      (fn [src-model src-key]
		  (let [new-applicator (fn [hook] (hook src-model src-key))
			new-chain (fn [] [chain-metadata-accessor new-applicator])
			split (reduce
			       (fn [chain split-spec] (apply msplit chain split-spec))
			       new-chain
			       split-specs)
			[reduction-metadata-accessor reduction-applicator reduction-name]
			((apply reduction-monad split reduction-args))
			[reduction-metadata reduction-key] (reduction-metadata-accessor)
			tgt-name (str (.getName src-model) "." (apply str tgt-key) "=*." reduction-name)
			tgt-model (model tgt-name reduction-metadata)]
		    (insert *metamodel* tgt-model)
		    (hook tgt-model tgt-key)
		    (reduction-applicator
		     (fn [src-model src-key]
			 (connect src-model tgt-model)
			 tgt-model))
		    tgt-model))))
	 "union"
	 ])
      ))

(execute (mgroup my-whales [[:type false]] mcount))

(execute (mgroup my-whales [[:type false]
			    [:time false (fn [time] (list (or (.lower years time) time)))]]
		 mcount))