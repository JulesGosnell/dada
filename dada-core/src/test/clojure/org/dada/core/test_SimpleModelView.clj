(ns org.dada.core.test-SimpleModelView
  (:use 
   [clojure test]
   [clojure.contrib logging]
   [org.dada core]
   )
  (:import
   [org.dada.core Data SimpleModelView Update]
   )
  )

(defn get-mutable [^SimpleModelView model] @(second (.state model)))

(deftest test-simple-model-view

  (def account-model  (model "Accounts" (seq-metadata 3))) ;; id, version, name

  (is (= (get-mutable account-model) [{} {} {}]))

  ;; insertion
  (def jules-0 [:jules 0 "Julian Gosnell"])
  (insert account-model jules-0)
  (is (= (get-mutable account-model) [{:jules jules-0} {} {} [(Update. nil jules-0)] [] []]))

  ;; update
  (def jules-1 [:jules 1 "J.A.F. Gosnell"])
  (update account-model jules-0 jules-1)
  (is (= (get-mutable account-model) [{:jules jules-1} {} {} [] [(Update. jules-0 jules-1)] []]))

  ;; out of order update
  (update account-model jules-0 jules-1)
  (is (= (get-mutable account-model) [{:jules jules-1} {} {} [] [] []]))
  
  ;; out of order deletion
  (delete account-model jules-1)
  (is (= (get-mutable account-model) [{:jules jules-1} {} {} [] [] []]))

  ;; successful deletion
  (def jules-2 [:jules 2 nil])
  (delete account-model jules-2)
  (is (= (get-mutable account-model) [{} {:jules jules-2} {} [] [] [(Update. jules-1 jules-2)]]))
  )
