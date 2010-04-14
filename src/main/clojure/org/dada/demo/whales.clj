(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use [org.dada.core])
 (:import [clojure.lang
	   ])
 (:import [java.math
	   ])
 (:import [java.util
	   NavigableSet
	   TreeSet
	   ])
 (:import [org.dada.core
	   Batcher
	   Creator
	   Metadata
	   Model
	   ])
 (:import [org.joda.time
	   DateMidnight
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
      :time DateMidnight
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

(def num-whales 1000000)

(insert *metamodel* whales)

;;----------------------------------------

(defn dfilter [#^Model src-model #^String tgt-view #^Collection attr-keys #^IFn filter-fn]
  ;; TODO: accept ready-made View
  (insert *metamodel* (do-filter tgt-view src-model attr-keys filter-fn)))

(defn dtransform [#^Model src-model #^String suffix & #^Collection attribute-descrips]
  ;; TODO: accept ready-made View ?
  ;; TODO: default key/version from src-model
  (insert *metamodel* (apply do-transform suffix src-model attribute-descrips)))

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
  )

;; TODO: try to lose key-to-value and vice-versa then define dpivot...
;; TODO: should mutability be part of metadata ?
(defn dsplit
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

;; TODO
;; dindex

;;----------------------------------------

;; demonstrate filtration - only selecting a subset of the values from
;; one model into another - based on their contents.

;;----------------------------------------

(def median-length (/ max-length 2))
(def median-weight (/ max-weight 2))

(def longer-whales 
     (dfilter whales (str "length>" median-length) '(:length) #(> % median-length)))

(def heavier-whales
     (dfilter whales (str "weight>" median-weight) '(:weight) #(> % median-weight)))

;;----------------------------------------
;; demonstrate transformation - only selecting a subset of the
;; attributes from one model into another. We may support synthetic
;; attributes...
;;----------------------------------------

(def longer-whales-weight
     (dtransform longer-whales "weight" :id :version :id :version :weight))

(def #^Model heavier-whales-length
     (dtransform heavier-whales "length" :id :version :id :version :length))

;;----------------------------------------
;; demonstrate transformation - a synthetic field - metric tons per metre
;;----------------------------------------

(dtransform
 whales
 "tonsPerMetre"
 :id
 :version 
 :id
 :version 
 (list
  :tonsPerMetre
  Number
  '(:weight :length)
  (fn [weight length] (if (= length 0) 0 (/ weight length))))
 )

;;----------------------------------------
;; try a transformation on top of a filtration
;; e.g. select time, version, length from whales where type="narwhal"
;;----------------------------------------

(def narwhals-length
     (dtransform 
      (dfilter 
       whales
       "type=narwhal" 
       '(:type)
       #(= "narwhal" %))
      "length"
      :id
      :version
      :id
      :version
      :length)
     )

;;----------------------------------------
;; demonstrate reduction (sum)
;;----------------------------------------

(dcount whales "whales")
(dsum whales :length "whales")
(dsum whales :weight "whales")
(dcount heavier-whales-length "heavier-whales")
(dsum heavier-whales-length :length "heavier-whales")
(dsum longer-whales-weight :weight "longer-whales")
(dcount longer-whales-weight "longer-whales")

;;----------------------------------------
;; demonstrate splitting [and more reduction] - a pivot...
;;----------------------------------------

;; PROBLEM - 
;; tgt-model values must be same shape as intermediate model values...
;; EITHER: we prearrange the shape (preferred - complex - how do do this?)
;; OR: we could construct the tgt mode lazily (simpler - but concurrency issues)

;; (defn dpivot [#^Model src-model #^Keyword split-key #^boolean mutable #^String tgt-model-name #^IFn tgt-model-hook
;; 	      #^Keyword key-key #^Keyword version-key & attribute-key-types]
;;   (let [tgt-metadata (class-metadata
;; 		      (name (gensym "org.dada.demo.core.Pivot"))
;; 		      Object
;; 		      key-key
;; 		      version-key
;; 		      attribute-key-types)
;; 	tgt-model (model tgt-model-name tgt-metadata)]
;;     (insert *metamodel* tgt-model)
;;     (dsplit
;;      src-model
;;      split-key
;;      mutable
;;      (fn [#^Model model value]
;; 	 (connect (dcount model String value tgt-metadata) tgt-model)
;; 	 (tgt-model-hook model value)
;; 	 )
;;      ))
;;   )

(let [union-metadata (class-metadata
		      "org.dada.demo.whales.TypeCount"
		      Object
		      :key
		      :version
		      [:key String :version Integer :count Integer])
      union-model (model "Whales.union(count(split(type)))" union-metadata)
      ]
  (insert *metamodel* union-model)
  (dsplit
   whales
   :type
   false
   (fn [#^Model model value]
       (connect (dcount model String value union-metadata) union-model)
       (dsum model :length value)
       (dsum model :weight value)
       model ; TODO: do we really need to be able to override the model ? should threading go here ?
       )
   ))

(def #^NavigableSet dates (TreeSet.
	    (list
	    (DateMidnight. 2000 1 1)
	    (DateMidnight. 2001 1 1)
	    (DateMidnight. 2002 1 1)
	    (DateMidnight. 2003 1 1)
	    (DateMidnight. 2004 1 1)
	    (DateMidnight. 2005 1 1)
	    (DateMidnight. 2006 1 1)
	    (DateMidnight. 2007 1 1)
	    (DateMidnight. 2008 1 1)
	    (DateMidnight. 2009 1 1))))

(def year-names (map (fn [day] (.toString day "yyyy")) dates))

(eval (list 'deftype 'pivot-type (apply vector 'type 'version (map (fn [year-name] (symbol year-name)) year-names))))

(def attribute-names (apply collection "type" "version" year-names))

(import org.dada.core.Creator)
(import org.dada.core.Getter)
(import org.dada.core.GetterMetadata)

(def pivot-metadata
     (GetterMetadata.
      (proxy [Creator] [] (create [args] (apply pivot-type args)))
      (collection "type" "version") ;; key-attribute-names
      (apply collection String Integer (repeat 10 DateMidnight)) ;; attribute-types
      attribute-names
      (apply collection
	     (map
	      (fn [name]
		  (let [key (keyword (symbol name))]
		    (proxy [Getter] [] (get [instance] (key instance)))))
	      attribute-names))))

(def survey-metadata (class-metadata
		      (name (gensym "org.dada.demo.whales.Survey"))
		      Object
		      :time
		      :version
		      [:time DateMidnight :version Integer :count Integer]))

(def pivot-model (model (str (.getName whales) ".pivot(count(time))") pivot-metadata))
(insert *metamodel* pivot-model)

(dsplit
 whales
 :type
 false
 (fn [#^Model type-model type]
     (let [survey-model (model (str (.getName type-model) ".count(time)") survey-metadata)]
       (insert *metamodel* survey-model)
       ;; need some sort of transform here...'
       ;;(connect survey-model pivot-model)
       (dsplit
	type-model
	:time
	false
	(fn [time] (list (or (.lower dates time) time)))
	identity
	(fn [#^Model time-model time]
	    (connect (dcount time-model DateMidnight time survey-metadata) survey-model)
	    time-model))
       )
     type-model)
 )

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
	     (DateMidnight. (+ (rand-int 10) 2000)
			    (+ (rand-int 12) 1)
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

