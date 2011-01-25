(ns 
 org.dada.swt.table
 (:use [org.dada core]
       [org.dada.swt swt utils])
 (:import
  [java.util Collection Comparator Timer TimerTask]
  [org.dada.core Attribute Getter Metadata Model ModelView SessionManager ServiceFactory SimpleModelView Update View]
  [org.eclipse.swt SWT]
  [org.eclipse.swt.widgets Composite Display Shell Table TableColumn TableItem Text Listener Widget]
  [org.eclipse.swt.layout GridData GridLayout]
  [org.eclipse.swt.events SelectionEvent SelectionListener]
  ;;[org.dada.swt TableCellState TableItemState TableState]
  )
 )

;;--------------------------------------------------------------------------------
;; table

;; (defn stash-item [#^Table table key item]
;;   (.put (.primaryKeyToTableItem #^TableState (.getData table)) key item))

;; (defn fetch-item [#^Table table key]
;;   (.get (.primaryKeyToTableItem #^TableState (.getData table)) key))

;; (defn stash-datum [#^TableItem item datum]
;;   (set! (.datum #^TableItemState (.getData item)) datum))  

;; (defn fetch-datum [#^TableItem item]
;;   (.datum #^TableItemState (.getData item)))

;; (defn make-table-item [datum getters #^Table table]
;;   (let [#^TableItem item (TableItem. table (SWT/NONE))
;; 	item-state (TableItemState. (count getters))]
;;     (.setData item item-state)
;;     (doall
;;      (map
;;       (fn [index #^Getter getter]
;; 	  (let [cell-state (TableCellState.)]
;; 	    (set! (.datum cell-state) datum)
;; 	    (aset (.cells item-state) index cell-state)
;; 	    (.setText item index (str (.get getter datum)))))
;;       (iterate inc 0)
;;       getters))
;;     item))

;; (defn sort-table [#^Table table index #^Getter getter getters #^TableColumn column]
;;   (println "SORT TABLE" (.getText column) index)
;;   (let [items (.getItems table)
;; 	comparator (proxy [Comparator][] (compare [lhs rhs] (.compareTo #^Comparable (.get getter lhs) #^Comparable (.get getter rhs))))
;; 	sorted (sort comparator (map fetch-datum items))]
;;     (println "SORTED" sorted)
;;     ;; rebuild all the items !! - there must be a better way...
;;     ;; THIS TRASHES THE MAP I AM HOLDING ON TABLE
;;     ;;(.removeAll table)
;;     ;;(doall (map (fn [datum] (make-table-item datum getters table)) sorted))
;;     ))

;; (defn make-table-column [#^Table table index getter getters #^String text]
;;   (let [ #^TableColumn column (TableColumn. table (SWT/NONE))]
;;     (.setText column text)
;;     (.addListener 
;;      column 
;;      (SWT/Selection)
;;      (proxy
;;       [Listener]
;;       []
;;       (handleEvent [evt] (try (sort-table table index getter getters column) (catch Throwable t (.printStackTrace t))))))
;;     column))

;; (defn blink-table-cell [#^TableItem item #^TableCellState state index]
;;   (let [task (.task state)
;; 	fg (.getForeground item index)
;; 	bg (.getBackground item index)]
;;     (if task
;;       ;; either
;;       ;; cancel existing timer
;;       (.cancel task)
;;       ;; or toggle colours
;;       (do
;; 	(.setForeground item index bg)
;; 	(.setBackground item index fg)))
;;     ;; kick off time to toggle them back
;;     (set! 
;;      (.task state)
;;      (.schedule
;;       (.timer #^TableState (.getData #^Table (.getParent item)))
;;       (proxy
;;        [TimerTask]
;;        []
;;        (run []
;; 	    (.asyncExec
;; 	     (.getDisplay item)
;; 	     (fn []
;; 		 (if (not (.isDisposed item))
;; 		   (do
;; 		     (.setForeground item index fg)
;; 		     (.setBackground item index bg)))
;; 		 (set! (.task state) nil)))))
;;       (long 1000)))))

;; (defn update-table-cell [#^TableItem item #^TableItemState item-state index old new #^Getter getter]
;;   (let [new-value (.get getter new)
;; 	new-string (str new-value)
;; 	old-string (.getText item index)
;; 	cell-states (.cells item-state)]
;;     (if (not (= old-string new-string))
;;       (do
;; 	(.setText item index new-string)
;; 	(set! (.value #^TableCellState (aget cell-states index)) new-value)
;; 	(blink-table-cell item (aget cell-states index) index)))))

;; (defn update-table-item [#^TableItem item old new getters]
;;   ;;(println "UPDATE TABLE ITEM")
;;   (let [#^TableItemState item-state (.getData item)]
;;     (doall (map (fn [index #^Getter getter] (update-table-cell item item-state index old new getter)) (iterate inc 0) getters))))

;; (defn table-select [table model service-factory]
;;   ;;(println "TABLE SELECT" (fetch-datum (first (.getSelection table))))
;;   )

;; (defn table-default-select [#^Table table model service-factory]
;;   (let [datum (fetch-datum #^TableItem (first (.getSelection table)))]
;;     (println "TABLE DEFAULT SELECT" datum)
;;     (if (instance? Model datum)
;;       (org.dada.swt.SWTView. datum :default service-factory (create-shell (.getDisplay table)))
;;       (if (and (instance? Collection datum) (instance? Model (first datum)))
;; 	(let [[model & rest] datum]
;; 	  (println "MODEL" model rest)
;; 	  (org.dada.swt.SWTView. model :default service-factory (create-shell (.getDisplay table))))
;; 	))))

;; (defmethod create :data [operation #^Model model #^Composite parent service-factory] 
;;   (let [#^Metadata metadata (.getMetadata model)
;; 	attributes (.getAttributes metadata)
;; 	#^Table table (Table. parent (SWT/SINGLE))
;; 	titles (map (fn [#^Attribute attribute] (str (.getKey attribute))) attributes)
;; 	getters (map (fn [#^Attribute attribute] (.getGetter attribute)) attributes)
;; 	columns (doall (map (fn [index title getter] (make-table-column table index getter getters title)) (iterate inc 0) titles getters))
;; 	primary-getter (.getPrimaryGetter metadata)
;; 	version-comparator (.getVersionComparator metadata)
;; 	]
;;     (.setData table (TableState.))
;;     (.addSelectionListener
;;      table
;;      (proxy
;;       [SelectionListener]
;;       []
;;       (widgetSelected [#^SelectionEvent evt]
;; 		      (try
;; 		       (table-select table model service-factory)
;; 		       (catch Throwable t (.printStackTrace t))))
;;       (widgetDefaultSelected [#^SelectionEvent evt]
;; 			     (try
;; 			      (table-default-select table model service-factory)
;; 			      (catch Throwable t (.printStackTrace t))))
;;       ))
;;     (.setLinesVisible table true)
;;     (.setHeaderVisible table true)
;;     (.setLayoutData table (GridData. (SWT/FILL) (SWT/FILL) true true))
;;     (doall (map (fn [#^TableColumn column](.pack column)) columns))
;;     ;; add listeners here
		      
;;     [ ;; widgets
;;      table
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
;; 	 ;;(println "UPDATES" insertions alterations deletions)
;; 	 (.asyncExec
;; 	  (.getDisplay parent)
;; 	  (fn []
;; 	      (try
;; 	       (if (not (.isDisposed table))
;; 		 (do
;; 		   (doall ;; dirty
;; 		    (map
;; 		     (fn [#^Update update]
;; 			 (let [datum (.getNewValue update)
;; 			       #^TableItem item (make-table-item datum getters table)]
;; 			   ;;(println "INSERT ITEM" table item)
;; 			   ;; TODO - NEED TO SORT THIS OUT - IT FALLS OUT OF DATE WHEN ITEMS ARE REMOVED ETC
;; 			   ;; ADD A DISPOSE LISTENER OR RETHINK
;; 			   ;; SHOULD BE DONE IN MAKE ITEM

;; 			   ;; DO WE REALLY WANT TO HODKL STATE ON THE WIDGETS ?
;; 			   ;; CAN THIS BE DONE GENERICALLY FOR ALL UI REALISATIONS ?
;; 			   (stash-item table (.get primary-getter datum) item)
;; 			   (stash-datum item datum)
;; 			   item			  
;; 			   ))
;; 		     insertions))
;; 		   (doall ;; dirty
;; 		    (map
;; 		     (fn [#^Update update]
;; 			 (let [new (.getNewValue update)
;; 			       pk (str (.get primary-getter new)) ;TODO: WARNING PK is an Object NOT a String
;; 			       #^TableItem item (fetch-item table pk)
;; 			       old (fetch-datum item)]
;; 			   ;;(println "UPDATE ITEM" table item)
;; 			   (if (.higher version-comparator old new)
;; 			     (update-table-item item old new getters))
;; 			   item			  
;; 			   ))
;; 		     alterations))
;; 		   (doall ;; dirty
;; 		    (map
;; 		     (fn [#^Update update]
;; 			 (let [new (.getOldValue update)
;; 			       pk (str (.get primary-getter new)) ;TODO: WARNING PK is an Object NOT a String
;; 			       #^TableItem item (fetch-item table pk)]
;; 			   (if item
;; 			     (let [old (fetch-datum item)]
;; 			       (println "DELETE ITEM" table item)
;; 			       (if (.higher version-comparator old new)
;; 				 (.remove table (.indexOf table item))))
;; 			     (println "WARN: DELETING UNKNOWN ITEM: " pk))
;; 			   item			  
;; 			   ))
;; 		     deletions))
;; 		   ;;(.pack table)
;; 		   ;;(.pack parent)
;; 		   ))
;; 	       (catch Throwable t (.printStackTrace t))))
;; 	  ))]
;;     ))

;;--------------------------------------------------------------------------------

(defn insert-table-meta-view [#^Composite parent insertion]
  (println "INSERT" insertion)
  
  )

(defn update-table-meta-view [#^Composite parent insertions _ deletions]
  (doall (map (fn [^Update insertion] (insert-table-meta-view parent (.getNewValue insertion))) insertions)))
									 
(defn make-table-meta-view [#^Model async-model #^Composite parent]
  ;; this model will accept unordered async events and put out ordered
  ;; sync events, suitable for the gui...
  (let [display (.getDisplay parent)
	#^ModelView sync-model (SimpleModelView. "table-meta-view" (.getMetadata async-model))
	async-view (proxy [View] [] (update [i a d] (try
						     ;;(.asyncExec display (fn []
						     (.update sync-model i a d)
									     ;;))
						     (catch Throwable t (.printStackTrace t)))))] ;TODO: Should be Serialisable
    (register async-model async-view)
    
    (register sync-model (proxy [View] [] (update [i a d] (update-table-meta-view parent i a d))))
    
    ))
  
  ;; make a View
  ;; connect it to a Model to handle events arriving in wrong order
  ;; connect that to a View (Meta) which maintains a new Table for each item
  ;; each Table should do the same for each element of each Model

