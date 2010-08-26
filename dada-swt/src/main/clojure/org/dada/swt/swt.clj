(ns
 org.dada.swt.swt
 (:use 
  [org.dada core]
  )
 (:import
  [java.util.concurrent Executors]
  [org.dada.core Model]
  ))

;;--------------------------------------------------------------------------------

(defmulti create (fn [operation #^Model model #^Composite parent] operation))

;;--------------------------------------------------------------------------------
