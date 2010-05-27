(ns org.dada.swt.gui
    (:import
     (java.io Serializable)
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

(defn begin [metadata data]
  (let [#^Display display (Display.)
	#^Shell shell (Shell. display)
	flags (reduce bit-or [(SWT/MULTI) (SWT/BORDER) (SWT/FULL_SELECTION)])
	#^Table table (Table. shell flags)
	
	;; add metadata
	titles (map str (.getAttributeKeys metadata))
	columns (doall (map #(make-column table %) titles))]
    
    (.setLayout shell (GridLayout.))
    (.setLinesVisible table true)
    (.setHeaderVisible table true)
    (.setLayoutData table (GridData. (SWT/FILL) (SWT/FILL) true true))
    
    ;; add data
    (doall		   ;TODO - use a fn that implies a side-effect
     (map
      (fn [datum]
	  (let [#^TableItem item (TableItem. table (SWT/NONE))]
	    (.setText item 0 (str datum))
	    ))
      data))
    
    (doall (map	#(.pack %) columns))
     
    ;; (.addListener 
    ;;  table
    ;;  (SWT/Selection)
    ;;  (proxy [Listener] [] (handleEvent [evt] (println "Selection" evt))))

    (.addListener
     table
     (SWT/DefaultSelection)
     (proxy [Listener] [] (handleEvent [evt] (println "DefaultSelection" (map #(.getText %) (.getSelection table))))))

    ;; insert item at index
    ;;	TableItem item = new TableItem (table, SWT.NONE, 1);
    ;;	item.setText ("*** New Item " + table.indexOf (item) + " ***");

    ;; remove item from table
    ;;  table.remove (table.getSelectionIndices ());

    (.pack shell)
    (.open shell)
    (swt-loop display shell)))

;; Executing code from a non-UI thread

;; Applications that wish to call UI code from a non-UI thread must provide a Runnable that calls the UI code. The methods syncExec(Runnable) and asyncExec(Runnable) in the Display class are used to execute these runnables in the UI thread during the event loop.

;;     * syncExec(Runnable) should be used when the application code in the non-UI thread depends on the return value from the UI code or otherwise needs to ensure that the runnable is run to completion before returning to the thread. SWT will block the calling thread until the runnable has been run from the application's UI thread. For example, a background thread that is computing something based on a window's current size would want to synchronously run the code to get the window's size and then continue with its computations.
;;     * asyncExec(Runnable) should be used when the application needs to perform some UI operations, but is not dependent upon the operations being completed before continuing. For example, a background thread that updates a progress indicator or redraws a window could request the update asynchronously and continue with its processing. In this case, there is no guaranteed relationship between the timing of the background thread and the execution of the runnable.


(import org.apache.activemq.ActiveMQConnectionFactory)
(import javax.jms.ConnectionFactory)
(import javax.jms.Connection)
(import javax.jms.Session)
(import org.dada.jms.RemotingFactory)
(import org.dada.core.MetaModel)
(import org.dada.core.View)
(import java.rmi.server.UID)
(import java.util.concurrent.ExecutorService)
(import java.util.concurrent.Executors)


(def server-name "Cetacea.MetaModel")
(def url "tcp://localhost:61616")
(def connection-factory (ActiveMQConnectionFactory. url))
(def connection (.createConnection connection-factory))
(.start connection)
(def session (.createSession connection false (Session/AUTO_ACKNOWLEDGE)))

;; create proxy to server-side metamodel

(def clientside-metamodel-proxy
     (.createSynchronousClient 
      (RemotingFactory. session MetaModel, 10000) 
      (.createQueue session server-name)
      true))

;; create proxy for ourselves to send to server

(def client-destination (.createQueue session (str "Client" (UID.))))
(def client-view (proxy [View] [] (update [insertions alterations deletions]
					  (println ("VIEW:" insertions alterations deletions)))))

(def view-remoting-factory (RemotingFactory. session View 10000))

(def clientside-view-server
     (.createServer
      view-remoting-factory 
      client-view
      client-destination
      (Executors/newFixedThreadPool 20)))

(def serverside-view-proxy
     (.createSynchronousClient
      view-remoting-factory
      client-destination
      true))

;; register our interest in the metamodel
(def registration (.registerView clientside-metamodel-proxy server-name serverside-view-proxy))

(begin (.getMetadata registration) (.getData registration))


