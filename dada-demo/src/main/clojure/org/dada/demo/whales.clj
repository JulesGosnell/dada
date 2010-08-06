(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use [org.dada core web])
 (:use [org.dada core dsl])
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

(def #^Metadata whale-metadata (custom-metadata "org.dada.demo.whales.Whale" Object [:id] [:version] int-version-comparator md-attributes))
;;(def #^Metadata whale-metadata (record-metadata [:id] md-attributes))

;;--------------------------------------------------------------------------------

(def #^Model whales-model (model "Whales" whale-metadata))

(start-server)

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

;; create some whales...

(def num-whales 1000)

(def some-whales (doall (pmap (fn [id] (whale id)) (range num-whales))))

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

(do (time (doall (pmap (fn [whale] (insert whales-model whale)) some-whales))) nil)

(println "LOADED")


;;--------------------------------------------------------------------------------

(def all-whales (metamodel whales-model))

(def #^Collection some-years (map #(Date. % 0 1) (range num-years)))
(def #^NavigableSet years (TreeSet. some-years))

(defn by-year [time] (list (or (.lower years time) time)))

;; (? [] all-whales)

;; (? [(union)] all-whales)
;; (? [(ccount)] all-whales)
;; (? [(sum :weight)] all-whales)

;; (? [(split :type)] all-whales)

;; (? [(ccount)(ccount)] all-whales)
;; (? [(union)(union)] all-whales)
;; (? [(ccount)(union)] all-whales)
;; (? [(sum :weight)(union)] all-whales)
;; (? [(union)(ccount)] all-whales)
;; (? [(union)(split :type)] all-whales)
;; (? [(ccount)(split :type)] all-whales)
;; (? [(split :time)(split :type)] all-whales)

;; (? [(ccount)(split :time)(split :type)] all-whales)
;; (? [(union)(split :time)(split :type)] all-whales)
;; (? [(split :length)(split :time)(split :type)] all-whales)

;; (? [(split :type nil [(ccount)])] all-whales)
;; (? [(split :type nil [(split :time)])] all-whales)
;; (? [(split :type nil [(split :time nil [(split :length)])])] all-whales)

;; (? [(split :type nil [(ccount)(split :time)])] all-whales)
;; (? [(split :type nil [(split :time nil)(split :length)])] all-whales)

;; (? [(split :type nil [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time by-year)])] all-whales)

;; (? [(union "count/type/year")(split :type nil [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time by-year)])] all-whales)

;;(? [(union "count/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (count-value-key nil)))(ccount)(split :ocean )])] all-whales)

;; (? [(union "sum(weight)/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (sum-value-key :weight)))(sum :weight)(split :ocean )])] all-whales)

;;(? [(union "count/ocean/type")(split :ocean nil [(pivot :type types (keyword (count-value-key nil)))(ccount)(split :type )])] all-whales)

;; (? [(union "sum(weight)/ocean/type")(split :ocean nil [(pivot :type types (keyword (sum-value-key :weight)))(sum :weight)(split :type )])] all-whales)

;; (def subquery [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time by-year)])

;; (? [(split :ocean nil [(union)(split :type nil subquery)])] all-whales)

;; (? [(union)(split :ocean nil subquery)] all-whales)

;; (? [
;;      (pivot :time years (keyword (count-value-key nil)))
;;      (split :time by-year [(ccount)])] all-whales)

;; (? [(ccount)] all-whales)

;; these queries seem to just be too big to run with a resonably sized dataset on my home box ...

;; (? [(split :reporter nil [(split :ocean nil [(union)(split :type nil subquery)])])] all-whales)

;; (? [(split :reporter nil [(union)(split :ocean nil subquery)])] all-whales)

;; (? [(split :reporter nil [
;; 			   (pivot :time years (keyword (count-value-key nil)))
;; 			   (split :time by-year [(ccount)])])] all-whales)


;;--------------------------------------------------------------------------------
;; fix so that a split/pivot can handle undeclared keys

;;--------------------------------------------------------------------------------

(start-client)
(start-jetty 8080)

;;(? [(split :type)] all-whales)
;;(? [(split :ocean nil [(ccount)])] all-whales)
;;(? [(pivot :ocean oceans (keyword (count-value-key nil)))(ccount)(split :ocean)] all-whales)

(? [(union "count/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (count-value-key nil)))(ccount)(split :ocean )])] all-whales)
(? [(union "count/ocean/type")(split :ocean nil [(pivot :type types (keyword (count-value-key nil)))(ccount)(split :type )])] all-whales)

(? [(union "sum(weight)/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (sum-value-key :weight)))(sum :weight)(split :ocean )])] all-whales)
(? [(union "sum(weight)/ocean/type")(split :ocean nil [(pivot :type types (keyword (sum-value-key :weight)))(sum :weight)(split :type )])] all-whales)

;;--------------------------------------------------------------------------------

(import org.dada.core.Attribute)

(defmulti mutate (fn [#^Attribute attribute datum] (.getKey attribute)))

(defmethod mutate :version [attribute datum] (inc (.get (.getGetter attribute) datum)))
(defmethod mutate :time [attribute datum] (Date.))
(defmethod mutate :reporter [attribute datum] (rnd reporters))
(defmethod mutate :ocean [attribute datum] (rnd oceans))
(defmethod mutate :length [attribute datum] (+ 1 (.get (.getGetter attribute) datum)))
(defmethod mutate :weight [attribute datum] (+ 1 (.get (.getGetter attribute) datum)))
(defmethod mutate :default [attribute datum] (.get (.getGetter attribute) datum))

(.start
 (Thread.
  (fn []
      (doall
       (map
	(fn [n]
	    (let [model whales-model
		  metadata (.getMetadata model)
		  old-value (rnd (.getExtant (.getData model)))
		  new-value (.create (.getCreator metadata) (into-array Object (map #(mutate % old-value) (.getAttributes metadata))))]
	      (update model old-value new-value)
	      (Thread/sleep 500)))
	(repeat 0))))))

;;--------------------------------------------------------------------------------

;; ;; create a client to a remote session manager
;; (def #^org.dada.core.SessionManager sm (.client *external-session-manager-service-factory* "SessionManager"))
;; ;; get the metadata for a remote model
;; (.getMetadata sm "Whales")
;; ;; register a View
;; (.registerView sm "Whales" (proxy [org.dada.core.View java.io.Serializable][](update [& rest] (println "UPDATE:" rest))))
