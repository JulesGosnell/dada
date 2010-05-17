(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use [org.dada.core])
 (:use org.dada.core.PivotModel)
 (:import [clojure.lang
	   ])
 (:import [java.math
	   ])
 (:import [java.util
	   Collection
	   Date
	   NavigableSet
	   TreeSet
	   ])
 (:import [org.dada.core
	   Batcher
	   Creator
	   Metadata
	   Model
	   PivotModel
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

(def oceans [
	     "arctic"
	     "atlantic"
	     "indian"
	     "pacific"
	     "southern"
	    ])

(def num-years 10)

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
      [:ocean String true]
      [:length (Float/TYPE) true]
      [:weight (Float/TYPE) true]
      ))

(def attributes (mapcat (fn [[k t]] [k t]) md-attributes))

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
       (Date. (rand-int num-years)
	      (rand-int 12)
	      (+ (rand-int 28) 1))
       (rnd types)
       (rnd oceans)
       (/ (rand-int max-length-x-100) 100)
       (/ (rand-int max-weight-x-100) 100)]))))

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

(defn dfilter [#^Model src-model #^String tgt-view #^Collection attr-keys #^IFn filter-fn]
  ;; TODO: accept ready-made View
  (insert *metamodel* (do-filter tgt-view src-model attr-keys filter-fn)))

(defn dtransform [#^Model src-model #^String suffix #^Keyword key-key #^Keyword version-key & #^Collection attribute-descrips]
  ;; TODO: accept ready-made View ?
  ;; TODO: default key/version from src-model
  (insert *metamodel* (apply do-transform suffix src-model key-key version-key attribute-descrips)))

;;--------------------------------------------------------------------------------
;; query operators...

;; a query returns a tuple of two functions [metadata-fn direct-fn] :
;; metadata-fn - returns upstream metadata - returns a tuple of [metadata metaprefix extra-keys ...]
;; data-fn -  returns a tuple of [metamodel prefix extra-pairs...]
;;--------------------------------------------------------------------------------

(import org.dada.core.View)
(import org.dada.core.Tuple)
(import org.dada.core.Update)

(defn metamodel [#^Model src-model]
  (let [prefix (.getName src-model)
	metaprefix (str "Meta-" prefix)]
    [ ;; metadata
     (fn [] [(.getMetadata src-model) metaprefix '()])
     ;; data
     (fn []
	 (let [metamodel (model (str "Meta-" prefix) nil (seq-metadata 1))]
	   (insert metamodel [src-model '()])
	   (insert *metamodel* metamodel)
	   [metamodel prefix '()]))]))

(defn ? [[metadata-fn direct-fn]]
  (let [metadata (metadata-fn)
	direct (direct-fn)]
    [ ;; metadata
     (fn [] metadata)
     ;; direct
     (fn [] direct)
     ]))

(defn thread-chain [chain model]
  (reduce (fn [results operator] (operator results)) model (reverse chain)))

(defn ?2 [chain model]
  (? (thread-chain chain model)))

(defn meta-view [#^String suffix #^Model src-metamodel f]
  ;; create a metamodel into which t place our results...
  (let [tgt-metamodel (model (str (.getName src-metamodel) suffix) nil (.getMetadata src-metamodel))]
    ;; register it with the global metamodel
    (insert *metamodel* tgt-metamodel)
    ;; view upstream metamodel for arrival of results
    (connect
     src-metamodel
     (proxy [View] []
	    (update [insertions alterations deletions]
		    (doall (map (fn [#^Update insertion] (apply f tgt-metamodel (.getNewValue insertion))) insertions)))))
    tgt-metamodel))

(defn union [& [model-name]]
  (fn [[metadata-fn direct-fn]]
      (let [[src-metadata metaprefix extra-keys] (metadata-fn)]
	[ ;; metadata
	 (fn []
	     [src-metadata (str metaprefix ".union()") extra-keys])
	 ;; direct
	 (fn []
	     (let [[#^Model src-metamodel prefix #^Collection extra-pairs] (direct-fn)
		   tgt-model (model (or model-name (str prefix ".union()")) nil src-metadata)
		   tgt-metamodel (meta-view ".union()" src-metamodel (fn [tgt-metamodel src-model extra-pairs] (connect src-model tgt-model)))]
	       (insert *metamodel* tgt-model)
	       (insert tgt-metamodel [tgt-model extra-pairs])
	       [tgt-metamodel (str prefix ".union()") extra-pairs]))])))

;; extra keys are inserted into attribute list
;; extra values are carried in model's row in metamodel
;; each split adds an extra key/value downstream that we may need to unwrap upstream
(defn ccount [& [count-key]]
  (fn [[metadata-fn data-fn]]
      (let [[#^Metadata src-metadata metaprefix extra-keys] (metadata-fn)
	    ;;dummy (println "COUNT METADATA" metaprefix extra-keys)
	    extra-attributes (map (fn [key] (.getAttribute src-metadata key)) extra-keys)
	    tgt-metadata (count-reducer-metadata extra-keys count-key extra-attributes)]
	[ ;; metadata
	 (fn []
	     [tgt-metadata (str metaprefix "." (count-value-key count-key)) extra-keys])
	 ;; direct
	 (if data-fn
	   (fn []
	       (let [[#^Model src-metamodel prefix #^Collection extra-pairs] (data-fn)
		     new-prefix (str prefix "." (count-value-key count-key))
		     tgt-model (model new-prefix nil src-metadata)
		     tgt-metamodel (meta-view
				    (str "." (count-value-key count-key))
				    src-metamodel
				    (fn [tgt-metamodel #^Model src-model extra-pairs]
					;;(println "CCOUNT DATA:" extra-values)
					(let [count-model (do-reduce-count 
							   (.getName src-model)
							   (.getMetadata src-model)
							   tgt-metadata
							   count-key
							   extra-pairs)]
					  (insert *metamodel* count-model)
					  (insert tgt-metamodel [count-model extra-pairs])
					  (connect src-model count-model))))]
		 [tgt-metamodel new-prefix extra-pairs])))])))

(defn sum [& [sum-key]]
  (fn [[metadata-fn data-fn]]
      (let [[#^Metadata src-metadata metaprefix extra-keys] (metadata-fn)
	    ;;dummy (println "SUM METADATA" metaprefix extra-keys)
	    extra-attributes (map (fn [key] (.getAttribute src-metadata key)) extra-keys)
	    tgt-metadata (sum-reducer-metadata extra-keys sum-key extra-attributes)]
	[ ;; metadata
	 (fn []
	     [tgt-metadata (str metaprefix "." (sum-value-key sum-key)) extra-keys])
	 ;; direct
	 (if data-fn
	   (fn []
	       (let [[#^Model src-metamodel prefix #^Collection extra-pairs] (data-fn)
		     new-prefix (str prefix "." (sum-value-key sum-key))
		     tgt-model (model new-prefix nil src-metadata)
		     tgt-metamodel (meta-view
				    (str "." (sum-value-key sum-key))
				    src-metamodel
				    (fn [tgt-metamodel #^Model src-model extra-pairs]
					;;(println "SUM DATA:" extra-values)
					(let [sum-model (do-reduce-sum 
							 (.getName src-model)
							 (.getMetadata src-model)
							 tgt-metadata
							 sum-key
							 extra-pairs)]
					  (insert *metamodel* sum-model)
					  (insert tgt-metamodel [sum-model extra-pairs])
					  (connect src-model sum-model))))]
		 [tgt-metamodel new-prefix extra-pairs])))])))

(defn split-key-value [key]
  (str "split(" (or key "") ")"))

(defn split [split-key & [split-key-fn subchain]]
  (fn [[src-metadata-fn direct-fn]]
      (let [src-metadata-tuple (src-metadata-fn)
	    [src-metadata src-metaprefix src-extra-keys] src-metadata-tuple
	    label (split-key-value split-key)
	    suffix (str "." label)
	    split-metadata-tuple [src-metadata (str src-metaprefix suffix) (concat src-extra-keys [split-key])]
	    [split-metadata split-metaprefix split-extra-keys] split-metadata-tuple
	    split-metadata-fn (fn [] split-metadata-tuple)
	    tgt-metadata-tuple (if subchain
				 (let [[sub-metadata-fn] (thread-chain subchain [split-metadata-fn nil])
					sub-metadata-tuple (sub-metadata-fn)
					[sub-metadata sub-metaprefix sub-extra-keys] sub-metadata-tuple]
				   ;;(println "SPLIT META SRC  " src-metadata-tuple)
				   ;;(println "SPLIT META SPLIT" split-metadata-tuple)
				   ;;(println "SPLIT META SUB  " sub-metadata-tuple)
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
		   [#^Model src-metamodel src-prefix #^Collection src-extra-pairs] src-data-tuple
		   tgt-metamodel (model (str (.getName src-metamodel) suffix) nil (.getMetadata src-metamodel))
		   tgt-prefix (str src-prefix "." split-key)
		   tgt-data-tuple [tgt-metamodel tgt-prefix (concat src-extra-pairs [[split-key "*"]])]]
	       ;; register it with the global metamodel
	       ;;(println "SPLIT DATA SRC  " src-data-tuple)
	       (insert *metamodel* tgt-metamodel)
	       ;; view upstream metamodel for the arrival or results
	       ;;(println "SPLIT - watching" src-metamodel)
	       (connect
		src-metamodel
		(proxy [View] []
		       (update [insertions alterations deletions]
			       (doall
				(map
				 (fn [#^Update insertion]
				     (let [[src-model src-extra-values] (.getNewValue insertion)
					   chain-fn (if subchain
						      ; plug split -> sub -> tgt
						      (fn [#^Model split-model split-extra-value]
							  (let [split-metamodel (model (str (.getName src-metamodel) (str suffix "=" split-extra-value)) nil (.getMetadata src-metamodel))
								split-prefix (str tgt-prefix "=" split-extra-value)
								split-extra-pairs (concat src-extra-pairs [[split-key split-extra-value]])
								split-data-tuple [split-metamodel split-prefix split-extra-pairs]
								split-data-fn (fn [] split-data-tuple)
								[_ sub-data-fn] (thread-chain subchain [split-metadata-fn split-data-fn])
								sub-data-tuple (sub-data-fn)
								[sub-metamodel sub-prefix sub-extra-pairs] sub-data-tuple
								split-model-tuple [split-model (concat src-extra-values [split-extra-value])]]
							    
							    ;;(println "SPLIT DATA SPLIT" split-data-tuple)
							    ;;(println "SPLIT DATA SUB" split-data-tuple)
							    (insert *metamodel* split-metamodel) ;add to global metamodel
							    (insert *metamodel* split-model) ;add to global metamodel
							    (insert split-metamodel split-model-tuple)
							    ;; collect results of calling this subchain and put them into our output metamodel
							    (connect
							     sub-metamodel
							     (proxy [View] []
								    (update [insertions alterations deletions]
									    (doall
									     (map
									      (fn [#^Update insertion]
										  (let [sub-model-tuple (.getNewValue insertion)] ;; [sub-model sub-extra-values]
										    ;; UNCOMMENT HERE AND WORK IT OUT
										    (insert tgt-metamodel sub-model-tuple)
										    ))
									      insertions)))))))
						      ;; plug split -> tgt
						      (fn [#^Model split-model split-extra-value]
							  ;;(println "SPLIT - producing new model" split-model split-extra-value)
							  (let [split-model-tuple (list split-model (concat src-extra-values [split-extra-value]))]
							    (insert *metamodel* split-model) ;add to global metamodel
							    (insert tgt-metamodel split-model-tuple) ;add to metamodel that has been passed downstream
							    ))
						      )]
				       ;; we have received a model from an upstream operation...
				       ;;(println "SPLIT - receiving new model" src-model src-extra-values)
				       ;; plug src-> split -> [chain defined above]
				       (do-split src-model split-key (or split-key-fn list) identity chain-fn)))
				 insertions)))))
	       tgt-data-tuple))]))
  )

;; pivot fn must supply such a closed list of values to be used as
;; attribute metadata for output model....
(defn pivot-metadata [#^Metadata src-metadata #^Collection keys #^Collection pivot-values value-key]
  (let [value-type (.getAttributeType src-metadata value-key)]
    (class-metadata 
     (name (gensym "org.dada.core.Pivot"))
     Object
     keys
     (concat
      (map #(.getAttribute src-metadata %) keys)
      [[:version Integer true]]
      (map #(vector % value-type true) pivot-values)))))

;; pivot-key - e.g. :time
;; pivot-values - e.g. years
;; value-key - e.g. :count(*) - needed to find type of new columns

(defn pivot [pivot-key pivot-values value-key]
  (fn [[metadata-fn direct-fn]]
      (let [[src-metadata metaprefix extra-keys] (metadata-fn)
	    ;;dummy (println "PIVOT METADATA" pivot-key keys value-key)
	    tgt-metadata (pivot-metadata src-metadata (remove #(= % pivot-key) extra-keys) pivot-values value-key)
	    tgt-name (str ".pivot(" value-key "/" pivot-key")")]
	[ ;; metadata
	 (fn []
	     [tgt-metadata (str metaprefix tgt-name) extra-keys])
	 ;; direct
	 (fn []
	     (let [[#^Model src-metamodel prefix #^Collection extra-pairs] (direct-fn)
		   ;;dummy (println "PIVOT" extra-pairs)
		   tgt-model (PivotModel. 
			      (str prefix tgt-name)
			      src-metadata
			      (second (first extra-pairs)) ; TODO:hack
			      (fn [old new] new) ;TODO - sort out version-fn
			      value-key
			      pivot-values
			      tgt-metadata)
		   tgt-metamodel (meta-view pivot-key src-metamodel (fn [tgt-metamodel src-model extra-values] (connect src-model tgt-model)))]
	       (insert *metamodel* tgt-model)
	       (insert tgt-metamodel [tgt-model extra-pairs])
	       [tgt-metamodel (str prefix ".union()") extra-pairs]))])
      ))

;;--------------------------------------------------------------------------------

(def all-whales (metamodel whales-model))

(def #^Collection some-years (map #(Date. % 0 1) (range num-years)))
(def #^NavigableSet years (TreeSet. some-years))

;; (?2 [] all-whales)

;; (?2 [(union)] all-whales)
;; (?2 [(ccount)] all-whales)
;; (?2 [(sum :weight)] all-whales)

;; (?2 [(split :type)] all-whales)

;; (?2 [(ccount)(ccount)] all-whales)
;; (?2 [(union)(union)] all-whales)
;; (?2 [(ccount)(union)] all-whales)
;; (?2 [(union)(ccount)] all-whales)
;; (?2 [(union)(split :type)] all-whales)
;; (?2 [(ccount)(split :type)] all-whales)
;; (?2 [(split :time)(split :type)] all-whales)

;; (?2 [(ccount)(split :time)(split :type)] all-whales)
;; (?2 [(union)(split :time)(split :type)] all-whales)
;; (?2 [(split :length)(split :time)(split :type)] all-whales)

;; (?2 [(split :type nil [(ccount)])] all-whales)
;; (?2 [(split :type nil [(split :time)])] all-whales)
;; (?2 [(split :type nil [(split :time nil [(split :length)])])] all-whales)

;; (?2 [(split :type nil [(ccount)(split :time)])] all-whales)
;; (?2 [(split :type nil [(split :time nil)(split :length)])] all-whales)

;; (?2 [(split :type nil [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time (fn [time] (list (or (.lower years time) time))))])] all-whales)

(?2 [(union "count/type/year")(split :type nil [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time (fn [time] (list (or (.lower years time) time))))])] all-whales)

(?2 [(union "count/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (count-value-key nil)))(ccount)(split :ocean )])] all-whales)

(?2 [(union "sum(weight)/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (sum-value-key :weight)))(sum :weight)(split :ocean )])] all-whales)

(?2 [(union "count/ocean/type")(split :ocean nil [(pivot :type types (keyword (count-value-key nil)))(ccount)(split :type )])] all-whales)

(?2 [(union "sum(weight)/ocean/type")(split :ocean nil [(pivot :type types (keyword (sum-value-key :weight)))(sum :weight)(split :type )])] all-whales)

;;(?2 [(split :ocean nil [(union)(split :type nil [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time (fn [time] (list (or (.lower years time) time))))])])] all-whales)

;;--------------------------------------------------------------------------------

;; create some whales...

(def num-whales 100000)

(def some-whales
     (pmap (fn [id] (whale id)) (range num-whales)))

(def some-whales2
     (let [#^Creator creator (.getCreator whale-metadata)]
       (map
	#(.create creator (into-array Object %))
	(list 
	 [0 0 (Date. 0 1 1) "blue whale" "arctic" 100 100]
	 [1 0 (Date. 0 1 1) "blue whale" "indian" 200 100]
	 [2 0 (Date. 0 1 1) "gray whale" "arctic" 100 100]
	 [3 0 (Date. 0 1 1) "gray whale" "indian" 200 100]
	 [4 0 (Date. 1 1 1) "blue whale" "arctic" 100 100]
	 [5 0 (Date. 1 1 1) "blue whale" "indian" 200 100]
	 [6 0 (Date. 1 1 1) "gray whale" "arctic" 100 100]
	 [7 0 (Date. 1 1 1) "gray whale" "indian" 200 100]
	 ))
       ))

(time
 (doall
  (let [batcher (Batcher. 999 1000 (list whales-model))]
    (pmap (fn [whale] (insert whales-model whale)) some-whales)))) ;TODO: reinstate pmap later

(println "LOADED")

nil

;;--------------------------------------------------------------------------------
;; fix so that a split/pivot can handle undeclared keys
