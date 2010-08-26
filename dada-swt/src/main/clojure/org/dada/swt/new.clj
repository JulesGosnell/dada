(ns
 org.dada.swt.new
 (:use 
  [org.dada core]  
  [org.dada.core dql]
  [org.dada.swt swt nattable tab table utils])
 (:import
  [java.util.concurrent Executors]
  [org.eclipse.swt.widgets Button Composite Control Display Shell Text Listener Widget]
  [org.eclipse.swt SWT]
  [org.dada.core Model ModelView SessionManagerNameGetter SimpleModelView Update View ViewNameGetter]
  ))

;;--------------------------------------------------------------------------------

(defmethod create :count [operation model #^Composite parent]
  (make-nattable-meta-view model parent))

(defmethod create :union [operation model #^Composite parent]
  (make-nattable-meta-view model parent))

(defmethod create :split [operation model #^Composite parent]
  (make-tab-meta-view model parent))

;;--------------------------------------------------------------------------------

(if *compile-files*
  (def *display* (Display.))
  (.start
   (Thread.
    (fn []
	(def *display* (Display.))
	(swt-loop *display*)))))

(defn inspect [query]
  (.asyncExec
   *display*
   (fn []
       (let [[metadata-fn data-fn] query
	     results (data-fn)
	     [metamodel metamodel-name details operation] results
	     shell (create-shell *display* (.getName metamodel))
	     component (create operation metamodel shell)]
	 (println results)
	 (.pack component)
	 (.pack shell)))))

;; set up a test model

(def whale-data
     ;;[id, version, type, ocean, length]
     [[0 0 :blue :atlantic 100]
      [1 0 :blue :pacific  100]
      [2 0 :grey :atlantic 50]
      [3 0 :grey :pacific  50]])

(def whales (model "Whales" (seq-metadata (count (first whale-data)))))
(insert *metamodel* whales)
(insert-n whales whale-data)

;;(inspect (? (dcount)(dfrom "Whales")))
;;(inspect (? (dsplit 2)(dfrom "Whales")))
;;(inspect (? (dcount)(dsplit 2)(dfrom "Whales")))
;;(inspect (? (dcount)(dsplit 2)(dsplit 3)(dfrom "Whales")))
;;(inspect (? (dunion)(dsplit 2)(dfrom "Whales")))
;;(inspect (? (dunion)(dcount)(dsplit 2)(dfrom "Whales")))
;;(inspect (? (dunion)(dcount)(dsplit 2)(dsplit 3)(dfrom "Whales")))

;;(inspect (? (dsplit 2 list [(dsplit 3)])(dfrom "Whales")))
;;(inspect (? (dsplit 3)(dsplit 2)(dfrom "Whales")))


;; (? (ccount)(from "Whales"))
;; (? (split :ocean)(from "Whales"))
;; (? (split :type)(from "Whales"))
;; (? (union)(split :ocean nil [(pivot :type org.dada.demo.whales/types (keyword (sum-value-key :weight)))(sum :weight)(split :type )])(from "Whales"))
;; (? (union)(split :type nil [(pivot :ocean org.dada.demo.whales/oceans (keyword (sum-value-key :weight)))(sum :weight)(split :ocean )]) (from "Whales"))
;; (? (from "MetaModel"))

