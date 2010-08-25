(ns org.dada.core.test-dql
    (:use 
     [clojure test]
     [org.dada core]
     [org.dada.core dql])
    )

(def whale-data
     ;;[id, version, type, ocean, length]
     [[0 0 :blue :atlantic 100]
      [1 0 :blue :pacific  100]
      [2 0 :grey :atlantic 50]
      [3 0 :grey :pacific  50]])

(def whales (model "Whales" (seq-metadata (count (first whale-data)))))
(insert *metamodel* whales)
(insert-n whales whale-data)

;;--------------------------------------------------------------------------------
;; THOUGHTS:
;; we should be able to test serially and concurrently (using exclusive lock)
;;--------------------------------------------------------------------------------
;; utils

(defn reduction-value [[metadata-fn data-fn]]
  (let [[metamodel] (data-fn)
	model (first (first (.getExtant (.getData metamodel))))
	value (first (.getExtant (.getData model)))]
    (.get (.getGetter (nth (.getAttributes (.getMetadata model)) 1)) value)))

;;--------------------------------------------------------------------------------

(deftest test-dcount
  (is (= (reduction-value (? (dcount)(dfrom "Whales")))
	 (count whale-data))))

(deftest test-dsum
  (is (= (reduction-value (? (dsum 4)(dfrom "Whales")))
	 (reduce (fn [total whale] (+ total (nth whale 4))) 0 whale-data))))



