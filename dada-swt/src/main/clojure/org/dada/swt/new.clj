(ns
 org.dada.swt.new
 (:use 
  [clojure.contrib logging]  
  [org.dada core]  
  [org.dada.core dql]
  [org.dada.swt swt nattable tab table utils])
 (:import
  [java.util Collection]
  [java.util.concurrent Executors]
  [org.eclipse.swt.widgets Button Composite Control Display Shell Text Listener Widget]
  [org.eclipse.swt SWT]
  [org.dada.core Model ModelView Result SessionManagerNameGetter SimpleModelView Update View ViewNameGetter]
  ))

;;--------------------------------------------------------------------------------

(defmethod create :metadata [element #^Composite parent & [drilldown-fn]] (tab-make element parent))
(defmethod create :data [element #^Composite parent & [drilldown-fn]] (nattable-make element parent drilldown-fn))
  
(defmethod extract-key :default [^Result result]
  (reduce (fn [output [key value]] (if value (conj output value) output)) [] (.getPairs result)))

;;--------------------------------------------------------------------------------

(let [display (atom nil)]
  (defn ensure-display []
    (swap! display (fn [d] (or d (Display.))))))

(if (not *compile-files*)
  (.start
   (Thread.
    (fn []
      (swt-loop (ensure-display))))))

;; TODO - detach View on closing
(defn inspect [query & [drilldown-fn]]
  (.asyncExec
   (ensure-display)
   (fn []
       (let [[metadata-fn data-fn] query
	     results (data-fn)
	     [^Model metamodel] results
	     ^Composite shell (create-shell (ensure-display) (.getName metamodel))
	     ^Composite component (create results shell drilldown-fn)]
	 (trace results)
	 (.pack component)
	 (.pack shell)))))


(defn inspect-model [^Model model & [drilldown-fn]]
  (.asyncExec
   (ensure-display)
   (fn []
     (let [results (Result. model "Foo" [] [])
	   ^Composite shell (create-shell (ensure-display) (.getName model))
	   ^Composite component (create results shell drilldown-fn)]
       (trace results)
       (.pack component)
       (.pack shell)))))