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
  (let [models (atom nil)
	new-model-fn (fn [grade change] (first (swap! models conj (versioned-optimistic-map-model :name :version >))))
	model (split-model :grade new-model-fn)
	view (test-view)
	james (->Employee :james 0 :developer 50)
	john  (->Employee :john 0 :developer 50)
	steve  (->Employee :steve 0 :manager 60)]
    (attach model view)
    (is (= {} (data model)))
    (is (= nil (data view)))

    (on-upsert model james)
    (is (identical? (first @models) (data view)))
    (let [{m :developer} (data model)]
      (is (identical? m (first @models)))
      ;; now check ms contents...
      ;;(is (= {:james james} (data m)))
      )))
