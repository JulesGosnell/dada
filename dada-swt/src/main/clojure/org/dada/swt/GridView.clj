(ns 
 org.dada.swt.GridView
 (:import
  [java.util Collection]
  [org.dada.core Metadata Model SessionManager ServiceFactory Update View]
  [org.eclipse.swt SWT]
  [org.eclipse.swt.widgets Button Display Shell Table TableColumn TableItem Text Listener]
  [org.eclipse.swt.layout GridData GridLayout]
  [org.eclipse.swt.events ShellAdapter SelectionEvent SelectionListener]
  )
 (:gen-class
  :implements [org.dada.core.View java.io.Serializable]
  :constructors {[org.dada.core.Model org.dada.core.SessionManager org.dada.core.ServiceFactory org.eclipse.swt.widgets.Display] []}
  :methods [[start [] void][writeReplace [] Object]]
  :init init
  :post-init post-init
  :state state
  )
 )

;;--------------------------------------------------------------------------------

(defn swt-loop [display shell]
  (loop []
    (if (.isDisposed shell)
      (.dispose display)
      (do
	(if (not (.readAndDispatch display))
	  (.sleep display))
	(recur)))))

(defn make-column [#^Table table #^String text]
  (let [ #^TableColumn column (TableColumn. table (SWT/NONE))]
    (.setText column text)
    column))

;;--------------------------------------------------------------------------------

(defn make-grid-view [model session-manager service-factory & [display]]
  (org.dada.swt.GridView. model session-manager service-factory (or display (Display.))))

(defn execute-query [session-manager query service-factory display]
  (println "QUERY" query)
  (make-grid-view 
   (.query session-manager "org.dada.dsl" query)
   session-manager
   service-factory
   display)
  )

;;--------------------------------------------------------------------------------

(defn close [#^Model model #^View view]
  (if (instance? View view)
    (.deregisterView model view)
    (println "view is not a View!:" view "cannot be deregistered...")))

;;--------------------------------------------------------------------------------

(defmulti open (fn [_ _ _ _ _ _ data] (class data)))

(defmethod open Model [#^Model model session-manager service-factory display table evt #^Model data]
  (make-grid-view data session-manager service-factory display))

(defmethod open Collection [#^Model model session-manager service-factory display table evt #^Collection data]
  (doall
   (map
    (fn [datum]
	(open model session-manager service-factory display table evt datum))
    data)))

(defmethod open :default [_ _ _ _ _ _ _])

(defn drill-down [#^Model model session-manager service-factory display table evt]
  (let [table-items (.getSelection table)]
    (doall
     (map
      (fn [table-item]
	  (println table-item)
	  (doall
	   (map
	    (fn [index]
		(println table-item index)
		(open model session-manager service-factory display table evt (.getData table-item (str index))))
	    (range (count (.getAttributes (.getMetadata model))))
	    )))
      (.getSelection table)))))

;;--------------------------------------------------------------------------------

(defn create [model session-manager service-factory display]
  (let [#^String model-name (.getName model)
	#^Metadata metadata (.getMetadata model)
	#^Shell shell (Shell. display)
	#^Table table (Table. shell (SWT/SINGLE))
	;; add metadata
	titles (map (fn [attribute] (str (.getKey attribute))) (.getAttributes metadata))
	columns (doall (map #(make-column table %) titles))]
    
    (.setText shell model-name)
    
    (.addShellListener 
     shell
     (proxy
      [ShellAdapter] []
      (shellClosed [evt] (try (doto shell (.setVisible false)(.dispose)) (catch Throwable t (.printStackTrace t))))))
    
    (.setLayout shell (GridLayout.))

    (.setLinesVisible table true)
    (.setHeaderVisible table true)
    (.setLayoutData table (GridData. (SWT/FILL) (SWT/FILL) true true))

    (let [#^Text text (Text. shell (reduce bit-and [(SWT/LEFT)(SWT/SINGLE)]))]
      (doto
	  text
	(.setLayoutData (GridData. (SWT/FILL) (SWT/FILL) true false))
	)
      (doto
	  #^Button (Button. shell (reduce bit-and [(SWT/PUSH)(SWT/CENTER)]))
	  (.setLayoutData (GridData. (SWT/FILL) (SWT/FILL) true false))
	  (.setText "Send Query")
	  (.addSelectionListener
	   (proxy [SelectionListener] []
		  (widgetSelected [#^SelectionEvent evt]
				  (try (execute-query session-manager (.getText text) service-factory display)(catch Throwable t (.printStackTrace t))))))))
    
    ;;(println (map #(.getName %) (.getMethods (type table))))

    ;; (.addListener 
    ;;  table
    ;;  (SWT/Selection)
    ;;  (proxy [Listener] [] (handleEvent [evt] (println "Selection" evt))))

    (.addListener
     table
     (SWT/DefaultSelection)
     (proxy [Listener] [] (handleEvent [evt] (try (drill-down model session-manager service-factory display table evt) (catch Throwable t (.printStackTrace t))))))

    ;; insert item at index
    ;;	TableItem item = new TableItem (table, SWT.NONE, 1);
    ;;	item.setText ("*** New Item " + table.indexOf (item) + " ***");

    ;; remove item from table
    ;;  table.remove (table.getSelectionIndices ());

    (.pack shell)
    (.open shell)
    [shell table columns]))

;; Executing code from a non-UI thread

;; Applications that wish to call UI code from a non-UI thread must provide a Runnable that calls the UI code. The methods syncExec(Runnable) and asyncExec(Runnable) in the Display class are used to execute these runnables in the UI thread during the event loop.

;;     * syncExec(Runnable) should be used when the application code in the non-UI thread depends on the return value from the UI code or otherwise needs to ensure that the runnable is run to completion before returning to the thread. SWT will block the calling thread until the runnable has been run from the application's UI thread. For example, a background thread that is computing something based on a window's current size would want to synchronously run the code to get the window's size and then continue with its computations.
;;     * asyncExec(Runnable) should be used when the application needs to perform some UI operations, but is not dependent upon the operations being completed before continuing. For example, a background thread that updates a progress indicator or redraws a window could request the update asynchronously and continue with its processing. In this case, there is no guaranteed relationship between the timing of the background thread and the execution of the runnable.


;;--------------------------------------------------------------------------------

(defn -init [#^Model model #^SessionManager session-manager #^ServiceFactory service-factory #^Display display]
  [ ;; super ctor args
   []
   ;; instance state
   (let [#^Metadata metadata (.getMetadata model)
	 [shell table columns] (create model session-manager service-factory display)
	 getters (map (fn [attribute] (.getGetter attribute)) (.getAttributes metadata))]
     [metadata display shell table columns getters session-manager service-factory display])
   ])

(defn -post-init [#^org.dada.swt.GridView this #^Model model #^SessionManager session-manager #^ServiceFactory service-factory #^Display display]
  (println "POST-INIT" this)
  ;; register us with model
  (let [data (.registerView model this)]
  (println "POST-INIT" this)
    ;; pump initial data in...
    (.update this (map (fn [datum] (Update. nil datum)) (.getExtant data)) '() (map (fn [datum] (Update. datum nil)) (.getExtinct data))))
  (let [[metadata display shell table columns getters session-manager service-factory] (.state this)]
  (println "POST-INIT" this)
  (let [grid-view this]			;"this" is implicitly bound inside proxy and was overiding grid-view.this - nasty !
    (.addShellListener shell (proxy [ShellAdapter] [] (shellClosed [evt] (try (close model grid-view) (catch Throwable t (.printStackTrace t)))))))))

(defn -update [this insertions alterations deletions]
  (let [[metadata display shell table columns getters session-manager service-factory] (.state this)]
    (.asyncExec
     display
     (fn []
	 (if (not (.isDisposed table))
	   (do
	     (doall ;; dirty
	      (map
	       (fn [update]
		   (let [datum (.getNewValue update)
			 #^TableItem item (TableItem. table (SWT/NONE))]
		     (doall
		      (map
		       (fn [index getter]
			   ;;(println (type getter))
			   (.setText
			    item
			    index
			    (str
			     (try
			      (.get getter datum)
			      (catch Exception e (println (.getMessage e)) datum))
			     ))
			   (.setData
			    item
			    (str index)
			    datum))
		       (iterate inc 0)
		       getters))
		     ))
	       insertions))
	     (doall (map	#(.pack %) columns))))))))

(defn -start [this]
  (let [[metadata display shell table columns getters session-manager service-factory] (.state this)]
    (swt-loop display shell)))

;;--------------------------------------------------------------------------------

(defn #^{:private true} -writeReplace [#^org.dada.swt.GridView this]
  (let [[_ _ _ _ _ _ _ service-factory] (.state this)]
    (.decouple service-factory  this)))
