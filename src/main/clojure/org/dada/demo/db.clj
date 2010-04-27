
(use 'clojure.contrib.sql)

(def db
     {:classname "org.apache.derby.jdbc.EmbeddedDriver"
     :subprotocol "derby:memory"
     :subname "whales-db"
     :create true})

(defn create-schema
  "create a table to store whales"
  []
  (create-table
   :whales
   [:id :int]
   [:version :int]
   [:time :timestamp "NOT NULL"]
   [:type "varchar(255)" "NOT NULL"]
   [:length :float "NOT NULL"]
   [:weight :float "NOT NULL"]
   ))

(defn insert-whale
  "Insert a whale into the whale table"
  [id version time type length weight]
  (insert-values
   :whales
   [:id :version :time :type :length :weight]
   [id version time type length weight]))

(with-connection
 db
 (transaction (create-schema)))

(def now (java.sql.Timestamp. (.getTime (java.util.Date.))))
(def then (java.sql.Timestamp. (+ 1 (.getTime (java.util.Date.)))))

(with-connection
 db
 (insert-whale 0 0 now "blue" 100 100)
 (insert-whale 1 0 then "blue" 100 100)
 (insert-whale 2 0 now "blue" 100 100)
 (insert-whale 3 0 then "blue" 100 100)
 (insert-whale 4 0 now "grey" 100 100)
 (insert-whale 5 0 then "grey" 100 100)
 (insert-whale 6 0 now "grey" 100 100)
 (insert-whale 7 0 then "grey" 100 100))

(defn ? [q]
  (with-connection
   db
   (with-query-results
    rs
    [q]
    (dorun (map #(println %) rs)))))

(? "select * from whales")
(? "select type, count(*) count from whales group by type")
(? "select type, time, count(type) count from whales group by type, time")
