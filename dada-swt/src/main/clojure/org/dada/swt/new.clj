(ns
 org.dada.swt.new
 (:use 
  [clojure.contrib logging]  
  [org.dada core]  
  [org.dada.core dql]
  [org.dada.swt swt nattable tab table utils])
 (:import
  [java.util Collection]
  [org.eclipse.swt.widgets Composite Display Shell]
  [org.eclipse.swt SWT]
  [org.dada.core Model ModelView Result SessionManagerNameGetter SimpleModelView Update View ViewNameGetter]
  ))

;;--------------------------------------------------------------------------------

(defmethod create :metadata [element #^Composite parent & [drilldown-fn]] (tab-make element parent))
(defmethod create :data [element #^Composite parent & [drilldown-fn]] (nattable-make element parent drilldown-fn))
  
(defmethod extract-key :default [^Result result]
  (reduce (fn [output [key value]] (if value (conj output value) output)) [] (.getPairs result)))

;;--------------------------------------------------------------------------------

(defn inspect-model [^Model model & [drilldown-fn close-fn]]
  (let [display (inc-display)]
    (.asyncExec
     display
     (fn []
	 (let [^Composite shell (create-shell display (.getName model) (fn [_] (if (not (dec-display)) (close-fn))))
	       ^Composite component (nattable-make model shell drilldown-fn)]
	   (.pack component)
	   (.pack shell))))))

(defn inspect [query & [drilldown-fn]]
  (let [[metadata-fn data-fn] query
	[^Model metamodel] (data-fn)]
    (inspect-model metamodel drilldown-fn)))
