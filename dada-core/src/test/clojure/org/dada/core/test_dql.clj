(ns org.dada.core.test-dql
    (:use 
     [clojure test]
     [org.dada core]
     [org.dada.core dql])
    (:import
     [org.dada.core
      Attribute
      Model
      Result])
    )

;;--------------------------------------------------------------------------------
;; THOUGHTS:
;; we should be able to test serially and concurrently (using exclusive lock)

;;--------------------------------------------------------------------------------
;; data model

(def whale-attributes
     (list
      [:id       (Integer/TYPE) false]
      [:version  (Integer/TYPE) true]
      ;;[:time     Date           true]
      ;;[:reporter String         true]
      [:type     clojure.lang.Keyword         false]	;a whale cannot change type
      [:ocean    clojure.lang.Keyword         true]
      [:length   (Float/TYPE)   true]
      ;;[:weight   (Float/TYPE)   true]
      ))

(def whale-data
     [[0 0 :blue :atlantic 100]
      [1 0 :blue :pacific  100]
      [2 0 :grey :atlantic 50]
      [3 0 :grey :pacific  50]])

;;(def whale-metadata (seq-metadata (count (first whale-data))))
(def whale-metadata (custom-metadata "org.dada.core.tmp.Whale" 
				     Object
				     [:id]
				     [:version] 
				     int-version-comparator 
				     whale-attributes))

(def whales (model "Whales" whale-metadata))
(insert *metamodel* whales)

;;(insert-n whales whale-data)
(let [creator (.getCreator whale-metadata)]
      (insert-n
       whales
       (map (fn [datum] (.create creator (into-array Object datum))) whale-data)))

;;--------------------------------------------------------------------------------
;; utils

(defn reduction-value [[metadata-fn data-fn]]
  (let [[#^Model metamodel] (data-fn)
	#^Model model (.getModel (first (.getExtant (.getData metamodel))))
	value (first (.getExtant (.getData model)))]
    (.get (.getGetter #^Attribute (nth (.getAttributes (.getMetadata model)) 1)) value)))

(defn to-list [#^Whale whale]
  (map (fn [#^Attribute attribute] (.get (.getGetter attribute) whale)) (.getAttributes whale-metadata)))

(defn flat-split-values [[metadata-fn data-fn]]
  (reduce
   (fn [values #^Result result]
       (println (.getModel result) (.getPrefix result) "," (.getPairs result) "," (.getOperation result))
       (conj values [(map second (.getPairs result)) 
		     (map to-list (.getExtant (.getData (.getModel result))))]))
   {}
   (.getExtant (.getData (.getModel (data-fn))))))

(defn valid-pairs [pairs]
  (println "VALID PAIRS" pairs)
  (reduce (fn [current pair] (if (= (count pair) 2) (conj current pair) current)) '() pairs))

(defn nested-split-values2 [result]
  (let [[#^Model model prefix pairs operation] result
	key (second (first (valid-pairs pairs)))
	values (.getExtant (.getData model))]
    [[key]
     (if (every? (fn [value] (instance? Result value)) values) ;TODO - is this the best I can do ?
       (reduce (fn [map value] (conj map (nested-split-values2 value))) {} values)
       (map to-list values))]))

(defn nested-split-values [[metadata-fn data-fn]]
    (second (nested-split-values2 (data-fn))))

(defn flat-group-by [fns data]
  (group-by (apply juxt fns) whale-data))

(defn nested-group-by [[fns-head & fns-tail] data]
  (reduce (fn [current [key val]] (conj current [key (if fns-tail (nested-group-by fns-tail val) val)])) {} (group-by (juxt fns-head) data)))

;;--------------------------------------------------------------------------------
;; tests

(deftest test-dcount
  (is (= (reduction-value (? (dcount)(dfrom "Whales")))
	 (count whale-data))))

(deftest test-dunion
  (is (= (reduction-value (? (dcount)(dunion)(dsplit :id)(dfrom "Whales")))
	 (count whale-data))))

(deftest test-dsum
  (is (= (reduction-value (? (dsum :length)(dfrom "Whales")))
	 (reduce (fn [total whale] (+ total (nth whale 4))) 0 whale-data))))

;; one dimension -  {[[key][whale...]]...}
(deftest test-split-1d
  (is (= (flat-split-values (? (dsplit :type)(dfrom "Whales")))
	 (flat-group-by [(fn [[_ _ type]] type)] whale-data))))

;; 2 dimensions - flat - {[[key1 key2][whale...]]...}
(deftest test-flat-split-2d
  (is (= (flat-split-values (? (dsplit :ocean)(dsplit :type)(dfrom "Whales")))
	 (flat-group-by [(fn [[_ _ type]] type)(fn [[_ _ _ ocean]] ocean)] whale-data))))

;;2 dimensions - nested - a map of {[[key1]{[[key2][whale...]]}]...}
(deftest test-nested-split-2d
  (is (= (nested-split-values (? (dsplit :type list [(dsplit :ocean)])(dfrom "Whales")))
	 (nested-group-by [(fn [[_ _ type]] type)(fn [[_ _ _ ocean]] ocean)] whale-data))))

;; TODO: extend tests to cover union and pivot...
