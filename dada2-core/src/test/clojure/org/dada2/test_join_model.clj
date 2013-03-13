(ns org.dada2.test-join-model
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 core]
     [org.dada2 test-core]
     [org.dada2 map-model]
     ;;[org.dada2 join-model]
     )
    (:import
     [org.dada2.core ModelView])
    )
;;--------------------------------------------------------------------------------
;; impl

(defn join-model [name & args]
  (map-model name nil nil nil nil nil assoc nil))

;;--------------------------------------------------------------------------------
;; tests

(defrecord Whale [name ^int version ocean]
  Object
  (^String toString [_] (str name)))

(defrecord Ocean [name ^int version area]
  Object
  (^String toString [_] (str name)))

(defrecord WhaleAndOcean [whale-name ^int version ocean-area]
  Object
  (^String toString [_] (str whale-name)))

(deftest test-join-model
  (let [model (join-model "join-model")
	view (test-view "test")
	;; james (->Employee :james 0 :developer 50)
	;; john  (->Employee :john 0 :developer 50)
	;; steve  (->Employee :steve 0 :manager 60)
	]
    (attach model view)
    (is (= {} (data model)))
    (is (= nil (data view)))

    ;; (on-upsert model james)
    ;; (let [developer (data view)]
    ;;   (is (= {:developer developer} (data model)))
    ;;   (is (= {:james james} (data developer))))

    ;; (on-delete model james)
    ;; (let [developer (data view)]
    ;;   (is (= {:developer developer} (data model)))
    ;;   (is (= {} (data developer))))

    ;; (on-upserts model [john steve])
    ;; (let [model-data (data model)
    ;; 	  {developers :developer managers :manager} model-data]
    ;;   (is (= (count model-data) 2))
    ;;   (is (= [managers] (data view)))
    ;;   (is (= {:john john} (data developers)))
    ;;   (is (= {:steve steve} (data managers)))
    ;;   )

    ;; (on-deletes model [john steve])
    ;; (let [model-data (data model)
    ;; 	  {developers :developer managers :manager} model-data]
    ;;   (is (= (count model-data) 2))
    ;;   (is (= [] (data view)))
    ;;   (is (= {} (data developers)))
    ;;   (is (= {} (data managers)))
    ;;   )

    ))
