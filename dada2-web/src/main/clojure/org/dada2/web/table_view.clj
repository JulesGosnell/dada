(ns org.dada2.web.table-view
  (import
   [com.vaadin.ui
    UI
    Label
    Table
    ]
   [com.vaadin.data
    Item
    Property
    ]
   )
  (use
   [org.dada2 core])
  )

;; metadata - list of tuples: [key type value-fn]

(defn- upsert [^Table table upsertion pk-fn vals-fn]
  (let [pk (pk-fn upsertion)]
    (if-let [^Item item (.getItem table pk)]
      (doall                            ;TODO - don't use map
       (map 
        (fn [id new-value]
          (let [^Property property (.getItemProperty item id)
                old-value (.getValue property)]
            (if (not (= old-value new-value))
              (.setValue property new-value))
            ))
        (.getItemPropertyIds item)
        (vals-fn upsertion)
        ))
      (.addItem table (into-array Object (vals-fn upsertion)) pk))))

(deftype TableView [^UI ui ^Table table pk-fn vals-fn]
  View
  (on-upsert [this upsertion]
    (.access ui (fn [] (upsert table upsertion pk-fn vals-fn)))
    this)
  (on-upserts [this upsertions] 
    (.access ui (fn [] (doseq [upsertion upsertions] (upsert table upsertion pk-fn vals-fn))))
    this)
  Object
  (^String toString [this] (.toString table))
  )

(defn record-table-view [^UI ui ^Table table pk-fn metadata]
  (doseq [[key type] metadata] (.addContainerProperty table (.toString key) Object nil))
  (TableView. ui table pk-fn vals))
