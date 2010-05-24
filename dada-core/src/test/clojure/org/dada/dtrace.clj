(ns 
 #^{:author "Jules Gosnell" :doc "Demo domain for DADA"}
 org.dada.dtrace
 (:use [clojure.contrib repl-utils trace])
 (:import (org.slf4j Logger LoggerFactory))
 )

(def #^Logger *tracer* (LoggerFactory/getLogger "org.dada"))

(defmacro dtrace [f]
  `(let [old-f# (eval ~f)]
     (println ~f "=" old-f#)))

;; TODO - route trace through logger
;; TODO - write untrace
