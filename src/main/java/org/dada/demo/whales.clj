(ns org.dada.whales
    ;;(:refer org.dada.core)
    )

(import (clojure.lang DynamicClassLoader))
(defn make-class [factory name superclass & properties]
  (. #^DynamicClassLoader
     (deref clojure.lang.Compiler/LOADER)
     (defineClass name 
       (.create 
	factory
	name
	(. superclass getCanonicalName)
	(if (empty? properties)
	  nil
	  (into-array (map (fn [pair] 
			       (let [array (make-array String 2)]
				 (aset array 0 (.getCanonicalName (first pair)))
				 (aset array 1 (first (rest pair)))
				 array))
			   properties)))))))

(import org.dada.asm.ClassFactory)
(def factory (new ClassFactory))

(do
  (def Cetacea (make-class factory "org.dada.whales.Cetacea" Object [(Float/TYPE) "length"] [(Float/TYPE) "weight"]))
  (def Mysticeti (make-class factory "org.dada.whales.Mysticeti" Cetacea))
  (def Balaenopteridae (make-class factory "org.dada.whales.Balaenopteridae" Mysticeti))
  (def Eschrichtiidae (make-class factory "org.dada.whales.Eschrichtiidae" Mysticeti))
  (def Neobalaenidae (make-class factory "org.dada.whales.Neobalaenidae" Mysticeti))
  (def Balaenidae (make-class factory "org.dada.whales.Balaenidae" Mysticeti))
  (def Odontoceti (make-class factory "org.dada.whales.Odontoceti" Cetacea))
  (def Physeteroidea (make-class factory "org.dada.whales.Physeteroidea" Odontoceti))
  (def Zyphiidae (make-class factory "org.dada.whales.Zyphiidae" Odontoceti))
  (def Delphinidae (make-class factory "org.dada.whales.Delphinidae" Odontoceti))
  )


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
	     "bottlenose dolphine"
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

(rnd types)


;; IDEAS

;; spotting - whale-id, time, coordinates, weight, length
;; birth - whale-id time, weight, length, location
;; death - id, time, weight, length
