(ns org.dada.core.test-counted-set
    (:use 
     [clojure test]
     [org.dada.core counted-set])
    )

(deftest initialisation-from-nil
  (is (= (counted-set-vals (counted-set-inc nil :a)) '(:a))))

(deftest initialisation-from-empty-list
  (is (= (counted-set-vals (counted-set-inc '() :a)) '(:a))))

(deftest single-level
  (is
   (empty?
    (counted-set-vals
     (counted-set-dec (counted-set-inc '() :a) :a)))))

;; if someone adds and removes a View, but the messages arrive in the
;; wrong order, the result should be the same as if they arrived in
;; the right order...
(deftest single-level-reversed
  (is
   (empty?
    (counted-set-vals
     (counted-set-inc (counted-set-dec '() :a) :a)))))

(deftest multi-level
  (empty?
   (counted-set-vals
    (counted-set-dec
     (counted-set-dec
      (counted-set-inc
       (counted-set-inc '() :a) :a) :a) :a))))

(deftest multi-level-unequal
  (=
   (counted-set-vals
    (counted-set-dec
     (counted-set-inc
      (counted-set-inc '() :a) :a) :a))
   '(:a)))

