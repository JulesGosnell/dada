(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use 
  [org.dada core]
  [org.dada.core dql]
  [org.dada.swt new])
 (:import
  [java.util
   Collection
   Date
   NavigableSet
   TreeSet
   ]
  [org.dada.core
   Attribute
   Batcher
   Creator
   JoinModel
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

;;(start-server)

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

(def num-whales 100)

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

(def #^Collection some-years (map #(Date. % 0 1) (range num-years)))
(def #^NavigableSet years (TreeSet. some-years))

(defn by-year [time] (list (or (.lower years time) time)))

;; (? (from "Whales"))

;; (? (union)(from "Whales"))
;; (? (ccount)(from "Whales"))
;; (? (sum :weight)(from "Whales"))

;; (? (split :type)(from "Whales"))

;; (? (ccount)(ccount)(from "Whales"))
;; (? (union)(union)(from "Whales"))
;; (? (ccount)(union)(from "Whales"))
;; (? (sum :weight)(union)(from "Whales"))
;; (? (union)(ccount)(from "Whales"))
;; (? (union)(split :type)(from "Whales"))
;; (? (ccount)(split :type)(from "Whales"))
;; (? (split :time)(split :type)(from "Whales"))

;; (? (ccount)(split :time)(split :type)(from "Whales"))
;; (? (union)(split :time)(split :type)(from "Whales"))
;; (? (split :length)(split :time)(split :type)(from "Whales"))

;; (? (split :type nil [(ccount)]) (from "Whales"))
;; (? (split :type nil [(split :time)]) (from "Whales"))
;; (? (split :type nil [(split :time nil [(split :length)])]) (from "Whales"))

;; (? (split :type nil [(ccount)(split :time)]) (from "Whales"))
;; (? (split :type nil [(split :time nil)(split :length)]) (from "Whales"))

;; (? (split :type nil [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time by-year)]) (from "Whales"))

;; (? (union "count/type/year")(split :type nil [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time by-year)])(from "Whales"))

;;(? (union "count/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (count-value-key nil)))(ccount)(split :ocean )])(from "Whales"))

;; (? (union "sum(weight)/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (sum-value-key :weight)))(sum :weight)(split :ocean )])(from "Whales"))

;;(? (union "count/ocean/type")(split :ocean nil [(pivot :type types (keyword (count-value-key nil)))(ccount)(split :type )])(from "Whales"))

;; (? (union "sum(weight)/ocean/type")(split :ocean nil [(pivot :type types (keyword (sum-value-key :weight)))(sum :weight)(split :type )])(from "Whales"))

;; (def subquery [(pivot :time years (keyword (count-value-key nil)))(ccount)(split :time by-year)])

;; (? (split :ocean nil [(union)(split :type nil subquery)])(from "Whales"))

;; (? (union)(split :ocean nil subquery)(from "Whales"))

;; (? 
;;      (pivot :time years (keyword (count-value-key nil)))
;;      (split :time by-year [(ccount)])] (from "Whales"))

;; (? (ccount)(from "Whales"))

;; these queries seem to just be too big to run with a resonably sized dataset on my home box ...

;; (? (split :reporter nil [(split :ocean nil [(union)(split :type nil subquery)])])(from "Whales"))

;; (? (split :reporter nil [(union)(split :ocean nil subquery)])(from "Whales"))

;; (? (split :reporter nil [
;; 			   (pivot :time years (keyword (count-value-key nil)))
;; 			   (split :time by-year [(ccount)])])] (from "Whales"))


;;--------------------------------------------------------------------------------
;; fix so that a split/pivot can handle undeclared keys

;;--------------------------------------------------------------------------------

;;(? (split :type)(from "Whales"))
;;(? (split :ocean nil [(ccount)])(from "Whales"))
;;(? (pivot :ocean oceans (keyword (count-value-key nil)))(ccount)(split :ocean)(from "Whales"))


(if false
  (do
    
    ;; all whales
    (inspect (? (dfrom "Whales")))

    ;; count all whales
    (inspect (? (dcount)(dfrom "Whales")))

    ;; sum length of all whales
    (inspect (? (dsum :length)(dfrom "Whales")))

    ;; split by ocean
    (inspect (? (dsplit :ocean)(dfrom "Whales")))

    ;; flat split by type then ocean
    (inspect (? (dsplit :ocean)(dsplit :type)(dfrom "Whales")))

    ;; nested split by type then ocean
    (inspect (? (dsplit :type list [(dsplit :ocean)])(dfrom "Whales")))

    (inspect (? (dsplit :type list [(dunion)(dsplit :ocean)])(dfrom "Whales")))

    ;; sum weights per ocean
    (inspect (? (dsum :weight)(dsplit :ocean)(dfrom "Whales")))

    ;; summarise weights per ocean
    (inspect (? (dunion)(dsum :weight)(dsplit :ocean)(dfrom "Whales")))
    ;; summarise weights per type
    (inspect (? (dunion)(dsum :weight)(dsplit :type)(dfrom "Whales")))

    ;; pivot weights per ocean summary
    (inspect (? (dpivot :ocean ["arctic" "atlantic" "indian" "pacific" "southern"] (keyword "sum(:weight)")) (dsum :weight) (dsplit :ocean) (dfrom "Whales")))

    ;; for each type - pivot weights per ocean summary
    (inspect (? (dsplit :type list [(dpivot :ocean ["arctic" "atlantic" "indian" "pacific" "southern"] (keyword "sum(:weight)")) (dsum :weight) (dsplit :ocean)]) (dfrom "Whales")))

    (inspect (? (dsplit :type list [(dunion)(dpivot :ocean ["arctic" "atlantic" "indian" "pacific" "southern"] (keyword "sum(:weight)")) (dsum :weight) (dsplit :ocean)]) (dfrom "Whales")))

;; TODO
;; split multiple dimensions at same time...
;; reduce multiple columns at same time...
;; rethink pivot 
;; support adding/deleting UI rows
;; support updating UI rows

  ;;(? (union "count/type/ocean")(split :type nil [(pivot :ocean oceans (keyword (count-value-key nil)))(ccount)(split :ocean )]) (from "Whales"))
  ;;(? (union "count/ocean/type")(split :ocean nil [(pivot :type types (keyword (count-value-key nil)))(ccount)(split :type )]) (from "Whales"))

  ;; weight/ocean/type
  ;;(inspect (? (dsplit :type nil [(dpivot :ocean oceans (keyword (sum-value-key :weight)))(dsum :weight)(dsplit :ocean )])(dfrom "Whales")))
  
  ;; weight/type/ocean
  ;;(inspect (? (dsplit :ocean nil [(dpivot :type types (keyword (sum-value-key :weight)))(dsum :weight)(dsplit :type )])(dfrom "Whales")))

  ))
;;--------------------------------------------------------------------------------

(defmulti mutate (fn [#^Attribute attribute datum] (.getKey attribute)))

(defmethod mutate :version [attribute datum] (inc (.get (.getGetter attribute) datum)))
(defmethod mutate :time [attribute datum] (Date.))
(defmethod mutate :reporter [attribute datum] (rnd reporters))
(defmethod mutate :ocean [attribute datum] (rnd oceans))
(defmethod mutate :length [attribute datum] (+ 1 (.get (.getGetter attribute) datum)))
(defmethod mutate :weight [attribute datum] (+ 1 (.get (.getGetter attribute) datum)))
(defmethod mutate :default [attribute datum] (.get (.getGetter attribute) datum))

(defn create-mutation []
  (let [model whales-model
	metadata (.getMetadata model)
	old-value (rnd (.getExtant (.getData model)))
	new-value (.create (.getCreator metadata) (into-array Object (map #(mutate % old-value) (.getAttributes metadata))))]
    ;;(println "MUTATING" old-value "->" new-value)
    (update model old-value new-value)))

(defn start-model []
  (.start (Thread. (fn [] (doall (map (fn [n] (create-mutation)(Thread/sleep 500)) (repeat 0)))))))

(if (not *compile-files*) (start-model))

;;--------------------------------------------------------------------------------

;; ;; create a client to a remote session manager
;; (def #^org.dada.core.SessionManager sm (.client *external-session-manager-service-factory* "SessionManager"))
;; ;; get the metadata for a remote model
;; (.getMetadata sm "Whales")
;; ;; register a View
;; (.registerView sm "Whales" (proxy [org.dada.core.View java.io.Serializable][](update [& rest] (println "UPDATE:" rest))))

;;--------------------------------------------------------------------------------
;; lets play with joins
;;--------------------------------------------------------------------------------

;; I want to know that max-depth that each whale might swim to (i.e. of the ocean in which it finds itself)

;;--------------------------------------------------------------------------------
;; we need an Oceans model

(def ocean-attributes
     (list
      [:id        String         false]
      [:version   (Integer/TYPE) true]
      [:area      (Integer/TYPE) true] ;; may grow and shrink with ice
      [:max-depth (Integer/TYPE) true]
      ))

(def #^Metadata ocean-metadata (custom-metadata "org.dada.demo.whales.Ocean" Object [:id] [:version] int-version-comparator ocean-attributes))

(def #^Model oceans-model (model "Oceans" ocean-metadata))
(insert *metamodel* oceans-model)

(insert-n
 oceans-model
 (let [creator (.getCreator ocean-metadata)]
   [
    (.create creator (into-array Object ["arctic"   0 0        17880]))
    (.create creator (into-array Object ["atlantic" 0 41100000 28232]))
    (.create creator (into-array Object ["indian"   0 28350000 23808]))
    (.create creator (into-array Object ["pacific"  0 64100000 35797]))
    (.create creator (into-array Object ["southern" 0 0        23737]))
    ]))

;;--------------------------------------------------------------------------------

;;(inspect (? (dfrom "Whales")))
;;(inspect (? (dfrom "Oceans")))

;; try updating an ocean
(insert oceans-model (.create (.getCreator ocean-metadata) (into-array Object ["arctic"   1 10000000 17880])))
(insert oceans-model (.create (.getCreator ocean-metadata) (into-array Object ["southern" 1 10000000 23737])))

;; we need a ModelView that listens to Whales AND Oceans and joins them together...

(def join-model
     (JoinModel.
      "TestJoin"
      :ocean
      [:id]
      [:version :time :reporter :type :ocean :length :weight]
      whales-model
      []
      [:max-depth]
      oceans-model))