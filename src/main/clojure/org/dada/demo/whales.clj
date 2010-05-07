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

(def #^Class Whale (apply make-class "org.dada.demo.whales.Whale" Object attributes))
(def #^Metadata whale-metadata (metadata Whale [:id] md-attributes))
(def #^Model whales-model (model "Whales" :version whale-metadata))

(insert *metamodel* whales-model)

(let [#^Creator creator (.getCreator whale-metadata)
      max-length-x-100 (* max-length 100)
      max-weight-x-100 (* max-weight 100)]

  (defn whale [id]
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

;;----------------------------------------

(defn dfilter [#^Model src-model #^String tgt-view #^Collection attr-keys #^IFn filter-fn]
  ;; TODO: accept ready-made View
  (insert *metamodel* (do-filter tgt-view src-model attr-keys filter-fn)))

(defn dtransform [#^Model src-model #^String suffix #^Keyword key-key #^Keyword version-key & #^Collection attribute-descrips]
  ;; TODO: accept ready-made View ?
  ;; TODO: default key/version from src-model
  (insert *metamodel* (apply do-transform suffix src-model key-key version-key attribute-descrips)))

;;--------------------------------------------------------------------------------
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

;;--------------------------------------------------------------------------------

(defn metamodel [#^Model src-model]
  (let [metametadata (seq-metadata 1)
	metamodel (model (str "Meta-" (.getName src-model)) nil metametadata)]
    (insert *metamodel* metamodel)
    (insert metamodel [src-model])
    [metamodel (.getMetadata src-model) []]))

(import org.dada.core.View)
(import org.dada.core.Update)

(defn meta-view [#^Model src-metamodel tgt-metamodel f]
  (insert *metamodel* tgt-metamodel)
  (connect
   src-metamodel
   (proxy [View] []
	  (update [insertions alterations deletions]
		  (doall (map (fn [#^Update insertion] (apply f (.getNewValue insertion))) insertions))))))

(defn split [[#^Model src-metamodel #^Metadata src-metadata #^Collection extra-keys] split-key
	     & [split-key-fn]]
  (let [tgt-metamodel (model (str (.getName src-metamodel) ".split(" split-key")") nil (.getMetadata src-metamodel))]
    (meta-view
     src-metamodel
     tgt-metamodel
     (fn [model & extra-values]
	 (do-split
	  model
	  split-key
	  (or split-key-fn list)
	  identity
	  (fn [#^Model model extra-value]
	      (let [model-entry (list* model (concat extra-values [extra-value]))]
		(insert tgt-metamodel model-entry)
		(insert *metamodel* model)
		)))))
    [tgt-metamodel src-metadata (concat extra-keys [split-key])]))

;; extra keys are inserted into attribute list
;; extra values are carried in model's row in metamodel
;; each split adds an extra key/value downstream that we may need to unwrap upstream
(defn ccount [[#^Model src-metamodel #^Metadata src-metadata #^Collection extra-keys]
	      & [count-key]]
  (let [tgt-metamodel (model (str (.getName src-metamodel) ".count(" (or count-key "") ")") nil (.getMetadata src-metamodel))
	extra-attributes (map (fn [key] (.getAttribute src-metadata key)) extra-keys)
	tgt-metadata (count-reducer-metadata extra-keys count-key extra-attributes)]
    (meta-view
     src-metamodel
     tgt-metamodel
     (fn [#^Model src-model & extra-values]
	 (let [count-model (do-reduce-count 
			    (.getName src-model)
			    (.getMetadata src-model)
			    tgt-metadata
			    count-key
			    extra-values)]
	   (insert *metamodel* count-model)
	   (insert tgt-metamodel (list* count-model extra-values))
	   (connect src-model count-model))))
    [tgt-metamodel tgt-metadata extra-keys]))


(defn union [[#^Model src-metamodel #^Metadata src-metadata #^Collection extra-keys] #^String prefix]
  (let [tgt-metamodel (model (str (.getName src-metamodel) ".union()") nil (.getMetadata src-metamodel))
	tgt-model (model (str prefix ".union()") nil src-metadata)]
    (insert *metamodel* tgt-model)
    (insert tgt-metamodel [tgt-model])
    (meta-view src-metamodel tgt-metamodel (fn [src-model & extra-values] (connect src-model tgt-model)))
    [tgt-metamodel src-metadata extra-keys]))

;;--------------------------------------------------------------------------------

(def all-whales (metamodel whales-model))
(def counted-whales (ccount all-whales))

(def whales-by-type (split all-whales :type))
(def counted-whales-by-type (ccount whales-by-type))
(def grouped-counted-whales-by-type (union counted-whales-by-type "Whales.split(:type).count()"))

(def #^NavigableSet years
     (TreeSet.
      (collection
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

(def whales-by-type-and-year (split whales-by-type
				    :time 
				    (fn [time] (list (or (.lower years time) time)))))

(def counted-whales-by-type-and-year (ccount whales-by-type-and-year))
(def grouped-counted-whales-by-type-and-year (union counted-whales-by-type-and-year "Whales.split(:type).split(:time).count()"))

;;--------------------------------------------------------------------------------

;; create some whales...

(def num-whales 1000)

(time
 (doall
  (let [batcher (Batcher. 999 1000 (list whales-model))]
    (pmap (fn [id] (insert whales-model (whale id))) (range num-whales)))))

(println "LOADED: " num-whales)

;;--------------------------------------------------------------------------------
