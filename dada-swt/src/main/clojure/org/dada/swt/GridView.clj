(ns 
 org.dada.swt.GridView
 (:import
  [org.dada.core Metadata MetaModel SessionManager ServiceFactory UnionModel Update View]
  [org.eclipse.swt SWT]
  [org.eclipse.swt.widgets Button Display Shell Table TableColumn TableItem Text Listener]
  [org.eclipse.swt.layout GridData GridLayout]
  [org.eclipse.swt.events ShellAdapter SelectionEvent SelectionListener]
  )
 (:gen-class
  :implements [org.dada.core.View]
  :constructors {[String org.dada.core.Metadata org.dada.core.SessionManager org.dada.core.ServiceFactory org.eclipse.swt.widgets.Display] []}
  :methods [[start [] void]]
  :init init
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

(defn make-grid-view [model-name session-manager service-factory & [display]]
  (let [display (or display (Display.))
	;; get metadata
	metadata (.getMetadata session-manager model-name)
	;; make a View
	view (org.dada.swt.GridView. model-name metadata session-manager service-factory display)
	;; register a proxy for the View with the remote Model
	data (.registerView session-manager model-name (.decouple service-factory  view))]
    ;; pump initial data in...
    (.update view (map (fn [datum] (Update. nil datum)) (.getExtant data)) '() (map (fn [datum] (Update. datum nil)) (.getExtinct data)))
    view))

(defn execute-query [session-manager query service-factory display]
  (println "QUERY" query)
  (doall
   (map
    (fn [model-name] (make-grid-view model-name session-manager service-factory display))
    (.query session-manager "org.dada.dsl" query))))

;;--------------------------------------------------------------------------------


(defn create [model-name metadata session-manager service-factory display]
  (let [#^Shell shell (Shell. display)
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
     (proxy [Listener] []
	    (handleEvent [evt]
			 (try
			  (let [selection (map #(.getText %) (.getSelection table))]
			    (println "DefaultSelection" selection)
			    (make-grid-view (first selection) session-manager service-factory display)
			    )
			  (catch Throwable t (.printStackTrace t))))))
     

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

(defn -init [#^String model-name #^Metadata metadata #^SessionManager session-manager #^ServiceFactory service-factory #^Display display]
  [ ;; super ctor args
   []
   ;; instance state
   (let [[shell table columns] (create model-name metadata session-manager service-factory display)
	 getters (map (fn [attribute] (.getGetter attribute)) (.getAttributes metadata))]
     [metadata display shell table columns getters session-manager service-factory display])
   ])

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
			     )))
		       (iterate inc 0)
		       getters))
		     ))
	       insertions))
	     (doall (map	#(.pack %) columns))))))))

(defn -start [this]
  (let [[metadata display shell table columns getters session-manager service-factory] (.state this)]
    (swt-loop display shell)))
