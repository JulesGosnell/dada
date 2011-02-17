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

(defmethod create :metadata [element #^Composite parent] (tab-make element parent))
(defmethod create :data [element #^Composite parent] (nattable-make element parent))
  
(defmethod extract-key :default [^Result result]
  (reduce (fn [output [key value]] (if value (conj output value) output)) [] (.getPairs result)))

;;--------------------------------------------------------------------------------

(if (not *compile-files*)
  (def ^Display *display* (Display.))
  (.start
   (Thread.
    (fn []
	(def ^Display *display* (Display.))
	(swt-loop *display*)))))

;; TODO - detach View on closing
(defn inspect [query]
  (.asyncExec
   *display*
   (fn []
       (let [[metadata-fn data-fn] query
	     results (data-fn)
	     [^Model metamodel] results
	     ^Composite shell (create-shell *display* (.getName metamodel))
	     ^Composite component (create results shell)]
	 (trace results)
	 (.pack component)
	 (.pack shell)))))

