(ns org.dada2.web.content
  (import
   [com.vaadin.ui
    UI
    VerticalLayout
    Label
    Table
    TextArea
    ])
 (:use
  [clojure test]
  [clojure.tools logging]
  [org.dada2 core map-model]
  [org.dada2.web table-view])
  )

;;; should be e.g. defdatum / defevent
(defrecord Astronomer [id firstName lastName year])
;; metadata should be produced by def above
(def astronomer-metadata [[:id Long] [:firstName String] [:lastName String] [:year Long]])

(def astronomers
  [(Astronomer. 0 "Nicolaus" "Copernicus" 1473)
   (Astronomer. 1 "Tycho"    "Brahe"      1546)
   (Astronomer. 2 "Giordano" "Bruno"      1548)
   (Astronomer. 3 "Galileo"  "Galilei"    1564)
   (Astronomer. 4 "Johannes" "Kepler"     1571)
   (Astronomer. 5 "Isaac"    "Newton"     1643)])

;;(def astronomers-model (simple-hashmap-model :id))
(def astronomers-model (unversioned-optimistic-map-model "Astronomers" :id))

(defn- animate [^UI ui]
  (.start
   (Thread.
    (fn []
      
      (Thread/sleep 2000)
      (doseq [astronomer astronomers] (on-upsert astronomers-model astronomer))

      (let [session (.getSession ui)]
        (println "starting session: " session)
        (while (not (.isClosing session))
          (do
            (Thread/sleep 250)
            (let [data (vals (data astronomers-model))
                  i (nth data (rand-int (count data)))
                  upsertion (assoc i :year (inc (:year i)))]
              (on-upsert astronomers-model upsertion)
              )))
        (println "stopping session: " session)
        )))))

(defn create-main-layout [^UI ui]
  (let [table (doto (Table. (:name astronomers-model))
                (.setColumnReorderingAllowed true)
                (.setColumnCollapsingAllowed true)
                (.setPageLength (count astronomers))
                (.setSortDisabled false))
        layout (doto (VerticalLayout.)
                 (.addComponent (Label. "DADA Web"))
                 (.addComponent table))]
    (attach astronomers-model (table-view ui table :id astronomer-metadata))
    (animate ui)
    layout
    ))
