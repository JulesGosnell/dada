(ns 
 ^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.demo.whales
 (:use
  [clojure set]
  [org.dada core]
  [org.dada.core dql]
  [org.dada.demo server])
 (:import
  [clojure.lang
   Keyword]
  [java.util
   Collection
   Date
   NavigableSet
   TreeSet
   ]
  [org.dada.core
   Attribute
   JoinModel
   Model
   ]
  )
 )

;; to use:
;; load-eval this file using dada/bin/clj
;; run dada/bin/client


;;--------------------------------------------------------------------------------
;; utils

(defn rnd [seq] (nth seq (rand-int (count seq))))

;;--------------------------------------------------------------------------------
;; Oceans
;;--------------------------------------------------------------------------------

(defrecord-metadata
    ocean-metadata
    Ocean
    [^{:tag String :primary-key true} id
     ^{:tag int    :version-key true} version
     ^{:tag int}                      area
     ^{:tag int}                      max-depth]
    (fn [^Integer lhs ^Integer rhs] (- (int lhs) (int rhs))))

(def ^Model oceans-model (model "Oceans" ocean-metadata))

(insert *metamodel* oceans-model)

(if (not *compile-files*)
  (insert-n
   oceans-model
   [(Ocean. "arctic"   0 14060000 17880)
    (Ocean. "atlantic" 0 41100000 28232)
    (Ocean. "indian"   0 28350000 23808)
    (Ocean. "pacific"  0 64100000 35797)
    (Ocean. "southern" 0 20330000 23737)]))

;;--------------------------------------------------------------------------------
;; Whales
;;--------------------------------------------------------------------------------

(defrecord-metadata
    whale-metadata
    Whale
    [^{:tag int :primary-key true}   id
     ^{:tag int :version-key true}   version
     ^{:tag Date}                    time
     ^{:tag String}                  reporter
     ^{:tag clojure.lang.Keyword :immutable true} type
     ^{:tag String}                  ocean
     ^{:tag float}                   length
     ^{:tag float}                   weight]
    (fn [^Integer lhs ^Integer rhs] (- (int lhs) (int rhs))))

(def ^Model whales-model (model "Whales" whale-metadata))

(insert *metamodel* whales-model)

;; see http://en.wikipedia.org/wiki/Cetacea#Tree
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
	h (derive h   :delphinidae :commersons-dolphin)
	h (derive h   :delphinidae :chilean-dolphin)
	h (derive h   :delphinidae :heavisides-dolphin)
	h (derive h   :delphinidae :hectors-dolphin)
	h (derive h   :delphinidae :long-beaked-common-dolphin)
	h (derive h   :delphinidae :short-beaked-common-dolphin)
	h (derive h   :delphinidae :arabian-common-dolphin)
	h (derive h   :delphinidae :pygmy-killer-whale)
	h (derive h   :delphinidae :short-finned-pilot-whale)
	h (derive h   :delphinidae :long-finned-pilot-whale)
	h (derive h   :delphinidae :rissos-dolphin)
	h (derive h   :delphinidae :frasers-dolphin)
	h (derive h   :delphinidae :atlantic-white-sided-dolphin)
	h (derive h   :delphinidae :white-beaked-dolphin)
	h (derive h   :delphinidae :peales-dolphin)
	h (derive h   :delphinidae :hourglass-dolphin)
	h (derive h   :delphinidae :pacific-white-sided-dolphin)
	h (derive h   :delphinidae :dusky-dolphin)
	h (derive h   :delphinidae :northern-right-whale-dolphin)
	h (derive h   :delphinidae :southern-right-whale-dolphin)
	h (derive h   :delphinidae :irawaddy-dolphin)
	h (derive h   :delphinidae :australian-snubfin-dolphin)
	h (derive h   :delphinidae :killer-whale)
	h (derive h   :delphinidae :melon-headed-whale)
	h (derive h   :delphinidae :false-killer-whale)
	h (derive h   :delphinidae :tucuxi)
	h (derive h   :delphinidae :costero)
	h (derive h   :delphinidae :pacific-humpback-dolphin)
	h (derive h   :delphinidae :indian-humpback-dolphin)
	h (derive h   :delphinidae :atlantic-humpback-dolphin)
	h (derive h   :delphinidae :pantropical-spotted-dolphin)
	h (derive h   :delphinidae :clymene-dolphin)
	h (derive h   :delphinidae :striped-dolphin)
	h (derive h   :delphinidae :atlantic-spotted-dolphin)
	h (derive h   :delphinidae :spinner-dolphin)
	h (derive h   :delphinidae :rough-toothed-dolphin)
	h (derive h   :delphinidae :indian-ocean-bottlenose-dolphin)
	h (derive h   :delphinidae :common-bottlenose-dolphin)
	h (derive h  :odontoceti :monodontidae)
	h (derive h   :monodontidae :beluga-whale)
	h (derive h   :monodontidae :narwhal)
	h (derive h  :odontoceti :phocoenidae)	; porpoises
	h (derive h   :phocoenidae :finless-porpoise)
	h (derive h   :phocoenidae :spectacled-porpoise)
	h (derive h   :phocoenidae :harbour-porpoise)
	h (derive h   :phocoenidae :vaquita)
	h (derive h   :phocoenidae :burmeisters-porpoise)
	h (derive h   :phocoenidae :dalls-porpoise)
	h (derive h  :odontoceti :physeteridae) ; sperm whales
	h (derive h   :physeteridae :sperm-whale)
	h (derive h   :physeteridae :pygmy-sperm-whale)
	h (derive h   :physeteridae :dwarf-sperm-whale)
	h (derive h  :odontoceti :iniidae) ;river dolphins
	h (derive h   :iniidae :amazon-river-dolphin)
	h (derive h   :iniidae :bolivian-river-dolphin)
	h (derive h   :iniidae :baiji)
	h (derive h   :iniidae :la-plata-dolphin)
	h (derive h   :iniidae :ganges-river-dolphin)
	h (derive h   :iniidae :indus-river-dolphin)
	h (derive h  :odontoceti :zyphidae) ;beaked whale
	h (derive h   :zyphidae :arnouxs-beaked-whale)
	h (derive h   :zyphidae :bairds-beaked-whale)
	h (derive h   :zyphidae :northern-bottlenose-whale)
	h (derive h   :zyphidae :southern-bottlenose-whale)
	h (derive h   :zyphidae :indo-pacific-beaked-whale)
	h (derive h   :zyphidae :sowerbys-beaked-whale)
	h (derive h   :zyphidae :andrews-beaked-whale)
	h (derive h   :zyphidae :hubbs-beaked-whale)
	h (derive h   :zyphidae :blainvilles-beaked-whale)
	h (derive h   :zyphidae :gervais-beaked-whale)
	h (derive h   :zyphidae :ginkgo-toothed-beaked-whale)
	h (derive h   :zyphidae :grays-beaked-whale)
	h (derive h   :zyphidae :hectors-beaked-whale)
	h (derive h   :zyphidae :layards-beaked-whale)
	h (derive h   :zyphidae :trues-beaked-whale)
	h (derive h   :zyphidae :perrins-beaked-whale)
	h (derive h   :zyphidae :pygmy-beaked-whale)
	h (derive h   :zyphidae :stejnegers-beaked-whale)
	h (derive h   :zyphidae :spade-toothed--whale)
	h (derive h   :zyphidae :tasman-beaked-whale)
	h (derive h   :zyphidae :cuviers-beaked-whale)
	]
    h))

(def ref-data
  [
   [:bowhead-whale			"Balaena mysticetus"]
   [:north-atlantic-right-whale		"Eubalaena glacialis"]
   [:north-pacific-right-whale		"Eubalaena japonica"]
   [:southern-right-whale		"Eubalaena australis"]
   [:common-minke-whale			"Balaenoptera acutorostrata"]
   [:antarctic-minke-whale		"Balaenoptera bonaerensis"]
   [:sei-whale				"Balaenoptera borealis"]
   [:brydes-whale			"Balaenoptera brydei"]
   [:edens-whale			"Balaenoptera edeni"]
   [:omuras-whale			"Balaenoptera omurai"]
   [:blue-whale				"Balaenoptera musculus"]
   [:fin-whale				"Balaenoptera physalus"]
   [:humpback-whale			"Megaptera novaeangliae"]
   [:gray-whale				"Eschrichtius robustus"]
   [:pygmy-right-whale			"Caperea marginata"]
   [:commersons-dolphin			"Cephalorhyncus commersonii"]
   [:chilean-dolphin			"Cephalorhyncus eutropia"]
   [:heavisides-dolphin			"Cephalorhyncus heavisidii"]
   [:hectors-dolphin			"Cephalorhyncus hectori"]
   [:long-beaked-common-dolphin         "Delphinus capensis"]
   [:short-beaked-common-dolphin        "Delphinus delphis"]
   [:arabian-common-dolphin		"Delphinus tropicalis"]
   [:pygmy-killer-whale			"Feresa attenuata"]
   [:short-finned-pilot-whale		"Globicephala macrorhyncus"]
   [:long-finned-pilot-whale		"Globicephala melas"]
   [:rissos-dolphin			"Grampus griseus"]
   [:frasers-dolphin			"Lagenodelphis hosei"]
   [:atlantic-white-sided-dolphin       "Lagenorhynchus acutus"]
   [:white-beaked-dolphin		"Lagenorhynchus albirostris"]
   [:peales-dolphin			"Lagenorhynchus australis"]
   [:hourglass-dolphin			"Lagenorhynchus cruciger"]
   [:pacific-white-sided-dolphin        "Lagenorhynchus obliquidens"]
   [:dusky-dolphin			"Lagenorhynchus obscurus"]
   [:northern-right-whale-dolphin       "Lissodelphis borealis"]
   [:southern-right-whale-dolphin       "Lissodelphis peronii"]
   [:irawaddy-dolphin			"Orcaella brevirostris"]
   [:australian-snubfin-dolphin         "Orcaella heinsohni"]
   [:killer-whale			"Orcinus orca"]
   [:melon-headed-whale			"Peponocephala electra"]
   [:false-killer-whale			"Pseudorca crassidens"]
   [:tucuxi				"Sotalia fluviatilis"]
   [:costero				"Sotalia guianensis"]
   [:pacific-humpback-dolphin		"Sousa chinensis"]
   [:indian-humpback-dolphin		"Sousa plumbea"]
   [:atlantic-humpback-dolphin		"Sousa teuszii"]
   [:pantropical-spotted-dolphin        "Stenella attenuata"]
   [:clymene-dolphin			"Stenella clymene"]
   [:striped-dolphin			"Stenella coeruleoalba"]
   [:atlantic-spotted-dolphin		"Stenella frontalis"]
   [:spinner-dolphin			"Stenella longirostris"]
   [:rough-toothed-dolphin		"Steno bredanensis"]
   [:indian-ocean-bottlenose-dolphin    "Tursiops aduncus"]
   [:common-bottlenose-dolphin		"Tursiops truncatus"]
   [:beluga-whale			"Delphinapterus leucas"]
   [:narwhal				"Monodon monoceros"]
   [:finless-porpoise			"Neophocaena phocaenoides"]
   [:spectacled-porpoise		"Phocoena dioptrica"]
   [:harbour-porpoise			"Phocoena phocaena"]
   [:vaquita				"Phocoena sinus"]
   [:burmeisters-porpoise		"Phocoena spinipinnis"]
   [:dalls-porpoise			"Phocoenoides dalli"]
   [:sperm-whale			"Physeter catodon"]
   [:pygmy-sperm-whale			"Kogia breviceps"]
   [:dwarf-sperm-whale			"Kogia sima"]
   [:amazon-river-dolphin		"Inia geoffrensis"]
   [:bolivian-river-dolphin		"Inia boliviensis"]
   [:baiji            			"Lipotes vexillifer"]
   [:la-plata-dolphin			"Pontoporia blainvillei"]
   [:ganges-river-dolphin		"Platanista gangetica"]
   [:indus-river-dolphin		"Platanista minor"]
   [:arnouxs-beaked-whale		"Berardius arnuxii"]
   [:bairds-beaked-whale		"Berardius bairdii"]
   [:northern-bottlenose-whale		"Hyperoodon ampullatus"]
   [:southern-bottlenose-whale		"Hyperoodon planifrons"]
   [:indo-pacific-beaked-whale		"Indopacetus pacificus"]
   [:sowerbys-beaked-whale		"Mesoplodon bidens"]
   [:andrews-beaked-whale		"Mesoplodon bowdoini"]
   [:hubbs-beaked-whale			"Mesoplodon carlhubbsi"]
   [:blainvilles-beaked-whale		"Mesoplodon densirostris"]
   [:gervais-beaked-whale		"Mesoplodon europaeus"]
   [:ginkgo-toothed-beaked-whale        "Mesoplodon ginkgodens"]
   [:grays-beaked-whale			"Mesoplodon grayi"]
   [:hectors-beaked-whale		"Mesoplodon hectori"]
   [:layards-beaked-whale		"Mesoplodon layardii"]
   [:trues-beaked-whale			"Mesoplodon mirus"]
   [:perrins-beaked-whale		"Mesoplodon perrini"]
   [:pygmy-beaked-whale			"Mesoplodon peruvianus"]
   [:stejnegers-beaked-whale		"Mesoplodon stejnegeri"]
   [:spade-toothed--whale		"Mesoplodon traversii"]
   [:tasman-beaked-whale		"Tasmacetus shepherdi"]
   [:cuviers-beaked-whale		"Ziphius cavirostris"]
   ])


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
		  "jane"
		  "anthony"
		  "lexie"])

  (def num-years 10)

  (def max-weight 172) ;; metric tons
  (def max-length 32.9) ;; metres

  (let [max-length-x-100 (* max-length 100)
	max-weight-x-100 (* max-weight 100)]

    (defn random-whale [id]
      (Whale.
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


;; (def some-whales
;;   [(Whale. 0 0 (Date. 0 1 1) "jules" "blue whale" "arctic" 100 100)
;;    (Whale. 1 0 (Date. 0 1 1) "jules" "blue whale" "indian" 200 100)
;;    (Whale. 2 0 (Date. 0 1 1) "jules" "gray whale" "arctic" 100 100)
;;    (Whale. 3 0 (Date. 0 1 1) "jules" "gray whale" "indian" 200 100)
;;    (Whale. 4 0 (Date. 1 1 1) "jules" "blue whale" "arctic" 100 100)
;;    (Whale. 5 0 (Date. 1 1 1) "jules" "blue whale" "indian" 200 100)
;;    (Whale. 6 0 (Date. 1 1 1) "jules" "gray whale" "arctic" 100 100)
;;    (Whale. 7 0 (Date. 1 1 1) "jules" "gray whale" "indian" 200 100)])

  (let [num-whales 50
	some-whales (doall (pmap (fn [id] (random-whale id)) (range num-whales)))]
    (do (time (doall (pmap (fn [whale] (insert whales-model whale)) some-whales))) nil))

  )

;;--------------------------------------------------------------------------------
;; A Join - by reference
;;--------------------------------------------------------------------------------

(definterface-metadata
    join-metadata
    Join
    [^{:tag int :primary-key true}   id
     ^{:tag int :version-key true}   version
     ^{:tag clojure.lang.Keyword :immutable true} type
     ^{:tag float}                   length
     ^{:tag float}                   weight
     ^{:tag String}                  ocean
     ^{:tag int}                     ocean-area
     ^{:tag int}                     ocean-max-depth]
    (fn [^Integer lhs ^Integer rhs] (- (int lhs) (int rhs))))

(deftype
  JoinImpl
  [^int id ^int version ^Whale whale ^Ocean ocean]
  java.io.Serializable
  Join
  (^int id [this] id)
  (^int version [this] version)
  (^clojure.lang.Keyword type [this] (.type whale))
  (^float length [this] (.length whale))
  (^float weight [this] (.weight whale))
  (^String ocean [this] (.id ocean))
  (^int ocean-area [this] (.area ocean))
  (^int ocean-max-depth [this] (.max-depth ocean)))

(def joins-model
     (JoinModel.
      "WhalesAndOceans"
      join-metadata
      whales-model
      {:ocean oceans-model}
      (fn [id version ^Whale whale [[^Ocean ocean]]] (JoinImpl. id version whale ocean))))

(insert *metamodel* joins-model)

;;--------------------------------------------------------------------------------
;; Animation
;;--------------------------------------------------------------------------------

(defmulti mutate (fn [^Attribute attribute datum] (.getKey attribute)))

;; whale attributes
(defmethod mutate :version   [^Attribute attribute datum] (inc (.get (.getGetter attribute) datum)))
(defmethod mutate :time      [^Attribute attribute datum] (Date.))
(defmethod mutate :reporter  [^Attribute attribute datum] (rnd reporters))
(defmethod mutate :ocean     [^Attribute attribute datum] (rnd oceans))
(defmethod mutate :length    [^Attribute attribute datum] (+ 1 (.get (.getGetter attribute) datum)))
(defmethod mutate :weight    [^Attribute attribute datum] (+ 1 (.get (.getGetter attribute) datum)))
(defmethod mutate :default   [^Attribute attribute datum] (.get (.getGetter attribute) datum))
;; ocean attributes
(defmethod mutate :max-depth [^Attribute attribute datum] (int (/ (* (.get (.getGetter attribute) datum) (+ 98 (rand-int 5))) 100))) ;; +/- 2%
(defmethod mutate :area      [^Attribute attribute datum] (int (/ (* (.get (.getGetter attribute) datum) (+ 98 (rand-int 5))) 100))) ;; +/- 2%

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

(if false
  (do
    
    ;; all whales
    (? (dfrom "Whales"))

    ;; count all whales
    (? (dcount)(dfrom "Whales"))

    ;; sum length of all whales
    (? (dsum :length)(dfrom "Whales"))

    ;; we're only interested in toothed whales
    (? (dsplit :type (fn [type] (if (isa? whale-hierarchy :odontoceti type) [:odontoceti] []))) (dfrom "Whales"))

    ;; we're only interested in rorquals
    (? (dsplit :type (fn [type] (if (isa? whale-hierarchy :balaenopteridae type) [:balaenopteridae] []))) (dfrom "Whales"))

    ;; split by suborder
    (do
      (def whale-hierarchy-children
	   (reduce (fn [reduction [key vals]] (reduce (fn [reduction val] (conj reduction [val key])) reduction vals)) {} (whale-hierarchy :parents)))
      
      (def whale-type-to-suborder
	   (reduce (fn [reduction type] (conj reduction [type (whale-hierarchy-children (whale-hierarchy-children type))])) {} types))
      
      (? (dsplit :type (fn [type] [(whale-type-to-suborder type)])) (dfrom "Whales"))
      )
    
    ;; split by family
    (do
      (def whale-hierarchy-children
	   (reduce (fn [reduction [key vals]] (reduce (fn [reduction val] (conj reduction [val key])) reduction vals)) {} (whale-hierarchy :parents)))
      
      (def whale-type-to-family
	   (reduce (fn [reduction type] (conj reduction [type (whale-hierarchy-children type)])) {} types))
      
      (? (dsplit :type (fn [type] [(whale-type-to-family type)])) (dfrom "Whales")))

    ;; split by ocean
    (? (dsplit :ocean)(dfrom "Whales"))
    
    ;; TODO - doesn't work...
    ;; (? (dsplit :ocean)(dfrom "WhalesAndOceans"))

    ;; flat split by type then ocean
    (? (dsplit :ocean)(dsplit :type)(dfrom "Whales"))

    ;; nested split by type then ocean
    (? (dsplit :type list [(dsplit :ocean)])(dfrom "Whales"))

    ;; TODO - what is this meant to do ?
    ;; (? (dsplit :type list [(dunion)(dsplit :ocean)])(dfrom "Whales"))

    ;; sum weights per ocean
    (? (dsum :weight)(dsplit :ocean)(dfrom "Whales"))

    ;; summarise weights per ocean
    (? (dunion)(dsum :weight)(dsplit :ocean)(dfrom "Whales"))
    ;; summarise weights per type
    (? (dunion)(dsum :weight)(dsplit :type)(dfrom "Whales"))

    ;; TODO - pivot broken

    ;; pivot weights per ocean summary
    ;; (? (dpivot :ocean ["arctic" "atlantic" "indian" "pacific" "southern"] (keyword "sum(:weight)")) (dsum :weight) (dsplit :ocean) (dfrom "Whales"))

    ;; for each type - pivot weights per ocean summary
    ;; (? (dsplit :type list [(dpivot :ocean ["arctic" "atlantic" "indian" "pacific" "southern"] (keyword "sum(:weight)")) (dsum :weight) (dsplit :ocean)]) (dfrom "Whales"))
    ;; (? (dsplit :type list [(dunion)(dpivot :ocean ["arctic" "atlantic" "indian" "pacific" "southern"] (keyword "sum(:weight)")) (dsum :weight) (dsplit :ocean)]) (dfrom "Whales"))

    ))

;; TODO:
;; - defmodel
;; - djoin
;; - dpivot
;; - fix remaining broken queries
