(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use [org.dada core dsl])
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

;;--------------------------------------------------------------------------------

(if true
  (do
    (start-server)
    (start-client)))

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

(def reporters [
		"jules"
		"andy"
		"ian"
		"steve"
		"mitesh"
		"ameen"
		])

(def num-years 10)

(def max-weight 172) ;; metric tons
(def max-length 32.9) ;; metres

(defn rnd [seq] (nth seq (rand-int (count seq))))



;;--------------------------------------------------------------------------------

(def md-attributes
     (list
      [:id       (Integer/TYPE) false]
      [:version  (Integer/TYPE) true]
      [:time     Date           true]
      [:reporter String         true]
      [:type     String         false]	;a whale cannot change type
      [:ocean    String         true]
      [:length   (Float/TYPE)   true]
      [:weight   (Float/TYPE)   true]
      ))

(def #^Metadata whale-metadata (custom-metadata "org.dada.demo.whales.Whale" Object [:id] md-attributes))
;;(def #^Metadata whale-metadata (record-metadata [:id] md-attributes))

;;--------------------------------------------------------------------------------

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
       (rnd reporters)
       (rnd types)
       (rnd oceans)
       (/ (rand-int max-length-x-100) 100)
       (/ (rand-int max-weight-x-100) 100)]))))

;;--------------------------------------------------------------------------------

(def all-whales (metamodel whales-model))

(def #^Collection some-years (map #(Date. % 0 1) (range num-years)))
(def #^NavigableSet years (TreeSet. some-years))

(defn by-year [time] (list (or (.lower years time) time)))

;; (?2 [] all-whales)

;; (?2 [(union)] all-whales)
(?2 [(ccount)] all-whales)
;; (?2 [(sum :weight)] all-whales)

;; (?2 [(split :type)] all-whales)

;; (?2 [(ccount)(ccount)] all-whales)
;; (?2 [(union)(union)] all-whales)
;; (?2 [(ccount)(union)] all-whales)
;; (?2 [(sum :weight)(union)] all-whales)
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

;; (?2 [(split :type nil [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time by-year)])] all-whales)

;; (?2 [(union "count/type/year")(split :type nil [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time by-year)])] all-whales)

(?2 [(union "count/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (count-value-key nil)))(ccount)(split :ocean )])] all-whales)

;; (?2 [(union "sum(weight)/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (sum-value-key :weight)))(sum :weight)(split :ocean )])] all-whales)

;;(?2 [(union "count/ocean/type")(split :ocean nil [(pivot :type types (keyword (count-value-key nil)))(ccount)(split :type )])] all-whales)

;; (?2 [(union "sum(weight)/ocean/type")(split :ocean nil [(pivot :type types (keyword (sum-value-key :weight)))(sum :weight)(split :type )])] all-whales)

;; (def subquery [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time by-year)])

;; (?2 [(split :ocean nil [(union)(split :type nil subquery)])] all-whales)

;; (?2 [(union)(split :ocean nil subquery)] all-whales)

;; (?2 [
;;      (pivot :time years (keyword (count-value-key nil)))
;;      (split :time by-year [(ccount)])] all-whales)

;; (?2 [(ccount)] all-whales)

;; these queries seem to just be too big to run with a resonably sized dataset on my home box ...

;; (?2 [(split :reporter nil [(split :ocean nil [(union)(split :type nil subquery)])])] all-whales)

;; (?2 [(split :reporter nil [(union)(split :ocean nil subquery)])] all-whales)

;; (?2 [(split :reporter nil [
;; 			   (pivot :time years (keyword (count-value-key nil)))
;; 			   (split :time by-year [(ccount)])])] all-whales)


;;--------------------------------------------------------------------------------

;; create some whales...

(def num-whales 10000)

(def some-whales
     (doall (pmap (fn [id] (whale id)) (range num-whales))))

(def some-whales2
     (let [#^Creator creator (.getCreator whale-metadata)]
       (map
	#(.create creator (into-array Object %))
	(list 
	 [0 0 (Date. 0 1 1) "jules" "blue whale" "arctic" 100 100]
	 [1 0 (Date. 0 1 1) "jules" "blue whale" "indian" 200 100]
	 [2 0 (Date. 0 1 1) "jules" "gray whale" "arctic" 100 100]
	 [3 0 (Date. 0 1 1) "jules" "gray whale" "indian" 200 100]
	 [4 0 (Date. 1 1 1) "jules" "blue whale" "arctic" 100 100]
	 [5 0 (Date. 1 1 1) "jules" "blue whale" "indian" 200 100]
	 [6 0 (Date. 1 1 1) "jules" "gray whale" "arctic" 100 100]
	 [7 0 (Date. 1 1 1) "jules" "gray whale" "indian" 200 100]
	 ))
       ))

(time
 (let [batcher (Batcher. 999 1000 (list whales-model))]
   (doall (pmap (fn [whale] (insert whales-model whale)) some-whales))
   nil))

(println "LOADED")


;;--------------------------------------------------------------------------------
;; fix so that a split/pivot can handle undeclared keys
