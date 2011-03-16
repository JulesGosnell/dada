(ns
 org.dada.swt.shell
 (:import
  [java.util.concurrent CountDownLatch]
  [java.util.concurrent.locks Lock ReentrantLock]
  [org.eclipse.swt.widgets Display Shell]
  [org.eclipse.swt.layout GridLayout]
  [org.eclipse.swt.events ShellAdapter]
  [org.dada.core Model Update View]
  ))

;;--------------------------------------------------------------------------------
;; utils
;;--------------------------------------------------------------------------------

(defmacro with-lock [lock & body]
  `(let [^Lock lock# ~lock]
     (.lock lock#)
     (try ~@body (finally (.unlock lock#)))))

(defn event-loop [^Display display]
  (println "Starting SWT Event Loop")
  (loop []
    (if (not (.isDisposed display))
      (do
	(if (not (.readAndDispatch display))
	  (.sleep display))
	(recur))))
  (println "Stopping SWT Event Loop"))

;; I know that it is horrible to use locks here - but creating a
;; Display has serious side effects !
(let [lock (ReentrantLock.)
      display-state (atom [0 nil])]
  
  (defn inc-display []
    (with-lock
     lock
     (let [[n display] (swap! display-state (fn [[n display]] [(inc n) display]))]
       (if (= n 1)
	 (let [latch (CountDownLatch. 1)]
	   (.start
	    (Thread.
	     (fn []
		 (println "Creating SWT Display")
		 (let [[n display] (swap! display-state (fn [[n display]] [1 (Display.)]))]
		   (.countDown latch)
		   (event-loop  display)))))
	   (.await latch)
	   (second @display-state))
	 display))))

  (defn dec-display []
    (with-lock
     lock
     (let [[n display] (swap! display-state (fn [[n display]] [(dec n) display]))]
       (if (zero? n)
	 (do (.syncExec display (fn [] (println "Desstroying SWT Display") (.dispose display))) nil)
	 display))))

  )

;;--------------------------------------------------------------------------------

(defn create-shell [#^Display display #^String text & [close-fn]]
  (let [shell (Shell. display)
	layout (GridLayout.)]
    (doto shell
      (.setText text)
      (.setLayout layout)
      (.addShellListener 
       (proxy
	[ShellAdapter] []
	(shellClosed [evt]
		     (try
		      (doseq [child (.getChildren shell)](.dispose child)) ;why is this not done for us ?
		      (doto shell (.setVisible false) (.dispose))
		      (close-fn shell)
		      (catch Throwable t (.printStackTrace t))))))
      (.pack)
      (.open))
    shell))
