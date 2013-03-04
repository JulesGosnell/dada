(ns org.dada2.test-map-model
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 core]
     [org.dada2 test-core]
     [org.dada2 map-model])
    (:import
     [org.dada2.core ModelView])
    )

(deftest test-unversioned-map-model
  (map
   (fn [make-map-model]
       (let [model (unversioned-pessimistic-map-model :key)
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
   [unversioned-optimistic-map-model unversioned-pessimistic-map-model]))

(defrecord VersionedDatum [key ^int version value])

(deftest test-versioned-map-model
  (map
   (fn [make-map-model]
       (let [model (make-map-model :key :version >)
	view (test-view)
	a1 (->VersionedDatum :a 0 1)
	a2 (->VersionedDatum :a 1 2)
	a3 (->VersionedDatum :a 2 3)
	b4 (->VersionedDatum :b 0 4)
	c6 (->VersionedDatum :c 0 6)]
    (attach model view)
    (is (= {} (data model)))
    (is (= nil (data view)))
    ;; some upsertions...
    ;; an insertion
    (on-upsert model a1)
    (is (= {:a a1}) (data model))
    (is (= a1 (data view)))
    ;; an update
    (on-upsert model a2)
    (is (= {:a a2}) (data model))
    (is (= a2 (data view)))
    ;; an insertion
    (on-upsert model b4)
    (is (= {:a a2 :b b4} (data model)))
    (is (= b4 (data view)))
    ;; a failed update (versions are the same)
    (on-upsert model a2)
    (is (= {:a a2 :b b4} (data model)))
    (is (= b4 (data view)))
    ;; a failed update (new version less than old)
    (on-upsert model a1)
    (is (= {:a a2 :b b4} (data model)))
    (is (= b4 (data view)))
    ;; an insertion
    (on-upsert model c6)
    (is (= {:a a2 :b b4 :c c6}) (data model))
    (is (= c6 (data view)))
    ;; some deletions
    ;; a successful deletion (versions are the same)
    (on-delete model c6)
    (is (= {:a a2 :b b4}) (data model))
    (is (= c6 (data view)))
    ;; a failed deletion (new version less than old)
    (on-delete model a1)
    (is (= {:a a2 :b b4} (data model)))
    (is (= c6 (data view)))
    ;; a successful deletion (versions are the same)
    (on-delete model b4)
    (is (= {:a a2} (data model)))
    (is (= b4 (data view)))
    ;; a successful deletion (new version greater than old)
    (on-delete model a3)
    (is (= {} (data model)))
    (is (= a3 (data view)))

    ;; and now the same, but batched...
    ;; upsertions
    (on-upserts model [a1 a2 b4 a2 a1 c6])
    (is (= {:a a2 :b b4 :c c6}) (data model))
    (is (= [a1 a2 b4 c6] (data view)))
    ;; deletions
    (on-deletes model [c6 a1 b4 a3])
    (is (= {}) (data model))
    (is (= [c6 b4 a3] (data view)))

    (detach model view)
    ))
   [optimistic-map-model pessimistic-map-model]))