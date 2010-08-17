(ns 
 org.dada.swt.SWTView
 (:import
  [java.util Collection]
  [org.dada.core Attribute Getter Metadata Model SessionManager ServiceFactory Update View]
  [org.eclipse.swt SWT]
  [org.eclipse.swt.custom CTabFolder CTabItem]
  [org.eclipse.swt.widgets Button Composite Control Display Shell Table TableColumn TableItem Text Listener Widget]
  [org.eclipse.swt.layout GridData GridLayout]
  [org.eclipse.swt.events ShellAdapter SelectionEvent SelectionListener]
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
;; utils

(defn swt-loop [#^Display display #^Shell shell]
  (loop []
    (if (.isDisposed shell)
      (.dispose display)
      (do
	(if (not (.readAndDispatch display))
	  (.sleep display))
	(recur)))))

(defn create-shell [#^Display display]
  (let [shell (Shell. display)]
    (.setLayout shell (GridLayout.))
    
    (.addShellListener 
     shell
     (proxy
      [ShellAdapter] []
      (shellClosed [evt] (try (doto shell (.setVisible false)(.dispose)) (catch Throwable t (.printStackTrace t))))))
    (.pack shell)
    (.open shell)

    shell))

;;--------------------------------------------------------------------------------

(defn execute-query [#^SessionManager session-manager query service-factory parent]
  (let [[[metadata name keys [operation & details]] [model & rest]] (.query session-manager "org.dada.dsl" query)
	shell (create-shell (if parent (.getDisplay parent) (Display.)))]
    (println "QUERY" query "->" model operation)
    (org.dada.swt.SWTView. model operation service-factory shell)
    shell))

;;--------------------------------------------------------------------------------

;; returns ...
(defmulti create (fn [operation model #^Composite parent service-factory] operation))

;;--------------------------------------------------------------------------------
;; table

(defn make-table-item [datum getters #^Table table]
  (let [#^TableItem item (TableItem. table (SWT/NONE))]
    (doall
     (map
      (fn [index #^Getter getter]
	  (.setText item index (str (.get getter datum)))
	  (.setData item (str index) datum))
      (iterate inc 0)
      getters))
    item))

(defn sort-table [#^Table table index #^Getter getter getters #^TableColumn column]
  (println "SORT TABLE" (.getText column) index)
  (let [items (.getItems table)
	comparator (proxy [java.util.Comparator][] (compare [lhs rhs] (.compareTo (.get getter lhs)(.get getter rhs))))
	sorted (sort comparator (map #(.getData %) items))]
    (println "SORTED" sorted)
    ;; rebuild all the items !! - there must be a better way...
    ;; THIS TRASHES THE MAP I AM HOLDING ON TABLE
    ;;(.removeAll table)
    ;;(doall (map (fn [datum] (make-table-item datum getters table)) sorted))
    ))

(defn make-table-column [#^Table table index getter getters #^String text]
  (let [ #^TableColumn column (TableColumn. table (SWT/NONE))]
    (.setText column text)
    (.addListener 
     column 
     (SWT/Selection)
     (proxy
      [Listener]
      []
      (handleEvent [evt] (try (sort-table table index getter getters column) (catch Throwable t (.printStackTrace t))))))
    column))

(defn blink-table-item [item index]
  (let [fg (.getForeground item index)
	bg (.getBackground item index)]
    (.setForeground item index bg)
    (.setBackground item index fg)))

(defn update-table-item [item old new getters]
  (doall
   (map
    (fn [index #^Getter getter]
	(let [new-value (.get getter new)]
	  (if (not (= (.get getter old) new-value))
	    (do
	      (println "UPDATE TABLE ITEM" old new index new-value)
	      (.setText item index (str new-value))
	      (.setData item (str index) new-value)
	      (blink-table-item item index)
	      ))))
    (iterate inc 0)
    getters)))

(defmethod create :data [operation model parent service-factory] 
  (let [#^Metadata metadata (.getMetadata model)
	attributes (.getAttributes metadata)
	#^Table table (Table. parent (SWT/SINGLE))
	titles (map (fn [#^Attribute attribute] (str (.getKey attribute))) attributes)
	getters (map (fn [#^Attribute attribute] (.getGetter attribute)) attributes)
	columns (doall (map (fn [index title getter] (make-table-column table index getter getters title)) (iterate inc 0) titles getters))
	primary-getter (.getPrimaryGetter metadata)
	version-comparator (.getVersionComparator metadata)
	]
    (.setLinesVisible table true)
    (.setHeaderVisible table true)
    (.setLayoutData table (GridData. (SWT/FILL) (SWT/FILL) true true))
    (doall (map (fn [#^TableColumn column](.pack column)) columns))
    ;; add listeners here
		      
    [ ;; widgets
     table
     ;; attach
     (fn [#^Model model #^View view]
	 (let [data (.registerView model view)]
	   (.update view (map (fn [datum] (Update. nil datum)) (.getExtant data)) '() (map (fn [datum] (Update. datum nil)) (.getExtinct data)))))
     ;; detach
     (fn [#^Model model #^View view]
	 (let [data (.registerView model view)]
	   (println "DEREGISTER - NYI:" data)))
     ;; update
     (fn [insertions alterations deletions]
	 (println "UPDATES" insertions alterations deletions)
	 (.asyncExec
	  (.getDisplay parent)
	  (fn []
	      (try
	       (if (not (.isDisposed table))
		 (do
		   (doall ;; dirty
		    (map
		     (fn [#^Update update]
			 (let [datum (.getNewValue update)
			       #^TableItem item (make-table-item datum getters table)]
			   (println "INSERT ITEM" table item)
			   ;; TODO - NEED TO SORT THIS OUT - IT FALLS OUT OF DATE WHEN ITEMS ARE REMOVED ETC
			   ;; ADD A DISPOSE LISTENER OR RETHINK
			   ;; SHOULD BE DONE IN MAKE ITEM

			   ;; DO WE REALLY WANT TO HODKL STATE ON THE WIDGETS ?
			   ;; CAN THIS BE DONE GENERICALLY FOR ALL UI REALISATIONS ?

			   (.setData
			    table
			    (str (.get primary-getter datum)) ;TODO: WARNING PK is an Object NOT a String
			    item)
			   (.setData item datum)
			   item			  
			   ))
		     insertions))
		   (doall ;; dirty
		    (map
		     (fn [#^Update update]
			 (let [new (.getNewValue update)
			       pk (str (.get primary-getter new)) ;TODO: WARNING PK is an Object NOT a String
			       #^TableItem item (.getData table pk)
			       old (.getData item)]
			   (println "UPDATE ITEM" table item)
			   (if (.higher version-comparator old new)
			     (update-table-item item old new getters))
			   item			  
			   ))
		     alterations))
		   (doall ;; dirty
		    (map
		     (fn [#^Update update]
			 (let [new (.getOldValue update)
			       pk (str (.get primary-getter new)) ;TODO: WARNING PK is an Object NOT a String
			       #^TableItem item (.getData table pk)]
			   (if item
			     (let [old (.getData item)]
			       (println "DELETE ITEM" table item)
			       (if (.higher version-comparator old new)
				 (.remove table (.indexOf table item))))
			     (println "WARN: DELETING UNKNOWN ITEM: " pk))
			   item			  
			   ))
		     deletions))
		   ;;(.pack table)
		   ;;(.pack parent)
		   ))
	       (catch Throwable t (.printStackTrace t))))
	  ))]
    ))

;;--------------------------------------------------------------------------------
;; tabs

(defn tab-select [#^CTabItem item]
  ;; (let [#^CTabItem item (.getSelection folder)
  ;; 	datum (.getData item)
  ;; 	model (first datum)
  ;; 	[old] (swap! selection (fn [[old current] new](println "SELECTION" current "->" new) [current new]) model)]
  ;;   (println "OLD" old)
  ;;   ;;(if old (.deregisterView model view))
  ;;   (if datum (.setControl item (.getControl (drill-down-fn folder datum))))
  ;;   (.pack folder))
  (println "SELECT" item)
  )

(defn tab-deselect [#^CTabItem item]
  (println "DESELECT" item)
  )

(defmethod create :split [operation model parent service-factory]
  (let [#^CTabFolder folder (CTabFolder. parent (reduce bit-and [(SWT/TOP)]))]
    (.setLayoutData folder (GridData. (SWT/FILL) (SWT/FILL) true true))
    (.addSelectionListener
     folder
     (proxy
      [SelectionListener]
      []
      (widgetSelected [#^SelectionEvent evt]
		      (try
		       (let [old (.getData folder)
			     new (.getSelection folder)]
			 (tab-deselect old)
			 (tab-select new)
			 (.setData folder new))  
		       (catch Throwable t (.printStackTrace t))))))
		      
    [ ;; widgets
     folder
     ;; attach
     (fn [#^Model model #^View view]
	 (let [data (.registerView model view)]
	   (.update view (map (fn [datum] (Update. nil datum)) (.getExtant data)) '() (map (fn [datum] (Update. datum nil)) (.getExtinct data)))
	   (.setSelection folder 0)))
     ;; detach
     (fn [#^Model model #^View view]
	 (let [data (.registerView model view)]
	   (println "DEREGISTER - NYI:" data)))
     ;; update
     (fn [insertions alterations deletions]
	 (.asyncExec
	  (.getDisplay parent)
	  (fn []
	      (if (not (.isDisposed folder))
	      	(do
	      	  (doall ;; dirty
	      	   (map
	      	    (fn [#^Update update]
	      		(let [[#^Model model details] (.getNewValue update) ;TODO - only works for metamodels
			      key (second (last details));TODO - only works for metamodels
	      		      #^CTabItem item (CTabItem. folder (reduce bit-and [(SWT/CLOSE)]))]
	      		  (.setText item key) 
			  (.setData folder nil) ; selection
	      		  (.setData folder key model);; key:model - key should be unique within this split
			  (let [view (org.dada.swt.SWTView.  model :data service-factory folder)
				control (.getControl view)]
			    ;;(.setData item 
			     (.setControl item control)
			     (.setLayoutData control (GridData. (SWT/FILL) (SWT/FILL) true true))
			    )))
	      	    insertions))
		  (if (or (not (empty? alterations))(not (empty? deletions)))
		    (println "TABS: alteration/deletion NYI"))
	      	  (.pack parent))))
	  ))]
    ))

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
