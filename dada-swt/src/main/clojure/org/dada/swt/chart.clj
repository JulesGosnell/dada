(ns 
 org.dada.swt.chart
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
;; chart

;; (defn make-chart-series [datum getters #^Chart chart]
;;   (into-array
;;    (Double/TYPE)
;;    (map
;;     (fn [#^Getter getter] (double (let [n (.get getter datum)](if (number? n) n 0))))
;;     getters)))

;; (defn update-chart-series [series old new getters]
;;   )

;; (defmethod create :chart [operation #^Model model #^Composite parent service-factory] 
;;   (let [#^Metadata metadata (.getMetadata model)
;; 	attributes (.getAttributes metadata)
;; 	#^Chart chart (Chart. parent (SWT/NONE))
;; 	#^ISeriesSet series-set (.getSeriesSet chart)
;; 	titles (map (fn [#^Attribute attribute] (str (.getKey attribute))) attributes)
;; 	getters (map (fn [#^Attribute attribute] (.getGetter attribute)) attributes)
;; 	primary-getter (.getPrimaryGetter metadata)
;; 	version-comparator (.getVersionComparator metadata)
;; 	]
;;     (.setLayoutData chart (GridData. (SWT/FILL) (SWT/FILL) true true))
;;     [ ;; widgets
;;      chart
;;      ;; attach
;;      (fn [#^Model model #^View view]
;; 	 (let [data (.registerView model view)]
;; 	   (.update view (map (fn [datum] (Update. nil datum)) (.getExtant data)) '() (map (fn [datum] (Update. datum nil)) (.getExtinct data)))))
;;      ;; detach
;;      (fn [#^Model model #^View view]
;; 	 (let [data (.registerView model view)]
;; 	   (println "DEREGISTER - NYI:" data)))
;;      ;; update
;;      (fn [insertions alterations deletions]
;; 	 (println "UPDATES" insertions alterations deletions)
;; 	 (if (not (.isDisposed chart))
;; 	   (.asyncExec
;; 	    (.getDisplay parent)
;; 	    (fn []
;; 		(try
;; 		 (do
;; 		   (doall ;; dirty
;; 		    (map
;; 		     (fn [#^Update update]
;; 			 (let [datum (.getNewValue update)
;; 			       points (make-chart-series datum getters chart)
;; 			       #^ISeries series (.createSeries series-set (ISeries$SeriesType/LINE) (str (.get primary-getter datum)))]
;; 			   (.setYSeries series points)))
;; 		     insertions))
;; 		   ;; (doall ;; dirty
;; 		   ;;  (map
;; 		   ;;   (fn [#^Update update]
;; 		   ;; 	 (let [new (.getNewValue update)
;; 		   ;; 	       pk (str (.get primary-getter new)) ;TODO: WARNING PK is an Object NOT a String
;; 		   ;; 	       #^ChartItem item (.getData chart pk)
;; 		   ;; 	       old (.getData item)]
;; 		   ;; 	   (println "UPDATE ITEM" chart item)
;; 		   ;; 	   (if (.higher version-comparator old new)
;; 		   ;; 	     (update-chart-item item old new getters))
;; 		   ;; 	   item			  
;; 		   ;; 	   ))
;; 		   ;;   alterations))
;; 		   ;; (doall ;; dirty
;; 		   ;;  (map
;; 		   ;;   (fn [#^Update update]
;; 		   ;; 	 (let [new (.getOldValue update)
;; 		   ;; 	       pk (str (.get primary-getter new)) ;TODO: WARNING PK is an Object NOT a String
;; 		   ;; 	       #^ChartItem item (.getData chart pk)]
;; 		   ;; 	   (if item
;; 		   ;; 	     (let [old (.getData item)]
;; 		   ;; 	       (println "DELETE ITEM" chart item)
;; 		   ;; 	       (if (.higher version-comparator old new)
;; 		   ;; 		 (.remove chart (.indexOf chart item))))
;; 		   ;; 	     (println "WARN: DELETING UNKNOWN ITEM: " pk))
;; 		   ;; 	   item			  
;; 		   ;; 	   ))
;; 		   ;;   deletions))
;; 		   (.adjustRange (.getAxisSet chart)))
;; 		(catch Throwable t (.printStackTrace t)))))
;; 	   ))]
;;     ))

