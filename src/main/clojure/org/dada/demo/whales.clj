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

(def md-attributes
     (list
      [:id (Integer/TYPE) false]
      [:version (Integer/TYPE) true]
      [:time Date true]
      [:type String false]		;a whale cannot change type
      [:length (Float/TYPE) true]
      [:weight (Float/TYPE) true]
      ))

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
(def #^Metadata whale-metadata (metadata Whale md-attributes))
(def #^Model whales-model (model "Whales" :id :version whale-metadata))

(def num-whales 100000)

(insert *metamodel* whales-model)

;;----------------------------------------

(defn dfilter [#^Model src-model #^String tgt-view #^Collection attr-keys #^IFn filter-fn]
  ;; TODO: accept ready-made View
  (insert *metamodel* (do-filter tgt-view src-model attr-keys filter-fn)))

(defn dtransform [#^Model src-model #^String suffix #^Keyword key-key #^Keyword version-key & #^Collection attribute-descrips]
  ;; TODO: accept ready-made View ?
  ;; TODO: default key/version from src-model
  (insert *metamodel* (apply do-transform suffix src-model key-key version-key attribute-descrips)))

;;--------------------------------------------------------------------------------
;; monads

(defn mlift [#^Model model key value]
  (fn []
      [;; get metadata / key
       (fn [] [(.getMetadata model) key])
       ;; apply
       (fn [hook] (hook model value))
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

(defn mcount [chain & src-keys]
  (fn []
      ;; TODO - don't need src-key from src-metadata-accessor
      (let [[src-metadata-accessor applicator] (chain)
	    [src-metadata src-key] (src-metadata-accessor)
	    src-keys (if (empty? src-keys) [src-key] src-keys)
	    dummy (println "COUNT[1]: " src-keys (.getAttributeKeys src-metadata))
	    ;;src-types (map
	    src-key-type Object
	    count-key nil		;attribute we are counting - should be exposed
	    tgt-metadata (count-reducer-metadata count-key (map (fn [key] (.getAttribute src-metadata key)) src-keys))
	    tgt-key :count2]
	[ ;; get metadata / key
	 (fn [] [tgt-metadata tgt-key])
	 ;; applyp
	 (fn [hook]
	     (applicator
	      (fn [src-model src-value]	;TODO: should be src-values
		  ;;                 Object       the-type count       split(type) 
		  ;;                 Object       the-date count       split(type/time)
		  (println "COUNT[2]: " src-key-type src-value tgt-key ": " src-model)
		  (let [src-values [src-value] ;TODO
			tgt-model (do-reduce-count src-model src-values tgt-metadata tgt-key)]

;; HERE - need multiple keys and values...

		    (insert *metamodel* tgt-model)
		    (connect src-model tgt-model)
		    (hook tgt-model tgt-key)
		    tgt-model)))
	     )
	 "count"
	 ]
	)))

(defn msum [chain key]
  (fn []
      (let [[src-metadata-accessor applicator] (chain)
	    src-key-type Object ;; TODO: problem - we don't know the src-key yet
	    tgt-metadata (sum-reducer-metadata :key src-key-type)
	    tgt-key "sum"]
	[ ;; get metadata / key
	 (fn [] [tgt-metadata tgt-key])
	 ;; apply
	 (fn [hook]
	     (applicator
	      (fn [src-model src-key]
		  ;;(println "SUM: " src-key src-model)
		  (let [tgt-model (do-reduce-sum src-model key src-key-type tgt-key tgt-metadata)]
		    (insert *metamodel* tgt-model)
		    ;;(connect src-model tgt-model)
		    (hook tgt-model tgt-key)
		    tgt-model)))
	     )
	 "sum"
	 ]
	)))

(defn msplit
  ([chain split-key]
   (msplit chain split-key list))
  ([chain split-key split-fn]
   (fn []
       (let [[src-metadata-accessor applicator] (chain)
	     [src-metadata src-key] (src-metadata-accessor)]
	 [ ;; get metadata / key
	  (fn [] [src-metadata split-key])
	  ;; apply
	  (fn [hook]
	      (applicator
	       (fn [src-model src-value]
		   (do-split src-model split-key split-fn identity
			     (fn [tgt-model tgt-key]
				 (println "SPLIT: " src-value tgt-key split-key)
				 (insert *metamodel* tgt-model)
				 (hook tgt-model tgt-key)
				 tgt-model))
		   src-model)))
	  "split"
	  ])
       )))

(defn mgroup [chain split-specs reduction-monad & reduction-args]
  (fn []
      (let [[chain-metadata-accessor chain-applicator] (chain)
	    [chain-metadata chain-key] (chain-metadata-accessor)
	    tgt-key (interpose "," (map (fn [[split-key & _]] split-key) split-specs))
	    reduction-specs (map (fn [[key & _]] (.getAttribute chain-metadata key)) split-specs)
	    ]
	[ ;; get metadata / key
	 (fn [] [(count-reducer-metadata nil reduction-specs) ;; TODO - filthy temporary hack
		 ;;chain-metadata
		 tgt-key])
	 ;; apply
	 (fn [hook]
	     (chain-applicator
	      (fn [#^Model src-model src-key]
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
			[reduction-key-key reduction-version-key] (.getAttributeKeys reduction-metadata)
			tgt-model (model tgt-name reduction-key-key reduction-version-key reduction-metadata)]
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

(defn execute [monad]
  ((second (monad)) (fn [model key])))

;;--------------------------------------------------------------------------------
;; some actual queries...

(def whales (mlift whales-model :id 0))	;TODO - col should be optional

;; count the whales

(execute (mcount whales))

;; count up whales by type then sum these counts...

;; (execute (msum (mgroup whales [[:type]] mcount) :count))

;; ;; count whales by type/time - not yet working...

;; (def #^NavigableSet years
;;      (TreeSet.
;;       (collection
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

;; (execute
;;  (mgroup
;;   whales
;;   [[:type]
;;    [:time (fn [time] (list (or (.lower years time) time)))]]
;;   mcount))

;;----------------------------------------
;; TODO
;; simplify splitting - Sparse splitting should use Object keys, not only int
;; transform needs to suppport synthetic attributes


;; create some whales...
(time
 (doall
  (let [batcher (Batcher. 999 1000 (list whales-model))
	#^Creator creator (.getCreator whale-metadata)
	max-length-x-100 (* max-length 100)
	max-weight-x-100 (* max-weight 100)]
    (pmap
     (fn [id]
	 (insert
	  whales-model ;; batcher
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

;TODO: ensure that count() can accept 0=* or 1 (e.g.=:type) keys to
;count and 0-n key/val selected-pairs
