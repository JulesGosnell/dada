(ns 
 org.dada.swt.SWTView
 (:use org.dada.swt.utils)
 (:import
  [java.util Collection Comparator Timer TimerTask]
  [org.dada.core Attribute Getter Metadata Model SessionManager ServiceFactory Update View]
  [org.eclipse.swt SWT]
  [org.eclipse.swt.custom CTabFolder CTabItem]
  [org.eclipse.swt.widgets Button Composite Control Display Shell Table TableColumn TableItem Text Listener Widget]
  [org.eclipse.swt.layout GridData GridLayout]
  [org.eclipse.swt.events ShellAdapter SelectionEvent SelectionListener]

;;  [org.swtchart Chart ISeries ISeriesSet ISeries$SeriesType]

  [org.dada.swt TableCellState TableItemState TableState]
  )
 (:gen-class
  :implements [org.dada.core.View java.io.Serializable]
  :constructors {[org.dada.core.Model clojure.lang.Keyword org.dada.core.ServiceFactory org.eclipse.swt.widgets.Composite] []}
  :methods [[writeReplace [] Object][getControl [] org.eclipse.swt.widgets.Control]]
  :init init
  :post-init post-init
  :state state
  )
 )

;;--------------------------------------------------------------------------------

;; need to consider how to manage state - extant and extinct tables
;; synchronisation will be done by WT as long as we only update state on UI threads :-)

;; nice highlight colours and fading

;; once state management is resolved, we can fix sorting

;; then we can move on to further display types

;; table view should probably be default View
;;--------------------------------------------------------------------------------

(defn execute-query [#^SessionManager session-manager query service-factory #^Composite parent]
  (let [[[metadata name keys [operation & details]] [model & rest]] (.query session-manager "org.dada.dsl" query)
	shell (create-shell (if parent (.getDisplay parent) (Display.)))]
    (println "QUERY" query "->" model operation)
    (org.dada.swt.SWTView. model operation service-factory shell)
    shell))

;;--------------------------------------------------------------------------------

;; returns ...
(defmulti create (fn [operation model #^Composite parent service-factory] operation))

(defmethod create :default [operation & rest] (apply create :data rest))

;;--------------------------------------------------------------------------------

(defn -init [#^Model model #^Keyword operation #^ServiceFactory service-factory #^Composite parent]
  (println "OPERATION" operation)
  [ ;; super ctor args
   []
   ;; instance state
   (let [[control attach-fn detach-fn update-fn] (create operation model parent service-factory)]
     (.pack parent)
     [[control service-factory attach-fn detach-fn update-fn] (atom [nil nil])])
   ])

(defn -post-init [#^org.dada.swt.SWTView self #^Model model & _]
  (let [[[_ _ attach-fn]] (.state self)]
    (attach-fn model self)))

(defn -update [#^org.dada.swt.SWTView self insertions alterations deletions]
  (let [[[_ _ _ _ update-fn]] (.state self)]
    (update-fn insertions alterations deletions)))

(defn -getControl [#^org.dada.swt.SWTView self]
  (let [[[control]] (.state self)]
    control))

;;--------------------------------------------------------------------------------

(defn #^{:private true} -writeReplace [#^org.dada.swt.SWTView self]
  (let [[[_ #^ServiceFactory service-factory]] (.state self)]
    (.decouple service-factory self)))
