(ns
 org.dada.swt.nattable
 (:import
  [java.util ArrayList]
  [org.eclipse.swt SWT]
  [org.eclipse.swt.layout GridData GridLayout]
  [org.eclipse.swt.widgets Display Shell]
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
  ))


(def parent (Shell. (Display.)))
(.setLayout parent (GridLayout.))

;;--------------------------------------------------------------------------------

;; Basic Table
;; see http://nattable.org/drupal/docs/basicgrid
;; Sorted Table
;; see http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/SortableGridExample.java?revision=3912&view=markup
;; Blinking Table
;; http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/BlinkingGridExample.java?revision=3912&view=markup

;;--------------------------------------------------------------------------------
;; Plugging data

(def data (ArrayList. [[3 "alexandra" 2005][1 "jane" 1969][2 "anthony" 2001][0 "jules" 1967]]))

(def event-list (GlazedLists/eventList data))
(def sorted-list (SortedList. event-list nil))
(def property-names (into-array String ["id" "name" "birthDate"]))

;;--------------------------------------------------------------------------------

(def property-name-to-index (apply array-map (interleave property-names (iterate inc 0))))
(def property-name-to-label (apply array-map (interleave property-names property-names)))

(def column-property-accessor
     (proxy 
      [IColumnPropertyAccessor]
      []
      ;; IColumnAccessor<T>
      (#^Object getDataValue [#^Collection rowObject #^int columnIndex] (nth rowObject columnIndex))
      (#^int getColumnCount [] (count property-names))
      ;; public void setDataValue(T rowObject, int columnIndex, Object newValue);
      
      ;;IColumnPropertyResolver 
	
      (#^String getColumnProperty [int columnIndex] (nth property-names columnIndex))
      (#^int getColumnIndex [#^String propertyName] (property-name-to-index "b"))
      ))

(def body-data-provider (ListDataProvider. sorted-list column-property-accessor))

;;--------------------------------------------------------------------------------
;; Setting up the body region

(def config-registry (ConfigRegistry.))

(def body-data-layer (DataLayer. body-data-provider))

(def glazed-lists-event-layer (GlazedListsEventLayer. body-data-layer event-list))

(def blink-layer
     (BlinkLayer.
      glazed-lists-event-layer
      body-data-provider
      (proxy [IRowIdAccessor][](#^Serializable getRowId [#^Collection row] (first row)))
      column-property-accessor
      config-registry))

(def body-layer-stack (DefaultBodyLayerStack. blink-layer))

;;--------------------------------------------------------------------------------
;; Setting up the column header region

(def column-header-data-provider (DefaultColumnHeaderDataProvider. property-names property-name-to-label))
(def column-header-data-layer (DefaultColumnHeaderDataLayer. column-header-data-provider))
(def column-header-layer (ColumnHeaderLayer. column-header-data-layer body-layer-stack (.getSelectionLayer body-layer-stack)))
(def sort-header-layer (SortHeaderLayer. column-header-layer (GlazedListsSortModel. sorted-list column-property-accessor config-registry column-header-data-layer) false))
(def column-header-layer-stack (proxy [AbstractLayerTransform] []))
(.setUnderlyingLayer column-header-layer-stack sort-header-layer)

;;--------------------------------------------------------------------------------
;; Setting up the row header layer

(def row-header-data-provider (DefaultRowHeaderDataProvider. body-data-provider))
(def row-header-data-layer (DefaultRowHeaderDataLayer. row-header-data-provider))
(def row-header-layer (RowHeaderLayer. row-header-data-layer body-layer-stack (.getSelectionLayer body-layer-stack)))
(def row-header-layer-stack (proxy [AbstractLayerTransform] []))
(.setUnderlyingLayer row-header-layer-stack row-header-layer)

;;--------------------------------------------------------------------------------
;; Setting up the corner layer

(def corner-data-provider (DefaultCornerDataProvider. column-header-data-provider row-header-data-provider))
(def corner-data-layer (DataLayer. corner-data-provider))
(def corner-layer (CornerLayer. corner-data-layer row-header-layer column-header-layer-stack))

;;--------------------------------------------------------------------------------
;; Drum roll ...

(def grid-layer (GridLayer. body-layer-stack column-header-layer-stack row-header-layer corner-layer))
;; define parent...
(def nattable (NatTable. parent grid-layer, false))
(.setConfigRegistry nattable config-registry)
(.addConfiguration nattable (DefaultNatTableStyleConfiguration.))
(.addConfiguration nattable (SingleClickSortConfiguration.))

;;TODO - still to translate
;;http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/SortableGridExample.java?revision=3912&view=markup
;; nattable.addConfiguration(getCustomComparatorConfiguration(glazedListsGridLayer.getColumnHeaderLayerStack().getDataLayer()));
(.addConfiguration nattable (DefaultSelectionStyleConfiguration.))
(.configure nattable)

(.setLayoutData nattable (GridData. (SWT/FILL) (SWT/FILL) true true))
(.pack nattable)
(.pack parent)
(.open parent)

(defn swt-loop [#^Display display #^Shell shell]
  (loop []
    (if (.isDisposed shell)
      (.dispose display)
      (do
	(if (not (.readAndDispatch display))
	  (.sleep display))
	(recur)))))

(swt-loop (.getDisplay parent)  parent)
