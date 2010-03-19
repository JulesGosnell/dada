(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use [org.dada.core])
 (:import [clojure.lang
	   ])
 (:import [java.math
	   ])
 (:import [org.dada.core
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
      :time (Long/TYPE)
      :version (Integer/TYPE)
      :type String
      :length (Float/TYPE)
      :weight (Float/TYPE)))

(def #^Class Whale (apply make-class "org.dada.demo.whales.Whale" Object attributes))
(def #^Metadata whale-metadata (apply metadata Whale :time :version attributes))
(def #^Model whales (model "Whales" whale-metadata))

(def num-whales 10000)
(def start-time (System/currentTimeMillis))

(insert *metamodel* whales)

;; create 10,000 whales and insert them into model
(insert-n
 whales
 (let [#^Creator creator (.getCreator whale-metadata)
       max-length-x-100 (* max-length 100)
       max-weight-x-100 (* max-weight 100)]
   (for [id (range start-time (+ start-time num-whales))]
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
;; demonstrate reduction (sum)
;;----------------------------------------

(insert *metamodel* (do-reduce-count whales "whales"))
(insert *metamodel* (do-reduce-sum whales :length "whales"))
(insert *metamodel* (do-reduce-sum whales :weight "whales"))
(insert *metamodel* (do-reduce-count heavier-whales-length "heavier-whales"))
(insert *metamodel* (do-reduce-sum heavier-whales-length :length "heavier-whales"))
(insert *metamodel* (do-reduce-sum longer-whales-weight :weight "longer-whales"))
(insert *metamodel* (do-reduce-count longer-whales-weight "longer-whales"))

;;----------------------------------------
;; demonstrate splitting [and more reduction)
;;----------------------------------------

(let [type-to-route (apply hash-map (interleave types (range (count types))))
      #^"[Ljava.lang.String;" route-to-type (into-array String types)
      type-count-metadata (class-metadata
       			   "org.dada.demo.whales.TypeCount"
       			   Object
       			   :key
       			   :version
       			   [:key String :version Integer :count Integer])
      type-count-model (model "Whales.count(types)" type-count-metadata)
      ]
  (insert *metamodel* type-count-model)
  (do-split
   whales
   :type
   false
   type-to-route
   #(aget route-to-type #^Integer %)
   (fn [#^Model model value]
       (insert *metamodel* model)
       (let [tcm (do-reduce-count model value type-count-metadata)]
	 (insert *metamodel* tcm)
	 (connect tcm type-count-model)
	 )
       (insert *metamodel* (do-reduce-sum model :length value))
       (insert *metamodel* (do-reduce-sum model :weight value))
       model ; TODO: do we really need to be able to override the model ? should threading go here ?
       )
   ))

;; (do-split
;;    whales
;;    :time
;;    false
;;    #(mod % 10)
;;    #(aget route-to-type #^Integer %)
;;    (fn [#^Model model value]
;;        (insert *metamodel* model)
;;        model)
;;    )

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
