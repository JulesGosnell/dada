(ns org.dada2.test-split-model
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 core]
     [org.dada2 test-core]
     [org.dada2 map-model]
     [org.dada2 split-model])
    (:import
     [org.dada2.core ModelView])
    )

(defrecord Employee [name ^int version grade salary]
  Object
  (^String toString [_] (str name)))

(deftest test-split-model
  (let [sub-model-fn (fn [grade change] (versioned-optimistic-map-model (name grade) :name :version >))
	model (split-model "split-model" :grade sub-model-fn)
	view (test-view "test")
	james (->Employee :james 0 :developer 50)
	john  (->Employee :john 0 :developer 50)
	steve  (->Employee :steve 0 :manager 60)]
    (attach model view)
    (is (= {} (data model)))
    (is (= nil (data view)))

    (on-upsert model james)
    (let [developer (data view)]
      (is (= {:developer developer} (data model)))
      (is (= {:james james} (data developer))))

    (on-delete model james)
    (let [developer (data view)]
      (is (= {:developer developer} (data model)))
      (is (= {} (data developer))))

    (on-upserts model [john steve])
    (let [model-data (data model)
	  {developers :developer managers :manager} model-data]
      (is (= (count model-data) 2))
      (is (= [managers] (data view)))
      (is (= {:john john} (data developers)))
      (is (= {:steve steve} (data managers)))
      )

    (on-deletes model [john steve])
    (let [model-data (data model)
	  {developers :developer managers :manager} model-data]
      (is (= (count model-data) 2))
      (is (= [] (data view)))
      (is (= {} (data developers)))
      (is (= {} (data managers)))
      )

    ;; TODO: amend in/out
    ))
