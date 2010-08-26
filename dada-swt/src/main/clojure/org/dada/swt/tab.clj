(ns 
 org.dada.swt.tab
 (:use org.dada.swt.utils)
 (:use org.dada.swt.nattable)
 (:import
  [java.util Collection Comparator Timer TimerTask]
  [org.dada.core Attribute Getter Metadata Model ModelView SessionManager SimpleModelView ServiceFactory Update View]
  [org.eclipse.swt SWT]
  [org.eclipse.swt.custom CTabFolder CTabItem]
  [org.eclipse.swt.widgets Button Composite Control Display Shell Table TableColumn TableItem Text Listener Widget]
  [org.eclipse.swt.layout GridData GridLayout]
  [org.eclipse.swt.events ShellAdapter SelectionEvent SelectionListener]
  )
 )

;;--------------------------------------------------------------------------------
;; tabs

;; (defn tab-select [#^CTabItem item]
;;   ;; (let [#^CTabItem item (.getSelection folder)
;;   ;; 	datum (.getData item)
;;   ;; 	model (first datum)
;;   ;; 	[old] (swap! selection (fn [[old current] new](println "SELECTION" current "->" new) [current new]) model)]
;;   ;;   (println "OLD" old)
;;   ;;   ;;(if old (.deregisterView model view))
;;   ;;   (if datum (.setControl item (.getControl (drill-down-fn folder datum))))
;;   ;;   (.pack folder))
;;   (println "SELECT" item)
;;   )

;; (defn tab-deselect [#^CTabItem item]
;;   (println "DESELECT" item)
;;   )

;; (defmethod create :split [operation model #^Composite parent service-factory]
;;   (let [#^CTabFolder folder (CTabFolder. parent (reduce bit-and [(SWT/TOP)]))]
;;     (.setLayoutData folder (GridData. (SWT/FILL) (SWT/FILL) true true))
;;     (.addSelectionListener
;;      folder
;;      (proxy
;;       [SelectionListener]
;;       []
;;       (widgetSelected [#^SelectionEvent evt]
;; 		      (try
;; 		       (let [old (.getData folder)
;; 			     new (.getSelection folder)]
;; 			 (tab-deselect old)
;; 			 (tab-select new)
;; 			 (.setData folder new))  
;; 		       (catch Throwable t (.printStackTrace t))))))
		      
;;     [ ;; widgets
;;      folder
;;      ;; attach
;;      (fn [#^Model model #^View view]
;; 	 (let [data (.registerView model view)]
;; 	   (.update view (map (fn [datum] (Update. nil datum)) (.getExtant data)) '() (map (fn [datum] (Update. datum nil)) (.getExtinct data)))
;; 	   (.setSelection folder 0)))
;;      ;; detach
;;      (fn [#^Model model #^View view]
;; 	 (let [data (.registerView model view)]
;; 	   (println "DEREGISTER - NYI:" data)))
;;      ;; update
;;      (fn [insertions alterations deletions]
;; 	 (.asyncExec
;; 	  (.getDisplay parent)
;; 	  (fn []
;; 	      (if (not (.isDisposed folder))
;; 	      	(do
;; 	      	  (doall ;; dirty
;; 	      	   (map
;; 	      	    (fn [#^Update update]
;; 	      		(let [[#^Model model details] (.getNewValue update) ;TODO - only works for metamodels
;; 			      key (second (last details));TODO - only works for metamodels
;; 	      		      #^CTabItem item (CTabItem. folder (reduce bit-and [(SWT/CLOSE)]))]
;; 	      		  (.setText item (str key)) ;TODO - use of 'str' again
;; 			  (.setData folder nil) ; selection
;; 	      		  (.setData folder (str key) model);; key:model - key should be unique within this split - TODO - use of str
;; 			  (let [view (org.dada.swt.SWTView.  model :data service-factory folder)
;; 				control (.getControl view)]
;; 			    ;;(.setData item 
;; 			     (.setControl item control)
;; 			     (.setLayoutData control (GridData. (SWT/FILL) (SWT/FILL) true true))
;; 			    )))
;; 	      	    insertions))
;; 		  (if (or (not (empty? alterations))(not (empty? deletions)))
;; 		    (println "TABS: alteration/deletion NYI"))
;; 	      	  (.pack parent))))
;; 	  ))]
;;     ))

(defn insert-tab-meta-view [#^Composite parent [#^Model model details]]
  (println "META INSERT" model)
   (let [key (second (last details)) ;TODO - only works for metamodels
	 #^CTabItem item (CTabItem. parent (reduce bit-and [(SWT/CLOSE)]))]
     (.setText item (str key))		;TODO - use of 'str' again
     (.setControl item (make-nattable model parent))
     )
)

(defn update-tab-meta-view [#^Composite parent insertions _ deletions]
  (doall (map (fn [insertion] (insert-tab-meta-view parent (.getNewValue insertion))) insertions))
  (.setSelection parent 0)
  (.pack parent))
									 
(defn make-tab-meta-view [#^Model model #^Composite parent]
  ;; this model will accept unordered async events and put out ordered
  ;; sync events, suitable for the gui...
  (let [display (.getDisplay parent)
	#^CTabFolder folder (CTabFolder. parent (reduce bit-and [(SWT/TOP)]))]
    (.setLayoutData folder (GridData. (SWT/FILL) (SWT/FILL) true true))
    (register model (proxy [View] [] (update [i a d] (update-tab-meta-view folder i a d))))
    folder
    ))
  
  ;; make a View
  ;; connect it to a Model to handle events arriving in wrong order
  ;; connect that to a View (Meta) which maintains a new Tab for each item
  ;; each Tab should do the same for each element of each Model


