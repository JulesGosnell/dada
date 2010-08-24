(ns
 org.dada.swt.new
 (:use 
  [org.dada core dsl]
  [org.dada.swt nattable tab table utils])
 (:import
  [java.util.concurrent Executors]
  [org.eclipse.swt.widgets Button Composite Control Display Shell Text Listener Widget]
  [org.eclipse.swt SWT]
  [org.dada.core Model ModelView SessionManagerNameGetter SimpleModelView Update View ViewNameGetter]
  ))

;;--------------------------------------------------------------------------------
;; set up a test model

(def my-data [["a" 0 87687]
	   ["b" 0 98797]])

(def my-metadata (seq-metadata 3))

(def my-model (model "new" my-metadata))
(insert-n my-model my-data)
(insert *metamodel* my-model)

;;--------------------------------------------------------------------------------

(defn sm-updates [i a d]
  (println "SM-UPDATES" i a d))

;; a shell with a grid containing a single vertical row...
;; but for now we'll us a horizontal one

(defmulti create (fn [operation model #^Composite parent] operation))

(defmethod create :count [operation model #^Composite parent]
  (make-nattable-meta-view model parent))

(defmethod create :split [operation model #^Composite parent]
  (make-tab-meta-view model parent))

;;--------------------------------------------------------------------------------

(defn inspect [display query]
  (let [[metadata-fn data-fn] query
	results (data-fn)
	[metamodel metamodel-name details operation] results]
    (println results)
    ;;(.asyncExec display 
    ;;(fn [] 
    (create operation metamodel (create-shell display (.getName metamodel)))
		    ;;)
		    ;;)
    ))

(defn first-model [query]
  (.getExtant (.getData (first ((second query))))))

;;--------------------------------------------------------------------------------

(def #^Display display (Display.))

;;--------------------------------------------------------------------------------


(def parent
     (inspect display (? (ccount)(from "new")))
     ;;(inspect display (? (split 0)(from "new")))
     )

;;--------------------------------------------------------------------------------

;;(.start (Thread. (fn [] (swt-loop2 display))))
(swt-loop display)

;;--------------------------------------------------------------------------------
;; example queries

;; (? (ccount)(from "Whales"))
;; (? (split :ocean)(from "Whales"))
;; (? (split :type)(from "Whales"))
;; (? (union)(split :ocean nil [(pivot :type org.dada.demo.whales/types (keyword (sum-value-key :weight)))(sum :weight)(split :type )])(from "Whales"))
;; (? (union)(split :type nil [(pivot :ocean org.dada.demo.whales/oceans (keyword (sum-value-key :weight)))(sum :weight)(split :ocean )]) (from "Whales"))
;; (? (from "MetaModel"))

