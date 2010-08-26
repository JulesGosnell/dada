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

(def my-model (model "Family" (seq-metadata 4)))
(insert-n my-model [[3 0 "alexandra" 2005][1 0 "jane" 1969][2 0 "anthony" 2001][0 0 "jules" 1967]])
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

(def *display* (Display.))

(defn inspect [query]
  (let [[metadata-fn data-fn] query
	results (data-fn)
	[metamodel metamodel-name details operation] results
	shell (create-shell *display* (.getName metamodel))
	component (create operation metamodel shell)]
    (println results)
    (.pack component)
    (.pack shell)
    (println "LOOP" shell)
    (shell-loop shell)))

;;--------------------------------------------------------------------------------
;; example queries

;;(inspect (? (ccount)(from "Family")))
;;(inspect (? (split 2)(from "Family")))

;;(? (ccount)(from "Whales"))
;; (? (split :ocean)(from "Whales"))
;; (? (split :type)(from "Whales"))
;; (? (union)(split :ocean nil [(pivot :type org.dada.demo.whales/types (keyword (sum-value-key :weight)))(sum :weight)(split :type )])(from "Whales"))
;; (? (union)(split :type nil [(pivot :ocean org.dada.demo.whales/oceans (keyword (sum-value-key :weight)))(sum :weight)(split :ocean )]) (from "Whales"))
;; (? (from "MetaModel"))

