(ns org.dada.swt.gui
    (:import
     (org.eclipse.swt SWT)
     (org.eclipse.swt.widgets Display Shell Table TableColumn TableItem Listener)
     (org.eclipse.swt.layout GridData GridLayout)
     (org.eclipse.swt.events ShellAdapter)))

(defn create-shell [display shell]
  (let [layout (GridLayout.)]
    (doto shell
      (.setText "DADA GUI")
      (.setLayout layout)
      (.addShellListener (proxy [ShellAdapter] [] (shellClosed [evt] (System/exit 0)))))))

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

(defn begin []
  (let [#^Display display (Display.)
	#^Shell shell (Shell. display)
	flags (reduce bit-or [(SWT/MULTI) (SWT/BORDER) (SWT/FULL_SELECTION)])
	#^Table table (Table. shell flags)
	;; plug in metadata here...
	titles [" ", "C", "!", "Description", "Resource", "In Folder", "Location"]
	columns (doall (map #(make-column table %) titles))]
    
    (.setLayout shell (GridLayout.))
    (.setLinesVisible table true)
    (.setHeaderVisible table true)
    (.setLayoutData table (GridData. (SWT/FILL) (SWT/FILL) true true))
    
    (dotimes [i 128]
	(let [#^TableItem item (TableItem. table (SWT/NONE))]
	  (.setText item 0 "x")
	  (.setText item 1 "y")
	  (.setText item 2 "!")
	  (.setText item 3 "this stuff behaves the way I expect")
	  (.setText item 4 "almost everywhere")
	  (.setText item 5 "some.folder")
	  (.setText item 6 (str "line " i " in nowhere"))
	  ))
    
    (doall (map	#(.pack %) columns))
     
    ;; (.addListener 
    ;;  table
    ;;  (SWT/Selection)
    ;;  (proxy [Listener] [] (handleEvent [evt] (println "Selection" evt))))

    (.addListener
     table
     (SWT/DefaultSelection)
     (proxy [Listener] [] (handleEvent [evt] (println "DefaultSelection" (.getSelection table)))))

    ;; insert item at index
    ;;	TableItem item = new TableItem (table, SWT.NONE, 1);
    ;;	item.setText ("*** New Item " + table.indexOf (item) + " ***");

    ;; remove item from table
    ;;  table.remove (table.getSelectionIndices ());

    (.pack shell)
    (.open shell)
    (swt-loop display shell)))

(begin)

;; Executing code from a non-UI thread

;; Applications that wish to call UI code from a non-UI thread must provide a Runnable that calls the UI code. The methods syncExec(Runnable) and asyncExec(Runnable) in the Display class are used to execute these runnables in the UI thread during the event loop.

;;     * syncExec(Runnable) should be used when the application code in the non-UI thread depends on the return value from the UI code or otherwise needs to ensure that the runnable is run to completion before returning to the thread. SWT will block the calling thread until the runnable has been run from the application's UI thread. For example, a background thread that is computing something based on a window's current size would want to synchronously run the code to get the window's size and then continue with its computations.
;;     * asyncExec(Runnable) should be used when the application needs to perform some UI operations, but is not dependent upon the operations being completed before continuing. For example, a background thread that updates a progress indicator or redraws a window could request the update asynchronously and continue with its processing. In this case, there is no guaranteed relationship between the timing of the background thread and the execution of the runnable.
