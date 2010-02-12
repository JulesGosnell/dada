(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use [org.dada.core])
 (:import [org.dada.core Average Creator Getter GetterMetadata VersionedModelView])
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


(def types '(
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
	     ))

(defn rnd [seq] (nth seq (rand-int (count seq))))

;; IDEAS

;; spotting - whale-id, time, coordinates, weight, length
;; birth - whale-id time, weight, length, location
;; death - id, time, weight, length


;;(def factory (new ClassFactory))

;; TODO - factory comes from core

(def properties (list [(Long/TYPE) "time"]
		      [(Integer/TYPE) "version"]
		      [String "type"]
		      [(Float/TYPE) "length"]
		      [(Float/TYPE) "weight"]))

(if false
  (def *metamodel*
       (new org.dada.core.MetaModelImpl
	    "Cetacea"
	    (new org.dada.core.StringMetadata "type")
	    (new org.dada.core.SynchronousServiceFactory)))
  (do
    (def *metamodel* (start-server "Cetacea"))
    (start-client "Cetacea")))

(def Whale (apply make-class "org.dada.demo.whales.Whale" Object properties))

;; TODO - metadata should encapsulate class...
(def whale-metadata
     (metadata Whale :time :version 
	       :time (Long/TYPE)
	       :version (Integer/TYPE)
	       :type String
	       :length (Float/TYPE)
	       :weight (Float/TYPE)))

(def whales (model "Whales" whale-metadata))

(insert *metamodel* whales)

;; create 10,000 whales and insert them into model
(insert-n
 whales
 (let [num-whales 1000
       time (System/currentTimeMillis)]
   (for [id (range time (+ time num-whales))]
     (. whale-metadata
	create
	(into-array
	 Object
	 (list id
	       0
	       (rnd types)
	       (/ (rand-int 3290) 100)     ;; 32.9 metres
	       (/ (rand-int 172) 100))))))) ;; 172 metric tons

;; syntactic sugar
(defn fetch [& args]
  (let [output (apply select args)]
    (insert *metamodel* output)
    output))

;; select whales into a new model...
(def narwhals (fetch whales "time" "version" '(["time"] ["version"] ["type"]["weight"]) :model "Narwhals" :filter (fn [value] (= "narwhal" (. value getType)))))

(def belugas (fetch whales "time" "version" '(["time"] ["version"] ["type"]["length"]) :model "Belugas" :filter (fn [value] (= "beluga whale" (. value getType)))))

(import org.dada.core.AggregatedModelView)
(import org.dada.core.AggregatedModelView$Aggregator)

;; make a class to hold key, version and aggregate value...

(defn make-metadata [classname pktype pkname fieldtype fieldname]
  (let [properties (list [pktype pkname] [(Integer/TYPE) "version"] [fieldtype fieldname])
	class (apply make-class classname Object properties)]
  (new GetterMetadata
       (proxy [Creator] [] (create [args] (apply make-instance class args)))
       (collection pkname "version")
       (map first properties)
       (map second properties)
       (map 
	(fn [property]
	    (make-proxy-getter 
	     class
	     (first property)
	     (second property)))
	properties))))

(def aggregator-initial-values
     {
     (Integer/TYPE) 0
     Integer 0
     (Float/TYPE) (new Float "0")
     Float (new Float "0")
     (Double/TYPE) (new Double "0")
     Double (new Double "0")
     BigDecimal (new BigDecimal "0")
     Number 0
     Average (new Average (BigDecimal/ZERO) 0)
     })

(def aggregator-metadata {
     'count [(Integer/TYPE) 0]
     'sum [Number 0]
     'average [Average (new Average (BigDecimal/ZERO) 0)]
     })

(defn make-sum-aggregator [metadata modelname pkval fieldname]
  (let [attribute-names (.getAttributeNames metadata)
	getter ((apply array-map (interleave attribute-names (.getAttributeGetters metadata))) fieldname)
	attribute-types (.getAttributeTypes metadata)
	attribute-type ((apply array-map (interleave attribute-names attribute-types)) fieldname)
	initial-value (aggregator-initial-values attribute-type)
	accessor #(.get getter %)]
    (AggregatedModelView.
     modelname
     metadata
     pkval
     (proxy
      [AggregatedModelView$Aggregator]
      []
      (initialValue []
		    initial-value)
      (initialType [type]
		    type)
      (currentValue [key version value]
		    (. metadata create (into-array Object (list key version value))))
      (aggregate [insertions updates deletions]
		 (- 
		  (+
		   (apply + (map #(accessor (.getNewValue %)) insertions))
		   (apply + (map #(accessor (.getNewValue %)) updates))
		   )
		  (+
		   (apply + (map #(accessor (.getOldValue %)) updates))
		   (apply + (map #(accessor (.getOldValue %)) deletions))
		   )))
      (apply [currentValue delta]
	     (+ currentValue delta))
      )
     )))

(defn make-count-aggregator [metadata modelname pkval fieldname]
  (let [getter ((apply array-map (interleave (.getAttributeNames metadata) (.getAttributeGetters metadata))) fieldname)
	accessor #(.get getter %)]
    (AggregatedModelView.
     modelname
     metadata
     pkval
     (proxy
      [AggregatedModelView$Aggregator]
      []
      (initialValue []
		    0)
      (initialType [type]
		    Integer)
      (currentValue [key version value]
		    (. metadata create (into-array Object (list key version value))))
      (aggregate [insertions updates deletions]
		 (- (count insertions) (count deletions)))
      (apply [currentValue delta]
	     (+ currentValue delta))
      )
     )))

(defn make-average-aggregator [metadata modelname pkval fieldname]
  (let [getter ((apply array-map (interleave (.getAttributeNames metadata) (.getAttributeGetters metadata))) fieldname)
	accessor #(.get getter %)]
    (AggregatedModelView.
     modelname
     metadata
     pkval
     (proxy
      [AggregatedModelView$Aggregator]
      []
      (initialValue []
		    (aggregator-initial-values Average))
      (initialType []
		   Average)
      (currentValue [key version value]
		    (. metadata create (into-array Object (list key version value))))
      (aggregate [insertions updates deletions]
		 (new
		  org.dada.core.Average
		  (new BigDecimal
		       (.toString ;; TODO - aaaaarrrrrrrggggggggghhhhhh !!
			(-	  ; total
			 (+
			  (apply + (map #(accessor (.getNewValue %)) insertions))
			  (apply + (map #(accessor (.getNewValue %)) updates))
			  )
			 (+
			  (apply + (map #(accessor (.getOldValue %)) updates))
			  (apply + (map #(accessor (.getOldValue %)) deletions))
			  ))))
		  (- (count insertions) (count deletions)) ; denominator
		  ))
      (apply [currentValue deltaValue]
	     (let [currentTotal (.getSum currentValue)
		   currentDenominator (.getDenominator currentValue)
		   deltaTotal (.getSum deltaValue)
		   deltaDenominator (.getDenominator deltaValue)]
	       (new org.dada.core.Average
		    (.add currentTotal deltaTotal) (+ currentDenominator deltaDenominator))))
      )
     )))

;; classname - "org.dada.demo.whales.Length"
;; modelname - "Beluga.Length"
;; pktype - String
;; pkname - "type"
;; pkval - "beluga whale"
;; fieldtype - (Float/TYPE)
;; fieldname - "length"
;; initial-value - (new Float "0")
;; accessor - .getLength

;; make-metadata [classname pktype pkname fieldtype fieldname]
;; make-aggregator [metadata modelname pkval initial-value fieldname]
(def
 sum-aggregator
 (let [expr "SUM(length)"
       fieldname "length"
       fieldtype Number
       packagename "org.dada.demo.whales" 
       classname (str packagename "." expr)
       metadata (make-metadata classname String "type" fieldtype fieldname)]
   (make-sum-aggregator metadata (str "Beluga." expr) "beluga whale" fieldname)
   ))

(def
 count-aggregator
 (let [expr "COUNT(*)"
       fieldtype (Integer/TYPE) ;; TODO - should get this from aggregator
       fieldname "count"
       packagename "org.dada.demo.whales" 
       classname (str packagename "." expr)
       metadata (make-metadata classname String "type" fieldtype fieldname)]
   (make-count-aggregator metadata (str "Beluga." expr) "beluga whale" fieldname)
   ))

(def
 average-aggregator
 (let [expr "AVERAGE(length)"
       fieldname "length"
       fieldtype Average			;TODO - we should have asked aggregator
       packagename "org.dada.demo.whales" 
       classname (str packagename "." expr)
       metadata (make-metadata classname String "type" fieldtype fieldname)]
   (make-average-aggregator metadata (str "Beluga." expr) "beluga whale" fieldname)
   ))

(insert *metamodel* sum-aggregator)
(insert *metamodel* count-aggregator)
(insert *metamodel* average-aggregator)

(connect belugas sum-aggregator)
(connect belugas count-aggregator)
(connect belugas average-aggregator)


;; extract output class from aggregate and create an aggregator per type of whale
;; collect all aggregators together for length and weight
;; pivot them into a single Model

;;----------------------------------------
;; utils
;;----------------------------------------

(defn clone-model [model name]
 (let [metadata (.getMetadata model)
       names (. metadata getAttributeNames)
       getters (. metadata getAttributeGetters)
       getter-map (apply array-map (interleave names getters))
       keys (. metadata getKeyAttributeNames)
       key (first keys)
       key-getter (getter-map key)
       version (second keys)
       version-getter (getter-map version)]
   (VersionedModelView. name metadata key-getter version-getter)))

;;----------------------------------------
;; filtration
;;----------------------------------------

(defn select-filter
  "apply a filter view to a model"
  ([model filter-fn view dummy]
   (connect model (make-filter filter-fn view))
   view)
  ([model filter-fn filter-name]
   (let [view-name (str (.getName model) "." filter-name)
	 view (clone-model model view-name)]
     (select-filter model filter-fn view "dummy"))))

(def blue-whales (select-filter whales #(= "blue whale" (.getType %)) "type='blue whale'"))
(insert *metamodel* blue-whales)

;;----------------------------------------
;; aggregation
;;----------------------------------------

(defn make-aggregator [model key-name key-val aggregator attribute]
  (let [attribute-type (first (aggregator-metadata aggregator))
	key key-name
	key-type (type key-val)
	version "version"
	version-type (Integer/TYPE)
	model-name (str (.getName model) "." (.toString aggregator) "(" attribute ")")
	class-name (.toString (gensym (str "org.dada.demo.tmp.Aggregator." aggregator)))
	metadata (make-metadata class-name key-type key attribute-type attribute)
	aggregator-fn (eval (symbol (str "make-" aggregator "-aggregator")))]
    (aggregator-fn metadata model-name key-val attribute)))

(defn select-aggregate
  "apply an aggregator view to a model"
  [model key-name key-val aggregator attribute]
  (connect model (make-aggregator model key-name key-val aggregator attribute)))


(def blue-whales-sum-length (select-aggregate blue-whales "type" "blue whale" 'sum "length"))
(def blue-whales-count-length (select-aggregate blue-whales "type" "blue whale" 'count "length"))
(def blue-whales-average-length (select-aggregate blue-whales "type" "blue whale" 'average "length"))

(insert *metamodel* blue-whales-sum-length)
(insert *metamodel* blue-whales-count-length)
(insert *metamodel* blue-whales-average-length)

;;----------------------------------------
;; combined filtration and aggregation
;; "combined weight of all killer whales"
;;----------------------------------------


(let [filter-attribute-name "type"
      filter-attribute-value "killer whale"
      aggregator-attribute-name "weight"
      aggregator-symbol 'count ;; 'sum, 'average, 'count

      filter-attribute-accessor (make-lambda-getter Whale String filter-attribute-name)
      filtration (select-filter
		  whales #(= filter-attribute-value (filter-attribute-accessor %)) (str filter-attribute-name "='" filter-attribute-value "'"))
      aggregation (select-aggregate 
		   filtration
		   filter-attribute-name filter-attribute-value aggregator-symbol aggregator-attribute-name)]
  (insert *metamodel* filtration)
  (insert *metamodel* aggregation))

;; TODO - aggregated column's name and type depends on aggregator, not original column type

;;----------------------------------------

  


;; THOUGHTS

;; primitives: a select is a combination of : filter, transform,
;; group, aggregate and join functions...

;; a (group) is a (filter) followed by a ?homogenous/symmetric join? ???

;; not sure yet about how a heterogenous/assymetric join works
