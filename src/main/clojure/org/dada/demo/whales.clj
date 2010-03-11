(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use [org.dada.core])
 (:import [clojure.lang Symbol])
 (:import [java.math BigDecimal])
 (:import [java.util Collection])
 (:import [org.dada.core
	   Average
	   Creator
	   Getter
	   GetterMetadata
	   Metadata
	   Model
	   Transformer
	   Transformer$Transform
	   VersionedModelView

	   Update
	   Reducer
	   Reducer$Strategy
	   ])
 )

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

;; IDEAS

;; spotting - whale-id, time, coordinates, weight, length
;; birth - whale-id time, weight, length, location
;; death - id, time, weight, length


;;(def factory (new ClassFactory))

;; TODO - factory comes from core

(if false
  (def *metamodel*
       (new org.dada.core.MetaModelImpl
	    "Cetacea"
	    (new org.dada.core.StringMetadata "type")
	    (new org.dada.core.SynchronousServiceFactory)))
  (do
    (def *metamodel* (start-server "Cetacea"))
    (start-client "Cetacea")))

;; TODO need class available to type accessors....
(def attributes
     (list 
      :time (Long/TYPE)
      :version (Integer/TYPE)
      :type String
      :length (Float/TYPE)
      :weight (Float/TYPE)))

(def #^Class Whale (apply make-class "org.dada.demo.whales.Whale" Object attributes))
(def #^Metadata whale-metadata (apply metadata Whale :time :version attributes))
(def #^Model whales (model "Whales" whale-metadata))

(insert *metamodel* whales)

;; create 10,000 whales and insert them into model
(insert-n
 whales
 (let [num-whales 10000
       time (System/currentTimeMillis)
       #^Creator creator (.getCreator whale-metadata)
       max-length-x-100 (* max-length 100)
       max-weight-x-100 (* max-weight 100)]
   (for [id (range time (+ time num-whales))]
     (.create
      creator
      (into-array
       Object
       [id
	0
	(rnd types)
	(/ (rand-int max-length-x-100) 100)
	(/ (rand-int max-weight-x-100) 100)])))))

;;----------------------------------------

;; demonstrate filtration - only selecting a subset of the values from
;; one model into another - based on their contents.

;;----------------------------------------

(let [half-max-length (/ max-length 2)]
  (def longer-whales
       (do-filter (str "length>" half-max-length) whales '(:length) #(> % half-max-length))))

(insert *metamodel* longer-whales)

(let [half-max-weight (/ max-weight 2)]
  (def heavier-whales
       (do-filter (str "weight>" half-max-weight)  whales '(:weight) #(> % half-max-weight))))

(insert *metamodel* heavier-whales)

;;----------------------------------------
;; demonstrate transformation - only selecting a subset of the
;; attributes from one model into another. We may support synthetic
;; attributes...
;;----------------------------------------

(def longer-whales-weight (do-transform "weight" longer-whales :time :version :weight))
(insert *metamodel* longer-whales-weight)

(def #^Model heavier-whales-length (do-transform "length" heavier-whales :time :version :length))
(insert *metamodel* heavier-whales-length)

;;----------------------------------------
;; try a transformation on top of a filtration
;; e.g. select time, version, length from whales where type="narwhal"
;;----------------------------------------

(def narwhals-length
     (do-transform 
      "length"
      (do-filter 
       "type=narwhal" 
       whales
       '(:type)
       #(= "narwhal" %))
      :time
      :version
      :length)
     )
(insert *metamodel* narwhals-length)

;;----------------------------------------
;; demonstrate splitting
;;----------------------------------------

(let [type-to-route (apply hash-map (interleave types (range (count types))))
      #^"[Ljava.lang.String;" route-to-type (into-array String types)]
  (do-split whales :type false type-to-route #(aget route-to-type #^Integer %) #(insert *metamodel* %)))

;;----------------------------------------
;; demonstrate reduction
;;----------------------------------------

(let [metadata (class-metadata "org.dada.core.Sum" Object :key :version
			       [:key String :version Integer :sum Number])
      getter (.getAttributeGetter (.getMetadata heavier-whales-length) "length")
      accessor (fn [value] (.get getter value))
      new-value (fn [#^Update update] (accessor (.getNewValue update)))
      old-value (fn [#^Update update] (accessor (.getOldValue update)))]
  (def sum
       (proxy
	[Reducer$Strategy]
	[]
	(initialValue [] 0M)
	(initialType [type] type)
	(currentValue [key version value]
		      (.create #^Metadata metadata #^Collection (collection key version value)))
	(reduce [insertions updates deletions]
		(-
		 (+
		  (reduce #(+ %1 (new-value %2)) 0 insertions)
		  (reduce #(+ %1 (- (new-value %2) (old-value %2))) 0 updates))
		 (reduce #(+ %1 (old-value %2)) 0 deletions))
		)
	(apply [currentValue delta] 
	       (+ currentValue delta))
	))

  (defn make-reducer [#^Model model #^Keyword key-key #^Reducer$Strategy strategy]
    (let [key (name key-key)]
      (Reducer. (str (.getName model) ".sum(" key ")") metadata key strategy)))
  )

(defn do-reduce [#^Model model #^Keyword key #^Reducer$Strategy reduction]
  (connect model (make-reducer model key reduction)))

;; sum, average, mean, mode, COUNT, minimum, maximum,...
(def heavier-whales-length-sum (do-reduce heavier-whales-length :length sum))
(insert *metamodel* heavier-whales-length-sum)

;;----------------------------------------
;; select - being refactored into filter, transform, aggregate, group, ...
;;----------------------------------------

;; ;; syntactic sugar
;; (defn fetch [& args]
;;   (let [output (apply select args)]
;;     (insert *metamodel* output)
;;     output))

;; ;; here
;; ;; select whales into a new model...
;; (def narwhals (fetch whales :time :version '([:time] [:version] [:type][:weight]) :model "Narwhals" :filter (fn [value] (= "narwhal" (. value getType)))))

;; (def belugas (fetch whales :time :version '([:time] [:version] [:type][:length]) :model "Belugas" :filter (fn [value] (= "beluga whale" (. value getType)))))

;; (import org.dada.core.Reducer)
;; (import org.dada.core.Reducer$Strategy)

;; ;; make a class to hold key, version and aggregate value...

;; (def aggregator-initial-values
;;      {
;;      (Integer/TYPE) 0
;;      Integer 0
;;      (Float/TYPE) (new Float "0")
;;      Float (new Float "0")
;;      (Double/TYPE) (new Double "0")
;;      Double (new Double "0")
;;      BigDecimal (new BigDecimal "0")
;;      Number 0
;;      Average (new Average (BigDecimal/ZERO) 0)
;;      })

;; (def aggregator-metadata {
;;      'count [(Integer/TYPE) 0]
;;      'sum [Number 0]
;;      'average [Average (new Average (BigDecimal/ZERO) 0)]
;;      })

;; (defn make-sum-aggregator [metadata modelname pkval field-key]
;;   (let [attribute-name (name field-key)
;; 	attribute-getter (.getAttributeGetter metadata attribute-name)
;; 	attribute-type (.getAttributeType metadata attribute-name)
;; 	initial-value (aggregator-initial-values attribute-type)
;; 	accessor #(.get attribute-getter %)]
;;     (AggregatedModelView.
;;      modelname
;;      metadata
;;      pkval
;;      (proxy
;;       [AggregatedModelView$Aggregator]
;;       []
;;       (initialValue []
;; 		    initial-value)
;;       (initialType [type]
;; 		    type)
;;       (currentValue [key version value]
;; 		    (.create metadata [key version value]))
;;       (aggregate [insertions updates deletions]
;; 		 (- 
;; 		  (+
;; 		   (apply + (map #(accessor (.getNewValue %)) insertions))
;; 		   (apply + (map #(accessor (.getNewValue %)) updates))
;; 		   )
;; 		  (+
;; 		   (apply + (map #(accessor (.getOldValue %)) updates))
;; 		   (apply + (map #(accessor (.getOldValue %)) deletions))
;; 		   )))
;;       (apply [currentValue delta]
;; 	     (+ currentValue delta))
;;       )
;;      )))

;; (defn make-count-aggregator [metadata model-name pkval field-key]
;;   (let [getter (.getAttributeGetter metadata (name field-key))
;; 	accessor #(.get getter %)]
;;     (AggregatedModelView.
;;      model-name
;;      metadata
;;      pkval
;;      (proxy
;;       [AggregatedModelView$Aggregator]
;;       []
;;       (initialValue []
;; 		    0)
;;       (initialType [type]
;; 		   Integer)
;;       (currentValue [key version value]
;; 		    (.create metadata [key version value]))
;;       (aggregate [insertions updates deletions]
;; 		 (- (count insertions) (count deletions)))
;;       (apply [currentValue delta]
;; 	     (+ currentValue delta))
;;       )
;;      )))

;; (defn make-average-aggregator [metadata model-name pkval field-key]
;;   (let [getter (.getAttributeGetter metadata field-key)
;; 	accessor #(.get getter %)]
;;     (AggregatedModelView.
;;      model-name
;;      metadata
;;      pkval
;;      (proxy
;;       [AggregatedModelView$Aggregator]
;;       []
;;       (initialValue []
;; 		    (aggregator-initial-values Average))
;;       (initialType []
;; 		   Average)
;;       (currentValue [key version value]
;; 		    (.create metadata [key version value]))
;;       (aggregate [insertions updates deletions]
;; 		 (new
;; 		  org.dada.core.Average
;; 		  (new BigDecimal
;; 		       (.toString ;; TODO - aaaaarrrrrrrggggggggghhhhhh !!
;; 			(-	  ; total
;; 			 (+
;; 			  (apply + (map #(accessor (.getNewValue %)) insertions))
;; 			  (apply + (map #(accessor (.getNewValue %)) updates))
;; 			  )
;; 			 (+
;; 			  (apply + (map #(accessor (.getOldValue %)) updates))
;; 			  (apply + (map #(accessor (.getOldValue %)) deletions))
;; 			  ))))
;; 		  (- (count insertions) (count deletions)) ; denominator
;; 		  ))
;;       (apply [currentValue deltaValue]
;; 	     (let [currentTotal (.getSum currentValue)
;; 		   currentDenominator (.getDenominator currentValue)
;; 		   deltaTotal (.getSum deltaValue)
;; 		   deltaDenominator (.getDenominator deltaValue)]
;; 	       (new org.dada.core.Average
;; 		    (.add currentTotal deltaTotal) (+ currentDenominator deltaDenominator))))
;;       )
;;      )))

;; classname - "org.dada.demo.whales.Length"
;; modelname - "Beluga.Length"
;; pktype - String
;; pkname - "type"
;; pkval - "beluga whale"
;; fieldtype - (Float/TYPE)
;; fieldname - "length"
;; initial-value - (new Float "0")
;; accessor - .getLength

;; make-aggregator [metadata modelname pkval initial-value fieldname]
;; (def
;;  sum-aggregator
;;  (let [expr "SUM(length)"
;;        field-key :length
;;        field-type Number
;;        package-name "org.dada.demo.whales" 
;;        class-name (str package-name "." expr)
;;        class (make-class class-name Object :type String :version (Integer/TYPE) field-key field-type)
;;        metadata (metadata class :type :version field-key field-type)]
;;    (make-sum-aggregator metadata (str "Beluga." expr) "beluga whale" field-key)
;;    ))

;; (def
;;  count-aggregator
;;  (let [expr "COUNT(*)"
;;        field-type (Integer/TYPE) ;; TODO - should get this from aggregator
;;        field-key :count
;;        package-name "org.dada.demo.whales" 
;;        class-name (str package-name "." expr)
;;        class (make-class class-name Object :type String :version (Integer/TYPE) field-key field-type)
;;        metadata (metadata class :type :version field-key field-type)]
;;    (make-count-aggregator metadata (str "Beluga." expr) "beluga whale" field-key)
;;    ))

;; (def
;;  average-aggregator
;;  (let [expr "AVERAGE(length)"
;;        field-key :length
;;        field-type Average			;TODO - we should have asked aggregator
;;        package-name "org.dada.demo.whales" 
;;        class-name (str package-name "." expr)
;;        class (make-class class-name Object :type String :version (Integer/TYPE) field-key field-type)
;;        metadata (metadata class :type :version field-key field-type)]
;;    (make-average-aggregator metadata (str "Beluga." expr) "beluga whale" field-key)
;;    ))

;; (insert *metamodel* sum-aggregator)
;; (insert *metamodel* count-aggregator)
;; (insert *metamodel* average-aggregator)

;; (connect belugas sum-aggregator)
;; (connect belugas count-aggregator)
;; (connect belugas average-aggregator)


;; extract output class from aggregate and create an aggregator per type of whale
;; collect all aggregators together for length and weight
;; pivot them into a single Model

;;----------------------------------------
;; aggregation
;;----------------------------------------

;; (defn make-aggregator [model key-name key-val #^Symbol aggregator attribute]
;;   (let [attribute-type (first (aggregator-metadata aggregator))
;; 	key key-name
;; 	key-type (type key-val)
;; 	model-name (str (.getName model) "." (.toString aggregator) "(" attribute ")")
;; 	class-name (.toString (gensym (str "org.dada.demo.tmp.Aggregator." aggregator)))
;; 	class (make-class class-name Object key-name key-type :version (Integer/TYPE) attribute attribute-type)
;; 	metadata (metadata class key :version attribute attribute-type)
;; 	aggregator-fn (eval (symbol (str "make-" aggregator "-aggregator")))]
;;     (aggregator-fn metadata model-name key-val attribute)))

;; (defn do-aggregate
;;   "apply an aggregator view to a model"
;;   [model key-name key-val aggregator attribute]
;;   (connect model (make-aggregator model key-name key-val aggregator attribute)))


;; (def blue-whales-sum-length (do-aggregate blue-whales :type "blue whale" 'sum :length))
;; (def blue-whales-count-length (do-aggregate blue-whales :type "blue whale" 'count :length))
;; (def blue-whales-average-length (do-aggregate blue-whales :type "blue whale" 'average :length))

;; (insert *metamodel* blue-whales-sum-length)
;; (insert *metamodel* blue-whales-count-length)
;; (insert *metamodel* blue-whales-average-length)

;;----------------------------------------
;; combined filtration and aggregation
;; "combined weight of all killer whales"
;;----------------------------------------



;; (let [model whales
;;       filter-attr-key :type
;;       filter-attr-value "killer whale"
;;       filtration (do-filter "type='killer whale'" model [filter-attr-key] #(= % filter-attr-value))

;;       aggregator-attr-key :weight
;;       aggregator-symbol 'count ;; 'sum, 'average, 'count
      
;;       aggregation (do-aggregate 
;;       		   filtration
;; 		   filter-attr-key filter-attr-value aggregator-symbol aggregator-attr-key)
;;   ]
;;   (insert *metamodel* filtration)
;;   (insert *metamodel* aggregation)
;;   )

;; TODO - aggregated column's name and type depends on aggregator, not original column type

;;----------------------------------------

  


;; THOUGHTS

;; primitives: a select is a combination of : filter, transform,
;; group, aggregate and join functions...

;; a (group) is a (filter) followed by a ?homogenous/symmetric join? ???

;; not sure yet about how a heterogenous/assymetric join works

