(ns
 org.dada.swt.utils
 (:import
  [org.eclipse.swt.widgets Display Shell]
  [org.eclipse.swt.layout GridLayout]
  [org.eclipse.swt.events ShellAdapter]
  [org.dada.core Model Update View]
  ))

;;--------------------------------------------------------------------------------
;; utils

(defn shell-loop [#^Shell shell]
   (loop []
     (if (not (.isDisposed shell))
       (let [display (.getDisplay shell)]
	 (if (not (.readAndDispatch display))
	   (.sleep display))
	 (recur)))))

(defn swt-loop
  ([#^Display display #^Shell shell]
   (loop []
     (if (.isDisposed shell)
       (.dispose display)
       (do
	 (if (not (.readAndDispatch display))
	   (.sleep display))
	 (recur)))))
  ([#^Display display]
   (loop []
     (do
       (if (not (.readAndDispatch display))
	 (.sleep display))
       (recur)))))

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
		      (catch Throwable t (.printStackTrace t))))))
      (.pack)
      (.open))
    shell))

(defn register [#^Model model #^View view]
  (let [data (.registerView model view)]
    (.update
     view
     (map (fn [datum] (Update. nil datum)) (.getExtant data))
     '()
     (map (fn [datum] (Update. datum nil)) (.getExtinct data)))
    data))

;;--------------------------------------------------------------------------------
