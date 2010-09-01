(ns
 org.dada.swt.nattable
 (:use [org.dada core]
       [org.dada.swt swt utils])
 (:import
  [java.util ArrayList Collection]
  [org.eclipse.swt SWT]
  [org.eclipse.swt.layout GridData GridLayout]
  [org.eclipse.swt.widgets Composite Display Shell]
  [ca.odell.glazedlists GlazedLists SortedList]
  [net.sourceforge.nattable NatTable]
  [net.sourceforge.nattable.blink BlinkLayer]
  [net.sourceforge.nattable.config ConfigRegistry DefaultNatTableStyleConfiguration]
  [net.sourceforge.nattable.data ListDataProvider IColumnPropertyAccessor IRowIdAccessor]
  [net.sourceforge.nattable.extension.glazedlists GlazedListsEventLayer GlazedListsSortModel]
  [net.sourceforge.nattable.grid.data DefaultColumnHeaderDataProvider DefaultCornerDataProvider DefaultRowHeaderDataProvider]
  [net.sourceforge.nattable.grid.layer ColumnHeaderLayer CornerLayer DefaultColumnHeaderDataLayer DefaultRowHeaderDataLayer GridLayer RowHeaderLayer]
  [net.sourceforge.nattable.hideshow ColumnHideShowLayer]
  [net.sourceforge.nattable.layer AbstractLayerTransform DataLayer]
  [net.sourceforge.nattable.layer.stack DefaultBodyLayerStack]
  [net.sourceforge.nattable.reorder ColumnReorderLayer]
  [net.sourceforge.nattable.selection SelectionLayer]
  [net.sourceforge.nattable.selection.config DefaultSelectionStyleConfiguration]
  [net.sourceforge.nattable.sort SortHeaderLayer]
  [net.sourceforge.nattable.sort.config SingleClickSortConfiguration]
  [net.sourceforge.nattable.viewport ViewportLayer]

  [org.dada.core Attribute Metadata Model Update View]
  ))

;;--------------------------------------------------------------------------------
;; hopefully this code will bridge between a DADA View and a GL EventList - untested...
;;--------------------------------------------------------------------------------
;; (defn #^EventList make-dada-gl-event-list [#^Model model]
;;   (let [extant (ArrayList.)
;; 	extinct (ArrayList.)
;; 	publisher (ListEventAssembler/createListEventPublisher)
;; 	read-write-lock (.createReadWriteLock (J2SE50LockFactory.))
;; 	list-event-assenbler (atom nil)
;; 	pk-fn (.getPrimaryGetter (.getMetadata model))
;; 	pk-comparator (proxy [Comparable][]
;; 			     (#^int compareTo [#^Comparable lhs #^Comparable rhs] (.compareTo (pk-fn lhs)(pk-fn rhs)))
;; 			     (#^boolean equals [lhs rhs] (.equals  (pk-fn lhs)(pk-fn rhs))))
;; 	insert (fn [#^Update update]
;; 		   (let [datum (.getNewValue update)
;; 			 index (- (Math/abs (Collections/binarySearch extant datum pk-comparator)) 1)]
;; 		     (.insert extant index datum)
;; 		     (.elementInserted list-event-assenbler index datum)))
;; 	alter (fn [#^Update update]
;; 		  (let [datum (.getNewValue update)
;; 			index (Collections/binarySearch extant datum pk-comparator)
;; 			old-value (.get extant index)]
;; 		    (.set extant index datum)
;; 		    (.elementUpdated list-event-assenbler index datum)))
;; 	delete (fn [#^Update update]
;; 		   (let [datum (.getOldValue update)
;; 			 index (Collections/binarySearch extant datum pk-comparator)
;; 			 old-value (.remove extant index)]
;; 		     (.elementDeleted list-event-assenbler index old-value datum)))
;; 	event-list (proxy [EventList][]
;; 			  ;; 1
;; 			  (#^ListEventPublisher getPublisher [] publisher)
;; 			  ;; 2
;; 			  (#^ReadWriteLock getReadWriteLock [] read-write-lock)
;; 			  ;; 3
;; 			  (#^int size [] (.size extant))
;; 			  ;; 4
;; 			  (#^void addListEventListener [#^ListEventListener listener] (.addListEventListener @list-event-assenbler listener))
;; 			  ;; 5
;; 			  (#^Objectget [#^int index] (.get extant index)))
;; 	view (proxy [View][]
;; 		    (#^void update [#^Collection indertions  #^Collection alterations #^Collection deletions]
;; 			    (doall (map insert insertions))
;; 			    (doall (map alter alterations))
;; 			    (doall (map delete deletions))))]
;;     (swap! list-event-assenbler (fn [old new] new) (ListEventAssembler. event-list publisher))
;;     (doall (map insert (.getExtant (.registerView model view))))
;;     event-list))

;; (def event-list (make-dada-gl-event-list my-model))

;;--------------------------------------------------------------------------------
;; see http://nattable.org/drupal/docs/basicgrid
;; Sorted Table
;; see http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/SortableGridExample.java?revision=3912&view=markup
;; Blinking Table
;; http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/BlinkingGridExample.java?revision=3912&view=markup

(defn nattable-make [[#^Model model pairs] #^Composite parent]
  (let [#^Metadata metadata (.getMetadata model)
	attributes (.getAttributes metadata)
	getters (map (fn [#^Attribute attribute] (.getGetter attribute)) attributes)
	pk-getter (.getPrimaryGetter metadata)
	property-names (into-array String (map (fn [attribute] (.toString (.getKey attribute))) attributes))
	property-name-to-index (apply array-map (interleave property-names (iterate inc 0)))
	property-name-to-label (apply array-map (interleave property-names property-names)) 
	data (.getExtant (.getData model))
	event-list (GlazedLists/eventList data)
	sorted-list (SortedList. event-list nil)
      
	column-property-accessor
	(proxy 
	 [IColumnPropertyAccessor]
	 []
	 ;; IColumnAccessor<T>
	 (#^Object getDataValue [#^Collection rowObject #^int columnIndex] (pr-str (.get (nth getters columnIndex) rowObject))) ;TODO - I'd rather not use pr-str here...
	 (#^int getColumnCount [] (count property-names))
	 ;; public void setDataValue(T rowObject, int columnIndex, Object newValue);
      
	 ;;IColumnPropertyResolver 
	 (#^String getColumnProperty [int columnIndex] (nth property-names columnIndex))
	 (#^int getColumnIndex [#^String propertyName] (property-name-to-index propertyName))
	 )

	config-registry (ConfigRegistry.)
	body-data-provider (ListDataProvider. sorted-list column-property-accessor)

	body-layer-stack (DefaultBodyLayerStack.
			   (BlinkLayer.
			    (GlazedListsEventLayer. (DataLayer. body-data-provider) event-list)
			    body-data-provider
			    (proxy [IRowIdAccessor][](#^Serializable getRowId [#^Object row] (.get pk-getter row)))
			    column-property-accessor
			    config-registry))

	column-header-data-provider (DefaultColumnHeaderDataProvider. property-names property-name-to-label)
	column-header-data-layer (DefaultColumnHeaderDataLayer. column-header-data-provider)
	column-header-layer (ColumnHeaderLayer. column-header-data-layer body-layer-stack (.getSelectionLayer body-layer-stack))
	column-header-layer-stack (proxy [AbstractLayerTransform] [])]
  
    (.setUnderlyingLayer column-header-layer-stack (SortHeaderLayer. column-header-layer (GlazedListsSortModel. sorted-list column-property-accessor config-registry column-header-data-layer) false))

    ;;--------------------------------------------------------------------------------
    ;; Setting up the row header layer
  
    (let [row-header-data-provider (DefaultRowHeaderDataProvider. body-data-provider)
	  row-header-layer (RowHeaderLayer. (DefaultRowHeaderDataLayer. row-header-data-provider) body-layer-stack (.getSelectionLayer body-layer-stack))
	  row-header-layer-stack (proxy [AbstractLayerTransform] [])]
      (.setUnderlyingLayer row-header-layer-stack row-header-layer)

      ;; define parent...
      (let [nattable (NatTable.
		      parent
		      (GridLayer. 
		       body-layer-stack
		       column-header-layer-stack
		       row-header-layer
		       (CornerLayer.
			(DataLayer.
			 (DefaultCornerDataProvider. column-header-data-provider row-header-data-provider))
			row-header-layer-stack
			column-header-layer-stack))
		      false)]
	(doto
	    nattable
	  (.setConfigRegistry config-registry)
	  (.addConfiguration (DefaultNatTableStyleConfiguration.))
	  (.addConfiguration (SingleClickSortConfiguration.))
    
	  ;;TODO - still to translate
	  ;;http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/SortableGridExample.java?revision=3912&view=markup
	  ;; nattable.addConfiguration(getCustomComparatorConfiguration(glazedListsGridLayer.getColumnHeaderLayerStack().getDataLayer()));
	  (.addConfiguration (DefaultSelectionStyleConfiguration.))
	  (.configure)

	  (.setLayoutData (GridData. (SWT/FILL) (SWT/FILL) true true))
	  (.pack))
	nattable))))

;;--------------------------------------------------------------------------------
