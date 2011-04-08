(ns
 org.dada.swt.inspect
 (:use 
  [clojure.tools logging]  
  [org.dada core]  
  [org.dada.core dql]
  [org.dada.swt nattable tab shell])
 (:import
  [java.util Collection]
  [org.eclipse.swt.widgets Composite Display Shell]
  [org.eclipse.swt SWT]
  [org.dada.core Model ModelView Result SimpleModelView Update View]
  ))

;;--------------------------------------------------------------------------------

(defn inspect-model [^Model model & [drilldown-fn close-fn]]
  (let [display (inc-display)]
    (.asyncExec
     display
     (fn []
       (let [^Composite shell (create-shell display (.getName model) (fn [_] (if (and (not (dec-display)) close-fn) (close-fn))))
	     ^Composite component (nattable-make model shell drilldown-fn)]
	 (.pack component)
	 (.pack shell))))))

(defn inspect [query & [drilldown-fn]]
  (let [[metadata-fn data-fn] query
	[^Model metamodel] (data-fn)]
    (inspect-model metamodel drilldown-fn)))
