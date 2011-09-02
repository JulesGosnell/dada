(ns org.dada.core.test-SimpleModelView
  (:use 
   [clojure test]
   [clojure.tools logging]
   [org.dada core]
   )
  (:import
   [java.util Collection]
   [org.dada.core Data ModelView SimpleModelView Update]
   )
  )

(defn get-mutable [^SimpleModelView model] @(second (.state model)))

(deftest test-simple-model-view

  (def ^ModelView account-model  (model "Accounts" (seq-metadata 3))) ;; id, version, name

  (is (= (get-mutable account-model) [{} {} {}]))

  ;; insertion
  (def jules-0 [:jules 0 "Julian Gosnell"])
  (insert account-model jules-0)
  (is (= (get-mutable account-model) [{:jules jules-0} {} {}]))

  ;; update
  (def jules-1 [:jules 1 "J.A.F. Gosnell"])
  (update account-model jules-0 jules-1)
  (is (= (get-mutable account-model) [{:jules jules-1} {} {}]))

  ;; out of order update
  (update account-model jules-0 jules-1)
  (is (= (get-mutable account-model) [{:jules jules-1} {} {}]))
  
  ;; out of order deletion
  (delete account-model jules-0)
  (is (= (get-mutable account-model) [{:jules jules-1} {} {}]))

  ;; successful direct deletion
  (delete account-model jules-1)
  (is (= (get-mutable account-model) [{} {:jules jules-1} {}]))

  ;; unsucessful re-insertion
  (insert account-model jules-1)
  (is (= (get-mutable account-model) [{} {:jules jules-1} {}]))

  ;; sucessful re-insertion
  (def jules-2 [:jules 2 nil])
  (insert account-model jules-2)
  (is (= (get-mutable account-model) [{:jules jules-2} {} {}]))

  ;; successful indirect deletion (amend away)
  (def jules-3 [:jules 3 "JAFG"])
  (.update account-model ^Collection [] ^Collection [] ^Collection [(Update. jules-2 jules-3)])
  (is (= (get-mutable account-model) [{} {:jules jules-3} {}]))
  )
