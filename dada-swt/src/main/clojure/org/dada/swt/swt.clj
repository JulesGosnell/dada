(ns
 org.dada.swt.swt
 (:use 
  [org.dada core]
  )
 (:import
  [java.util Collection]
  [java.util.concurrent Executors]
  [org.dada.core Model]
  ))

;;--------------------------------------------------------------------------------

(defn layer [element]
  (println "ELEMENT" element)
  (if (and (instance? Collection element) (= (count element) 2))
    (do
      (println "DATA" element)
      :data)
    (do
      (println "METADATA" (map (fn [i] (nth element i)) (range 4)))
      :metadata
      )))

(defmulti create (fn [element #^Composite parent] (layer element)))

(defmulti extract-key (fn [element] (layer element)))

;;--------------------------------------------------------------------------------
