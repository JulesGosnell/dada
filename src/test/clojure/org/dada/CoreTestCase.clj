;; see if we can integrate clojure with surefire/junit and thence with
;; cobertura...

(ns org.dada.CoreTestCase
    (:gen-class
     :extends junit.framework.TestCase
     ;;:init init
     :methods [[testCore [] void]
	       ;;[test-attribute-array [] void]
	       ]
     )
    (:use [org.dada.core])
    (:import [junit.framework TestCase] ;;[]
	     ))

(defn -testCore [this]
  (TestCase/assertTrue (Boolean/TRUE))
  (println "TESTED!"))

;; (defn -test-attribute-array [this]
;;   (let [a (attribute-array :a Integer :b String)]
;;     (TestCase/assertTrue (= (alength a) 2))
;;     (TestCase/assertTrue (= (aget a 0 0) "java.lang.Integer"))
;;     (TestCase/assertTrue (= (aget a 0 1) "a"))
;;     (TestCase/assertTrue (= (aget a 1 0) "java.lang.String"))
;;     (TestCase/assertTrue (= (aget a 1 1) "b"))
;;     ))
