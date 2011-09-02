(ns org.dada.core.test-SimpleModelView
    (:use 
     [clojure test]
     [clojure.tools logging]
     [org.dada core]
     [org.dada.core map]
     )
    (:import
     [java.util Collection]
     [org.dada.core Data ModelView SimpleModelView Update]
     )
    )

(defn get-mutable [^SimpleModelView model] @(second (.state model)))

(deftest test-simple-model-view

  (def ^ModelView account-model  (model "Accounts" (seq-metadata 3))) ;; id, version, name

  (is (= (get-mutable account-model) [(map-new) (map-new) (map-new)]))

  ;; insertion
  (def jules-0 [:jules 0 "Julian Gosnell"])
  (insert account-model jules-0)
  (is (= (get-mutable account-model) [(map-new2 :jules jules-0) (map-new) (map-new)]))

  ;; update
  (def jules-1 [:jules 1 "J.A.F. Gosnell"])
  (update account-model jules-0 jules-1)
  (is (= (get-mutable account-model) [(map-new2 :jules jules-1) (map-new) (map-new)]))

  ;; out of order update - current version
  (update account-model jules-0 jules-1)
  (is (= (get-mutable account-model) [(map-new2 :jules jules-1) (map-new) (map-new)]))
  
  ;; out of order deletion - previous version
  (delete account-model jules-0)
  (is (= (get-mutable account-model) [(map-new2 :jules jules-1) (map-new) (map-new)]))

  ;;================================================================================
  ;; TODO - reverse semantic here...
  ;; datum with same version should be ignored - etc...
  ;;================================================================================

  ;; successful direct deletion
  (delete account-model jules-1)
  (is (= (get-mutable account-model) [(map-new) (map-new2 :jules jules-1) (map-new)]))
  
  ;; unsuccessful re-insertion
  (insert account-model jules-1)
  (is (= (get-mutable account-model) [(map-new) (map-new2 :jules jules-1) (map-new)]))

  ;; successful re-insertion
  (def jules-2 [:jules 2 nil])
  (insert account-model jules-2)
  (is (= (get-mutable account-model) [(map-new2 :jules jules-2) (map-new) (map-new)]))

  ;; successful indirect deletion (amend away)
  (def jules-3 [:jules 3 "JAFG"])
  (.update account-model ^Collection [] ^Collection [] ^Collection [(Update. jules-2 jules-3)])
  (is (= (get-mutable account-model) [(map-new) (map-new2 :jules jules-3) (map-new)]))
  )
