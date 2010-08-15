(ns
 org.dada.swt.TabFolderView
 (:import
  (org.eclipse.swt SWT)
  (org.eclipse.swt.widgets Display Shell TabFolder TabItem Table TableItem)
  (org.eclipse.swt.layout GridData GridLayout)
  (org.eclipse.swt.events ShellAdapter)
  )
 )

(println "Starting...")

(def display (Display.))

(def shell (Shell. display))
(def layout (GridLayout.))
(.setLayout shell layout)
(.setText shell "TabDemo")


(def folder (TabFolder. shell 0))
(.setVisible folder true)

(doall
 (map
  (fn [index]
      (let [item (TabItem. folder 0 index)
	    table (Table. folder (reduce bit-or [(SWT/MULTI) (SWT/BORDER) (SWT/FULL_SELECTION)]))
	    row (TableItem. table (SWT/NONE))]
	(.setText item (str "Tab[" index "]"))
	(.setLinesVisible table true)
	(.setHeaderVisible table true)
	(.setLayoutData table (GridData. (SWT/FILL) (SWT/FILL) true true))
	(.setText row 0 (str "Row[" index "]"))
	(.setControl item table)
	))
  (range 10)))

(.pack shell)
(.setVisible shell true)
(.open shell)

(defn swt-loop [display shell]
  (loop []
    (if (.isDisposed shell)
      (.dispose display)
      (do
	(if (not (.readAndDispatch display))
	  (.sleep display))
	(recur)))))

(Thread/sleep 2000)
(swt-loop display shell)
;;(.start (Thread. (fn [] )))


