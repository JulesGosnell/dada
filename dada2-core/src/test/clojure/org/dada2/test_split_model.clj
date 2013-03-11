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

(defrecord Employee [name ^int version grade salary])

(deftest test-split-model
  (let [sub-model-fn (fn [grade change] (versioned-optimistic-map-model :name :version >))
	model (split-model :grade sub-model-fn)
	view (test-view)
	james (->Employee :james 0 :developer 50)
	john  (->Employee :john 0 :developer 50)
	steve  (->Employee :steve 0 :manager 60)]
    (attach model view)
    (is (= {} (data model)))
    (is (= nil (data view)))

    (on-upsert model james)
    (let [sub-model (data view)]
      (is (= {:developer sub-model} (data model)))
      (is (= {:james james} (data sub-model))))

    (on-delete model james)
    (let [sub-model (data view)]
      (is (= {:developer sub-model} (data model)))
      (is (= {} (data sub-model))))

    ))
