(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use
  [clojure set]
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
   Metadata$VersionComparator
   Model
   ])
 )

;;--------------------------------------------------------------------------------

(defn rnd [seq] (nth seq (rand-int (count seq))))

;;--------------------------------------------------------------------------------
;; Oceans
;;--------------------------------------------------------------------------------

(def #^Metadata ocean-metadata
  (record-metadata
   [:id] [:version]
   (proxy [Metadata$VersionComparator][](compareTo [lhs rhs] (- (.version lhs)(.version rhs))))
   [[:id        String         false]
    [:version   (Integer/TYPE) true]
    [:area      (Integer/TYPE) true] ;; may grow and shrink with ice
    [:max-depth (Integer/TYPE) true]]))

(def ocean-creator (.getCreator ocean-metadata))
(defn make-ocean [& args] (.create ocean-creator (into-array Object args)))

(def #^Model oceans-model (model "Oceans" ocean-metadata))

(insert *metamodel* oceans-model)

(insert-n
 oceans-model
 [(make-ocean "arctic"   0 0        17880)
  (make-ocean "atlantic" 0 41100000 28232)
  (make-ocean "indian"   0 28350000 23808)
  (make-ocean "pacific"  0 64100000 35797)
  (make-ocean "southern" 0 0        23737)])

;;--------------------------------------------------------------------------------
;; Whales
;;--------------------------------------------------------------------------------

(def #^Metadata whale-metadata
  (record-metadata
   [:id] [:version]
   (proxy [Metadata$VersionComparator][](compareTo [lhs rhs] (- (.version lhs)(.version rhs))))
   [[:id       (Integer/TYPE)       false]
    [:version  (Integer/TYPE)       true]
    [:time     Date                 true]
    [:reporter String               true]
    [:type     clojure.lang.Keyword false]	;a whale cannot change type
    [:ocean    String               true]
    [:length   (Float/TYPE)         true]
    [:weight   (Float/TYPE)         true]]))

(def whale-creator (.getCreator whale-metadata))
(defn make-whale [& args] (.create whale-creator (into-array Object args)))

(def #^Model whales-model (model "Whales" whale-metadata))

(insert *metamodel* whales-model)

;; (def some-whales
;;   [(make-whale 0 0 (Date. 0 1 1) "jules" "blue whale" "arctic" 100 100)
;;    (make-whale 1 0 (Date. 0 1 1) "jules" "blue whale" "indian" 200 100)
;;    (make-whale 2 0 (Date. 0 1 1) "jules" "gray whale" "arctic" 100 100)
;;    (make-whale 3 0 (Date. 0 1 1) "jules" "gray whale" "indian" 200 100)
;;    (make-whale 4 0 (Date. 1 1 1) "jules" "blue whale" "arctic" 100 100)
;;    (make-whale 5 0 (Date. 1 1 1) "jules" "blue whale" "indian" 200 100)
;;    (make-whale 6 0 (Date. 1 1 1) "jules" "gray whale" "arctic" 100 100)
;;    (make-whale 7 0 (Date. 1 1 1) "jules" "gray whale" "indian" 200 100)])


(def whale-hierarchy
  (let [h (make-hierarchy)
	h (derive h :cetacea :mysticeti)     ; baleen whales
	h (derive h  :mysticeti :balaenidae) ;right and bowhead whales
	h (derive h   :balaenidae :bowhead-whale)
	h (derive h   :balaenidae :north-atlantic-right-whale)
	h (derive h   :balaenidae :north-pacific-right-whale)
	h (derive h   :balaenidae :southern-right-whale)
	h (derive h  :mysticeti :balaenopteridae) ;rorqual whales
	h (derive h   :balaenopteridae :antarctic-minke-whale)
	h (derive h   :balaenopteridae :omuras-whale)
	h (derive h   :balaenopteridae :blue-whale)
	h (derive h   :balaenopteridae :brydes-whale)
	h (derive h   :balaenopteridae :common-minke-whale)
	h (derive h   :balaenopteridae :edens-whale)
	h (derive h   :balaenopteridae :fin-whale)
	h (derive h   :balaenopteridae :humpback-whale)
	h (derive h   :balaenopteridae :sei-whale)
	h (derive h  :mysticeti :eschrichtiidae)
	h (derive h   :eschrichtiidae :gray-whale)
	h (derive h  :mysticeti :neobalaenidae)
	h (derive h   :neobalaenidae :pygmy-right-whale)
	h (derive h :cetacea :odontoceti)      ;toothed whales
	h (derive h  :odontoceti :delphinidae) ;dolphins
	h (derive h   :delphinidae :bottlenose-dolphin)
	h (derive h   :delphinidae :false-killer-whale)
	h (derive h   :delphinidae :killer-whale)
	h (derive h   :delphinidae :melon-headed-whale)
	h (derive h   :delphinidae :pilot-whale)
	h (derive h   :delphinidae :spinner-dolphin)
	h (derive h  :odontoceti :iniidae) ;river dolphins
	h (derive h   :iniidae :amazon-river-dolphin)
	h (derive h  :odontoceti :monodontidae)
	h (derive h   :monodontidae :beluga-whale)
	h (derive h   :monodontidae :narwhal)
	h (derive h  :odontoceti :phocoenidae)	; porpoises
	h (derive h   :phocoenidae :harbour-porpoise)
	h (derive h  :odontoceti :physeteridae) ; sperm whales
	h (derive h   :physeteridae :sperm-whale)
	h (derive h   :physeteridae :dwarf-sperm-whale)
	h (derive h   :physeteridae :pygmy-sperm-whale)
	h (derive h  :odontoceti :zyphidae) ;beaked whale
	h (derive h   :zyphidae :beaked-whale)
	]
    h))

(do

  (def types (apply vector
		    (clojure.set/difference
		     (apply hash-set (keys (whale-hierarchy :descendants)))
		     (apply hash-set (keys (whale-hierarchy :ancestors))))))

  (def oceans ["arctic"
	       "atlantic"
	       "indian"
	       "pacific"
	       "southern"])

  (def reporters ["jules"
		  "andy"
		  "ian"
		  "steve"
		  "mitesh"
		  "ameen"])

  (def num-years 10)

  (def max-weight 172) ;; metric tons
  (def max-length 32.9) ;; metres

  (let [max-length-x-100 (* max-length 100)
	max-weight-x-100 (* max-weight 100)]

    (defn random-whale [id]
      (make-whale
       id
       0
       (Date. (rand-int num-years)
	      (rand-int 12)
	      (+ (rand-int 28) 1))
       (rnd reporters)
       (rnd types)
       (rnd oceans)
       (float (/ (rand-int max-length-x-100) 100))
       (float (/ (rand-int max-weight-x-100) 100)))))


  (def num-whales 50)

  (def some-whales (doall (pmap (fn [id] (random-whale id)) (range num-whales))))
  )

(do (time (doall (pmap (fn [whale] (insert whales-model whale)) some-whales))) nil)

;;--------------------------------------------------------------------------------
;; A Join
;;--------------------------------------------------------------------------------

(def #^Metadata join-metadata
  (record-metadata
   [:id] [:version]
   (proxy [Metadata$VersionComparator][](compareTo [lhs rhs] (- (.version lhs)(.version rhs))))
   (list
    [:id        (Integer/TYPE)       false]
    [:version   (Integer/TYPE)       true]
    [:type      clojure.lang.Keyword false]
    [:length    (Integer/TYPE)       true]
    [:weight    (Integer/TYPE)       true]
    [:ocean     String               true]
    [:ocean-area      (Integer/TYPE) true] ;; may grow and shrink with ice
    [:ocean-max-depth (Integer/TYPE) true]
    )))

(def join-creator (.getCreator join-metadata))
(defn make-join [& args] (.create join-creator (into-array Object args)))

(def joins-model
     (JoinModel.
      "WhalesAndOceans"
      join-metadata
      whales-model
      {:ocean oceans-model}
      (fn [id version whale [[ocean]]]
	(let [[type length weight] (if whale [(.type whale)(.length whale)(.weight whale)][nil 0 0])
	      [ocean ocean-max-depth ocean-area] (if ocean [(.id ocean)(.max_minus_depth ocean)(.area ocean)] [nil 0 0])]
	  (make-join id version type length weight ocean ocean-area ocean-max-depth)
	  ))))

(insert *metamodel* joins-model)

;;--------------------------------------------------------------------------------
;; Animation
;;--------------------------------------------------------------------------------

(defmulti mutate (fn [#^Attribute attribute datum] (.getKey attribute)))

;; whale attributes
(defmethod mutate :version   [attribute datum] (inc (.get (.getGetter attribute) datum)))
(defmethod mutate :time      [attribute datum] (Date.))
(defmethod mutate :reporter  [attribute datum] (rnd reporters))
(defmethod mutate :ocean     [attribute datum] (rnd oceans))
(defmethod mutate :length    [attribute datum] (+ 1 (.get (.getGetter attribute) datum)))
(defmethod mutate :weight    [attribute datum] (+ 1 (.get (.getGetter attribute) datum)))
(defmethod mutate :default   [attribute datum] (.get (.getGetter attribute) datum))
;; ocean attributes
(defmethod mutate :max-depth [attribute datum] (int (/ (* (.get (.getGetter attribute) datum) (+ 98 (rand-int 5))) 100))) ;; +/- 2%
(defmethod mutate :area      [attribute datum] (int (/ (* (.get (.getGetter attribute) datum) (+ 98 (rand-int 5))) 100))) ;; +/- 2%

(defn mutate-datum [^Model model]
  (let [metadata (.getMetadata model)
	old-value (rnd (.getExtant (.getData model)))
	new-value (.create (.getCreator metadata) (into-array Object (map #(mutate % old-value) (.getAttributes metadata))))]
    (update model old-value new-value)))

(defn animate [model delay]
  (.start (Thread. (fn [] (doall (map (fn [n] (mutate-datum model)(Thread/sleep delay)) (repeat 0)))))))

(def animation-delay 500)

(if (not *compile-files*)
  (do
    (animate whales-model animation-delay)
    (animate oceans-model animation-delay)))

;;--------------------------------------------------------------------------------
;; Join scratch area
;;--------------------------------------------------------------------------------

(if false
  (do

    (inspect (? (dfrom "Whales")))
    (inspect (? (dfrom "Oceans")))
    (inspect (? (dfrom "WhalesAndOceans")))

    (insert oceans-model (make-ocean "arctic"   1000000 99999999 17880))
    (insert oceans-model (make-ocean "southern" 1000000  10000000 23737))

    (insert whales-model (make-whale 10000 4000000 (Date. 0 1 1) "jules" "blue whale" "seaworld" 100 100))
    (delete whales-model (make-whale 10000 5000000 (Date. 0 1 1) "jules" "blue whale" "seaworld" 100 100))
    (.find whales-model 10000)

    ))

;;--------------------------------------------------------------------------------
;; DSL experimentation
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

    ;; we're only interested in toothed whales
    (inspect (? (dsplit :type (fn [type] (if (isa? whale-hierarchy :odontoceti type) [:odontoceti] []))) (dfrom "Whales")))

    ;; we're only interested in rorquals
    (inspect (? (dsplit :type (fn [type] (if (isa? whale-hierarchy :balaenopteridae type) [:balaenopteridae] []))) (dfrom "Whales")))

    ;; split by family
    (do
      (def whale-hierarchy-children
	(reduce (fn [reduction [key vals]] (reduce (fn [reduction val] (conj reduction [val key])) reduction vals)) {} (whale-hierarchy :parents)))
      
      (def whale-type-to-family
	(reduce (fn [reduction type] (conj reduction [type (whale-hierarchy-children (whale-hierarchy-children type))])) {} types))
      
      (inspect (? (dsplit :type (fn [type] [(whale-type-to-family type)])) (dfrom "Whales"))))
    
    ;; split by ocean
    (inspect (? (dsplit :ocean)(dfrom "Whales")));; TODO - nattable delete not working

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

(if false
  (do
    
    ;;(start-server)

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

    ))

;; rescued from swt/new.clj

;; (if false
;;   (do
;;     ;; set up a test model

;;     (def whale-data
;; 	 ;;[id, version, type, ocean, length]
;; 	 [[0 0 :blue :atlantic 100]
;; 	  [1 0 :blue :pacific  100]
;; 	  [2 0 :grey :atlantic 50]
;; 	  [3 0 :grey :pacific  50]])

;;     (def whales (model "Whales" (seq-metadata (count (first whale-data)))))
;;     (insert *metamodel* whales)
;;     (insert-n whales whale-data)

;;     (inspect (? (dfrom "Whales")))
;;     (inspect (? (dcount)(dfrom "Whales")))
;;     (inspect (? (dsplit 2)(dfrom "Whales")))
;;     (inspect (? (dcount)(dsplit 2)(dfrom "Whales")))
;;     (inspect (? (dsum 4)(dsplit 2)(dfrom "Whales")))
;;     (inspect (? (dcount)(dsplit 2)(dsplit 3)(dfrom "Whales")))
;;     (inspect (? (dunion)(dsplit 2)(dfrom "Whales")))
;;     (inspect (? (dcount)(dunion)(dsplit 2)(dfrom "Whales")))
;;     (inspect (? (dcount)(dunion)(dsplit 2)(dsplit 3)(dfrom "Whales")))
;;     (inspect (? (dsplit 3)(dsplit 2)(dfrom "Whales")))
;;     (inspect (? (dunion)(dsplit 3)(dsplit 2)(dfrom "Whales")))
;;     (inspect (? (dsplit 2 list [(dsplit 3)])(dfrom "Whales")))
;;     (inspect (? (dunion)(dsplit 2 list [(dsplit 3)])(dfrom "Whales")))
;;     (inspect (? (dsplit 2 list [(dunion)(dsplit 3)])(dfrom "Whales")))
    
;;     ;; needs tidying up...
;;     (inspect (? (dfrom "Whales")))
;;     (inspect (? (dunion)(dsplit 2)(dsplit 3)(dfrom "Whales")))
;;     (inspect (? (dsplit 2 list [(dunion)(dsplit 3)])(dfrom "Whales")))

;;     ;; needs fixing
;;     ;;(inspect (? (dunion)(dsplit 2 list [(dunion)(dsplit 3)])(dfrom "Whales")))

;;     ;; need a pivot demo...
;;     ))

;; (? (dunion)(dsplit :ocean nil [(pivot :type org.dada.demo.whales/types (keyword (sum-value-key :weight)))(dsum :weight)(split :type )])(from "Whales"))
;; (? (dunion)(split :type nil [(pivot :ocean org.dada.demo.whales/oceans (keyword (sum-value-key :weight)))(sum :weight)(split :ocean )]) (from "Whales"))

;;--------------------------------------------------------------------------------

;; ;; create a client to a remote session manager
;; (def #^org.dada.core.SessionManager sm (.client *external-session-manager-service-factory* "SessionManager"))
;; ;; get the metadata for a remote model
;; (.getMetadata sm "Whales")
;; ;; register a View
;; (.registerView sm "Whales" (proxy [org.dada.core.View java.io.Serializable][](update [& rest] (println "UPDATE:" rest))))

;;--------------------------------------------------------------------------------
