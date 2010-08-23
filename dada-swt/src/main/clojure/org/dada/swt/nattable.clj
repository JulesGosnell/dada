(ns
 org.dada.swt.nattable
 (:import
  [java.util ArrayList]
  [org.eclipse.swt SWT]
  [org.eclipse.swt.layout GridData GridLayout]
  [org.eclipse.swt.widgets Display Shell]
  [net.sourceforge.nattable NatTable]
  [net.sourceforge.nattable.data ListDataProvider IColumnPropertyAccessor]
  [net.sourceforge.nattable.grid.data DefaultColumnHeaderDataProvider DefaultCornerDataProvider DefaultRowHeaderDataProvider]
  [net.sourceforge.nattable.grid.layer ColumnHeaderLayer CornerLayer GridLayer RowHeaderLayer]
  [net.sourceforge.nattable.layer AbstractLayerTransform DataLayer]
  [net.sourceforge.nattable.reorder ColumnReorderLayer]
  [net.sourceforge.nattable.hideshow ColumnHideShowLayer]
  [net.sourceforge.nattable.selection SelectionLayer]
  [net.sourceforge.nattable.viewport ViewportLayer]
  ))


(def parent (Shell. (Display.)))
(.setLayout parent (GridLayout.))

;;--------------------------------------------------------------------------------

;; see http://nattable.org/drupal/docs/basicgrid

;;--------------------------------------------------------------------------------
;; Plugging data

(def data (ArrayList. [[0 "jules" 1967][1 "jane" 1969][2 "anthony" 2001][3 "alexandra" 2005]]))
(def property-names (into-array String ["id" "name" "birthDate"]))

;;--------------------------------------------------------------------------------

(def property-name-to-index (apply array-map (interleave property-names (iterate inc 0))))
(def property-name-to-label (apply array-map (interleave property-names property-names)))

(def property-accessor
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

(def data-provider (ListDataProvider. data property-accessor))

;;--------------------------------------------------------------------------------
;; Setting up the body region

(def body-data-layer (DataLayer. data-provider))

(def column-reorder-layer (ColumnReorderLayer. body-data-layer))

(def column-hide-show-layer (ColumnHideShowLayer. column-reorder-layer))

(def selection-layer (SelectionLayer. column-hide-show-layer))

(def viewport-layer (ViewportLayer. selection-layer))

(def body-layer-stack (proxy [AbstractLayerTransform] []))
(.setUnderlyingLayer body-layer-stack viewport-layer)

;;--------------------------------------------------------------------------------
;; Setting up the column header region

(def column-header-data-provider (DefaultColumnHeaderDataProvider. property-names property-name-to-label))
(def column-header-layer-stack (proxy [AbstractLayerTransform] []))
(def column-header-data-layer (DataLayer. column-header-data-provider))
(def column-header-layer (ColumnHeaderLayer. column-header-data-layer body-layer-stack selection-layer))
(.setUnderlyingLayer column-header-layer-stack column-header-layer)

;;--------------------------------------------------------------------------------
;; Setting up the row header layer

(def row-header-data-provider (DefaultRowHeaderDataProvider. data-provider))
(def row-header-layer-stack (proxy [AbstractLayerTransform] []))
(def row-header-data-layer (DataLayer. row-header-data-provider 50 20))
(def row-header-layer (RowHeaderLayer. row-header-data-layer body-layer-stack selection-layer))
(.setUnderlyingLayer row-header-layer-stack row-header-layer)

;;--------------------------------------------------------------------------------
;; Setting up the corner layer


(def corner-data-provider (DefaultCornerDataProvider. column-header-data-provider row-header-data-provider))
(def corner-layer (CornerLayer. (DataLayer. corner-data-provider) row-header-layer column-header-layer))

;;--------------------------------------------------------------------------------
;; Drum roll ...

(def grid-layer (GridLayer. body-layer-stack column-header-layer-stack row-header-layer-stack corner-layer))
;; define parent...
(def nat-table (NatTable. parent grid-layer))

(.setLayoutData nat-table (GridData. (SWT/FILL) (SWT/FILL) true true))
(.pack nat-table)
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
