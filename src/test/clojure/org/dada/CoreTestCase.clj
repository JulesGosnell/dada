;; see if we can integrate clojure with surefire/junit and thence with
;; cobertura...

(ns org.dada.CoreTestCase
    (:gen-class
     :extends junit.framework.TestCase
     ;;:init init
     :methods [[testCore [] void]]
     )
    ;;(:use [])
    (:import [junit.framework TestCase] ;;[]
	     ))

(defn -testCore [this]
  (TestCase/assertTrue (Boolean/TRUE))
  (println "TESTED!"))
