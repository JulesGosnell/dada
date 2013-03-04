(ns org.dada2.test-core
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 core])
    (:import
     [clojure.lang Atom]
     [org.dada2.core ModelView])
    )

(defn ^ModelView test-view []
  (->ModelView 
   (atom nil)
   (fn [current upsertion] [upsertion upsertion])
   (fn [current deletion] [deletion deletion])
   (fn [current upsertions] [upsertions upsertions])
   (fn [current deletions] [deletions deletions])
   ))

(deftest test-model
  (let [model (simple-counting-model)
	view (test-view)]
    (is (not ((.getWatches ^Atom (.state model)) view)))
    (attach model view)
    ((.getWatches ^Atom (.state model)) view)
    (detach model view)
    (is (not ((.getWatches ^Atom (.state model)) view)))
    ))

(deftest test-counting-model
  (let [model (simple-counting-model)
	view (test-view)]
    (attach model view)
    (is (= 0 (data model)))
    (is (= nil (data view)))
    (on-upsert model "a")
    (is (= 1 (data model) (data view)))
    (on-upsert model "b")
    (is (= 2 (data model) (data view)))
    (on-upsert model "c")
    (is (= 3 (data model) (data view)))
    (detach model view)
    ))

(deftest test-simple-sequence-model
  (let [model (simple-sequence-model)
	view (test-view)]
    (attach model view)
    (is (not (data model)))
    (on-upsert model 1)
    (is (= '(1) (data model)))
    (is (= 1 (data view)))
    (on-upsert model 2)
    (is (= '(2 1) (data model)))
    (is (= 2 (data view)))
    (on-upsert model 3)
    (is (= '(3 2 1) (data model)))
    (is (= 3 (data view)))
    (detach model view)
    ))

(deftest test-simple-hashset-model
  (let [model (simple-hashset-model)
	view (test-view)]
    (attach model view)
    (is (= #{} (data model)))
    (is (= nil (data view)))
    (on-upsert model 1)
    (is (= #{1}) (data model))
    (is (= 1 (data view)))
    (on-upsert model 2)
    (is (= #{1 2} (data model)))
    (is (= 2 (data view)))
    (on-upsert model 1) ;;; no effect
    (is (= #{1 2}) (data model))
    (is (= 2 (data view)))
    (on-upsert model 3)
    (is (= #{3 2 1}) (data model))
    (is (= 3 (data view)))
    (detach model view)
    ))

;; aside - overwriting a map with a k/v pair which have the same value
;; but not the same references as a pair already contained in the map,
;; results in no change to the map.

(deftest test-overwrite-map-pair-by-value-has-no-effect
  (is (let [m1 {"a" "1"}](identical?  m1 (conj m1 ["a" "1"])))))

(defrecord Datum [key value])

(deftest test-simple-hashmap-model
  (let [model (simple-hashmap-model :key)
	view (test-view)
	a1 (->Datum :a 1)
	a2 (->Datum :a 2)
	b4 (->Datum :b 4)
	c6 (->Datum :c 6)]
    (attach model view)
    (is (= {} (data model)))
    (is (= nil (data view)))
    (on-upsert model a1)
    (is (= {:a a1}) (data model))
    (is (= a1 (data view)))
    (on-upsert model a2)
    (is (= {:a a2}) (data model))
    (is (= a2 (data view)))
    (on-upsert model b4)
    (is (= {:a a2 :b b4} (data model)))
    (is (= b4 (data view)))
    (on-upsert model a2)
    (is (= {:a a2 :b b4} (data model)))
    (is (= b4 (data view)))
    (on-upsert model c6)
    (is (= {:a a2 :b b4 :c c6}) (data model))
    (is (= c6 (data view)))
    (detach model view)
    ))

;; TODO
;; look into coverage reporting for Clojure
;; use leiningen
