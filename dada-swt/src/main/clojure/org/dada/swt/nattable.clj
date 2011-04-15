(ns
    org.dada.swt.nattable
  (:use
   [clojure.tools logging]
   [org.dada core]
   )
  (:import
   [java.util ArrayList Collection HashMap List Map]
   [org.eclipse.swt SWT]
   [org.eclipse.swt.layout GridData GridLayout]
   [org.eclipse.swt.widgets Composite Display Shell]
   [ca.odell.glazedlists EventList GlazedLists SortedList]
   [net.sourceforge.nattable NatTable]
   [net.sourceforge.nattable.blink BlinkConfigAttributes BlinkLayer IBlinkingCellResolver]
   [net.sourceforge.nattable.command DisposeResourcesCommand ILayerCommand ILayerCommandHandler]
   [net.sourceforge.nattable.config CellConfigAttributes ConfigRegistry DefaultNatTableStyleConfiguration]
   [net.sourceforge.nattable.data ListDataProvider IColumnPropertyAccessor IRowIdAccessor]
   [net.sourceforge.nattable.extension.glazedlists GlazedListsEventLayer GlazedListsSortModel]
   [net.sourceforge.nattable.grid.data DefaultColumnHeaderDataProvider DefaultCornerDataProvider DefaultRowHeaderDataProvider]
   [net.sourceforge.nattable.grid.layer ColumnHeaderLayer CornerLayer DefaultColumnHeaderDataLayer DefaultRowHeaderDataLayer GridLayer RowHeaderLayer]
   [net.sourceforge.nattable.hideshow ColumnHideShowLayer]
   [net.sourceforge.nattable.layer AbstractLayerTransform DataLayer ILayer ILayerListener]
   [net.sourceforge.nattable.layer.event ILayerEvent]
   [net.sourceforge.nattable.layer.stack DefaultBodyLayerStack]
   [net.sourceforge.nattable.reorder ColumnReorderLayer]
   [net.sourceforge.nattable.selection SelectionLayer]
   [net.sourceforge.nattable.selection.command SelectCellCommand]
   [net.sourceforge.nattable.selection.config DefaultSelectionStyleConfiguration]
   [net.sourceforge.nattable.selection.event CellSelectionEvent]
   [net.sourceforge.nattable.sort SortHeaderLayer]
   [net.sourceforge.nattable.style CellStyleAttributes DisplayMode Style]
   [net.sourceforge.nattable.sort.config SingleClickSortConfiguration]
   [net.sourceforge.nattable.viewport ViewportLayer]
   [org.dada.core Attribute Data Getter Metadata Metadata$VersionComparator Model Update View]
   [org.dada.swt Mutable]
   ))

(defn ^Data register [^Model model ^View view]
  (let [data (.registerView model view)]
    (.update
     view
     (map (fn [datum] (Update. nil datum)) (.getExtant data))
     '()
     (map (fn [datum] (Update. datum nil)) (.getExtinct data)))
    data))

;;--------------------------------------------------------------------------------
;; (defn handle-layer-event [^EventList event-list ^NatTable nattable ^ILayerEvent event ^ILayer layer & [drilldown-fn]]
;;   (if (instance? CellSelectionEvent event)
;;     (try
;;      (if (and (.isWithShiftMask ^CellSelectionEvent event) drilldown-fn (.convertToLocal event))
;;        (let [datum (.getDatum (.get event-list (- (.getRowPosition ^CellSelectionEvent event) 1)))]
;; 	 (if (and datum (instance? Model datum))
;; 	   (drilldown-fn datum))))
;;      (catch Exception e (error e)))))

(defn handle-select-cell [^Metadata metadata ^NatTable nattable ^SelectCellCommand command ^ILayer target-layer ^EventList event-list & [drilldown-fn]]
  (if (.convertToTargetLayer command target-layer)
    (if (and (not (.isShiftMask command)) drilldown-fn)
      (let [row (.getRowPosition command)
	    datum (.getDatum ^Mutable (.get event-list row))]
	(if datum
	  ;; if row contains model...
	  (if (instance? Model datum)
	    (drilldown-fn datum)
	    ;; if cell contains model...
	    (let [datum (.get (.getGetter ^Attribute (nth (.getAttributes metadata) (.getColumnPosition command))) datum)]
	      (if (instance? Model datum)
		(drilldown-fn datum)))))))))

;;--------------------------------------------------------------------------------
;; see http://nattable.org/drupal/docs/basicgrid
;; Sorted Table
;; see http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/SortableGridExample.java?revision=3912&view=markup
;; Blinking Table
;; http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/BlinkingGridExample.java?revision=3912&view=markup

(defn apply-updates [^Getter pk-getter ^Metadata$VersionComparator version-comparator ^EventList event-list ^Map index ^GlazedListsEventLayer event-layer property-names getters ^Collection insertions ^Collection alterations ^Collection deletions]
  ;; since insertions may be carrying an old-value and deletions a new value - do we need to handle them differently ? - consider...
  (dorun
   (map
    (fn [^Update update]
      (if-let [new-value (.getNewValue update)]
	;; insertion/alteration
	(let [pk (.get pk-getter new-value)
	      lock (.writeLock (.getReadWriteLock event-list))]
	  (try
	    (.lock lock)       ;we are holding this for a long time...
	    (if-let [^Mutable old-mutable (.get index pk)]
	      ;; alteration
	      (let [old-value (.getDatum old-mutable)]
		(if (< (.compareTo version-comparator old-value new-value) 0)
		  (do
		    (.setDatum old-mutable new-value)
		    (dorun
		     (map
		      (fn [^Getter getter property-name]
			(let [old (.get getter old-value)
			      new (.get getter new-value)]
			  (if (not (= old new))
			    (.propertyChange event-layer (java.beans.PropertyChangeEvent. old-mutable property-name old new)))))
		      getters
		      property-names)))
		  (debug ["rejecting out-of-order version: " old-value new-value])))
	      ;; insertion
	      ;; TODO - we should check that old-value-version is not > new-value version - if it is, we should remember it as extinct
	      (let [new-mutable (Mutable. new-value)]
		(.put index pk new-mutable)
		(.add event-list new-mutable)
		;; TODO - flashing whole row by flashing all cells is a bit brute force...
		(dorun
		 (map
		  (fn [^Getter getter property-name]
		    (.propertyChange event-layer (java.beans.PropertyChangeEvent. new-mutable property-name nil (.get getter new-value))))
		  getters
		  property-names))))
	    (finally
	     (.unlock lock))))))
    (concat insertions alterations)))
  
  (dorun
   (map
    (fn [^Update update]
      (let [old-value (.getOldValue update)
	    pk (.get pk-getter old-value)
	    lock (.writeLock (.getReadWriteLock event-list))]
	;; TODO: we should remember the latest of old and new version here as extinct
	(try
	  (.lock lock)
	  (if-let [old-mutable (.remove index pk)]
	    (.remove event-list old-mutable))
	  (finally
	   (.unlock lock)))))
    deletions))
  

  )

(defn nattable-make [^Model model ^Composite parent & [drilldown-fn]]
  (let [^Display display (.getDisplay parent)
	^Metadata metadata (.getMetadata model)
	attributes (.getAttributes metadata)
	getters (map (fn [^Attribute attribute] (.getGetter attribute)) attributes)
	pk-getter (.getPrimaryGetter metadata)
	version-comparator (.getVersionComparator metadata)
	property-names (into-array String (map (fn [^Attribute attribute] (.toString (.getKey attribute))) attributes))
	property-name-to-index (apply array-map (interleave property-names (iterate inc 0)))
	property-name-to-label (apply array-map (interleave property-names property-names)) 
	;; store list of immutable data as list of mutable singleton arrays of immutable data
	;; now we can hand off a mutable ref containing immutable data...
	;; this index only to be used inside the event-list's lock
	^Map index (HashMap.)
	event-list (GlazedLists/eventList (.values index))
	sorted-list (SortedList. event-list nil)
      
	column-property-accessor
	(proxy 
	    [IColumnPropertyAccessor]
	    []
	  ;; IColumnAccessor<T>
	  (^Object getDataValue [^Mutable rowObject columnIndex] (.get ^Getter (nth getters columnIndex) (.getDatum rowObject)))
	  (getColumnCount [] (count property-names))
	  ;; public void setDataValue(T rowObject, int columnIndex, Object newValue);
      
	  ;;IColumnPropertyResolver 
	  (^String getColumnProperty [int columnIndex] (nth property-names columnIndex))
	  (getColumnIndex [^String propertyName] (property-name-to-index propertyName))
	  )

	config-registry (ConfigRegistry.)
	body-data-provider (ListDataProvider. sorted-list column-property-accessor)

	data-layer (DataLayer. body-data-provider)
	glazed-lists-event-layer (GlazedListsEventLayer. data-layer event-list)
	blink-layer (BlinkLayer.
		     glazed-lists-event-layer
		     body-data-provider
		     (proxy [IRowIdAccessor][](^Serializable getRowId [^Mutable row] (.get pk-getter (.getDatum row))))
		     column-property-accessor
		     config-registry)
	dummy (do
		(.registerConfigAttribute config-registry 
					  (BlinkConfigAttributes/BLINK_RESOLVER)
					  (proxy [IBlinkingCellResolver][] (resolve [oldValue newValue] (into-array String ["up"])))
					  (DisplayMode/NORMAL))
		(.registerConfigAttribute 
		 config-registry
		 (CellConfigAttributes/CELL_STYLE)
		 (doto (Style.) (.setAttributeValue (CellStyleAttributes/BACKGROUND_COLOR) (.getSystemColor display (SWT/COLOR_GREEN))))
		 (DisplayMode/NORMAL)
		 "up")
		)
	body-layer-stack (DefaultBodyLayerStack. blink-layer)

	column-header-data-provider (DefaultColumnHeaderDataProvider. property-names property-name-to-label)
	column-header-data-layer (DefaultColumnHeaderDataLayer. column-header-data-provider)
	column-header-layer (ColumnHeaderLayer. column-header-data-layer body-layer-stack (.getSelectionLayer body-layer-stack))
	column-header-layer-stack (proxy [AbstractLayerTransform] [])

	view (proxy [View][]
	       (^void update [^Collection insertions ^Collection alterations ^Collection deletions]
		      (.asyncExec
		       display
		       (fn []
			 (apply-updates pk-getter version-comparator event-list index glazed-lists-event-layer property-names getters insertions alterations deletions)))))
	data (register model view)
	]
    ;; initial update
    (apply-updates
     pk-getter
     version-comparator
     event-list
     index
     glazed-lists-event-layer
     property-names
     getters
     (map (fn [insertion] (Update. nil insertion)) (.getExtant data))
     nil
     (map (fn [deletion] (Update. deletion nil)) (.getExtinct data)))

    

    (.setUnderlyingLayer column-header-layer-stack (SortHeaderLayer. column-header-layer (GlazedListsSortModel. sorted-list column-property-accessor config-registry column-header-data-layer) false))

    ;;--------------------------------------------------------------------------------
    ;; Setting up the row header layer
  
    (let [row-header-data-provider (DefaultRowHeaderDataProvider. body-data-provider)
	  row-header-layer (RowHeaderLayer. (DefaultRowHeaderDataLayer. row-header-data-provider) body-layer-stack (.getSelectionLayer body-layer-stack))
	  row-header-layer-stack (proxy [AbstractLayerTransform] [])]
      (.setUnderlyingLayer row-header-layer-stack row-header-layer)

      ;; define parent...
      (let [grid-layer (GridLayer. 
			body-layer-stack
			column-header-layer-stack
			row-header-layer
			(CornerLayer.
			 (DataLayer.
			  (DefaultCornerDataProvider. column-header-data-provider row-header-data-provider))
			 row-header-layer-stack
			 column-header-layer-stack))
	    nattable (NatTable. parent grid-layer false)]

	;; deregister view on disposal
	(.registerCommandHandler 
	 grid-layer
	 (proxy [ILayerCommandHandler] []
		(^Class getCommandClass [] ILayerCommand)
		(^Boolean doCommand [^ILayer targetLayer ^ILayerCommand command]
			  (if (instance? SelectCellCommand command) (handle-select-cell metadata nattable command data-layer sorted-list drilldown-fn))
			  (if (instance? DisposeResourcesCommand command) (.deregisterView model view))
			  false)))

	(doto nattable
	  (.setConfigRegistry config-registry)
	  (.addConfiguration (DefaultNatTableStyleConfiguration.))
	  (.addConfiguration (SingleClickSortConfiguration.))
    
	  ;;TODO - still to translate
	  ;;http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/SortableGridExample.java?revision=3912&view=markup
	  ;; nattable.addConfiguration(getCustomComparatorConfiguration(glazedListsGridLayer.getColumnHeaderLayerStack().getDataLayer()));
	  (.addConfiguration (DefaultSelectionStyleConfiguration.))
	  (.configure)
	  
	  ;;(.addLayerListener (proxy [ILayerListener][](handleLayerEvent [event] (handle-layer-event sorted-list nattable event data-layer drilldown-fn))))

	  (.setLayoutData (GridData. (SWT/FILL) (SWT/FILL) true true))
	  (.pack))))))

;;--------------------------------------------------------------------------------
;; TODO
;; is there really no faster way to remove from an event-list that by identity ?
;; version check
;; when window is closed view should be removed...
;; highlight should be configurable
