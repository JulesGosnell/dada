(ns
 org.dada.swt.swt
 (:use 
  [org.dada core]
  )
 (:import
  [java.util Collection]
  [java.util.concurrent Executors]
  [org.dada.core Model Result]
  ))

;;--------------------------------------------------------------------------------

(defn layer [#^Result result]
  (trace "RESULT" (map (fn [i] (nth result i)) (range 4)))
  (if (every? (fn [datum] (instance? Result datum)) (.getExtant (.getData (.getModel result))))
    :metadata
    :data))

(defmulti create (fn [element #^Composite parent] (layer element)))

(defmulti extract-key (fn [element] (layer element)))

;;--------------------------------------------------------------------------------
