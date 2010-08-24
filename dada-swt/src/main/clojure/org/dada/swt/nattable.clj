(ns
 org.dada.swt.nattable
 (use org.dada.core)
 (use org.dada.swt.utils)
 (:import
  [java.util ArrayList]
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

  [org.dada.core Metadata Model]
  ))

;;--------------------------------------------------------------------------------
;; see http://nattable.org/drupal/docs/basicgrid
;; Sorted Table
;; see http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/SortableGridExample.java?revision=3912&view=markup
;; Blinking Table
;; http://nattable.svn.sourceforge.net/viewvc/nattable/trunk/nattable/net.sourceforge.nattable.examples/src/net/sourceforge/nattable/examples/demo/BlinkingGridExample.java?revision=3912&view=markup

(defn make-nattable [#^Model model #^Composite parent]
  (let [#^Metadata metadata (.getMetadata model)
	attributes (.getAttributes metadata)
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
	 (#^Object getDataValue [#^Collection rowObject #^int columnIndex] (nth rowObject columnIndex))
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
			    (proxy [IRowIdAccessor][](#^Serializable getRowId [#^Collection row] (first row)))
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
      (doto
	  (NatTable.
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
	   false)
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
      )))

;;--------------------------------------------------------------------------------

(def table-model (model "Family" (seq-metadata 4)))
(insert-n table-model (ArrayList. [[3 0 "alexandra" 2005][1 0 "jane" 1969][2 0 "anthony" 2001][0 0 "jules" 1967]]))

(if (not *compile-files*)
  (do
    (def parent (Shell. (Display.)))
    (.setLayout parent (GridLayout.))

    (make-nattable table-model parent)

    (.pack parent)
    (.open parent)
    (swt-loop (.getDisplay parent)  parent)
    ))
  