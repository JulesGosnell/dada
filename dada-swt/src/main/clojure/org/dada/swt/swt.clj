(ns
 org.dada.swt.swt
 (:import
  [org.eclipse.swt.widgets Composite]
  [org.dada.core Result]
  ))

;;--------------------------------------------------------------------------------

(defn layer [#^Result result]
  ;;(trace (str "RESULT " (interpose " " (map (fn [i] (nth result i)) (range 4)))))
  (if (every? (fn [datum] (instance? Result datum)) (.getExtant (.getData (.getModel result))))
    :metadata
    :data))

(defmulti create (fn [element #^Composite parent] (layer element)))

(defmulti extract-key (fn [element] (layer element)))

;;--------------------------------------------------------------------------------
