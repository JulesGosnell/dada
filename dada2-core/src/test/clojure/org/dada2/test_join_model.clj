(ns org.dada2.test-join-model
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 utils]
     [org.dada2 core]
     [org.dada2 test-core]
     [org.dada2 map-model]
     [org.dada2 split-model]
     ;;[org.dada2 join-model]
     )
    (:import
     [clojure.lang Atom]
     [org.dada2.core ModelView])
    )
;;--------------------------------------------------------------------------------
;; 1x left-hand-side joined to Nx right-hand-sides via common key fns
;; state lives in join model
;; 1x joined model contains the result of calling the join-fn on 1x lhs and Nx rhs for each successful join
;; 1+N unjoined models, containing any unjoined members of lhs and rhs sources
;; as row becomes involved in a join it is deleted from its unjoined model and upserted into the joined model
;;--------------------------------------------------------------------------------
;; impl

;;; calculate all permutations for a given list of sets...
;;; e.g. 
;;; (permute [[]] [[:a1 :a2][:b1 :b2 :b3][:c1]]) ->
;;; ([:a1 :b1 :c1] [:a1 :b2 :c1] [:a1 :b3 :c1] [:a2 :b1 :c1] [:a2 :b2 :c1] [:a2 :b3 :c1])
;;; TODO:
;;;  should use recurse
;;;  should support both inner and outer joins
(defn- permute [state [head & tail]]
  (if (empty? head)
    state
    (permute (mapcat (fn [i] (map (fn [j] (conj i j)) head)) state) tail)))


(deftest test-permute
  (is (= 
       [[:a1 :b1 :c1] [:a1 :b2 :c1] [:a1 :b3 :c1] [:a2 :b1 :c1] [:a2 :b2 :c1] [:a2 :b3 :c1]]
       (permute [[]] [[:a1 :a2][:b1 :b2 :b3][:c1]]))))

;;--------------------------------------------------------------------------------
;; this layer encapsulates access to lhs index

(defn- lhses-get [lhs-indeces i fk]
  "return seq of lhses indexed by i/fk"
  (vals ((nth lhs-indeces i) fk)))

(defn- lhs-assoc [indeces lhs-pk-key lhs-fk-keys lhs]
  "associate a new lhs with existing index"
  (assoc
   indeces
   0
   (let [lhs-pk (lhs-pk-key lhs)]
     (mapv
      (fn [lhs-index lhs-fk-key] (assoc-in lhs-index [(lhs-fk-key lhs) lhs-pk] lhs))
      (first indeces)
      lhs-fk-keys))))

(deftest test-lhs-access
  (let [before [[{}]]
	after  [[{String {3 "xxx"}}]]]
    (is (= after (lhs-assoc before count [type] "xxx")))
    (is (= '("xxx") (lhses-get (first after) 0 String)))
    ))

;;--------------------------------------------------------------------------------
;; this layer encapsulates access to rhs indeces

(defn- rhses-get [rhs-indeces lhs-fk-keys lhs]
  "return a lazy seq of seqs of rhses indexed by their respective fks"
  (map (fn [rhs-index lhs-fk-key] (vals (rhs-index (lhs-fk-key lhs)))) rhs-indeces lhs-fk-keys))

(defn- rhs-assoc [indeces i rhs-pk-key fk rhs]
  "associate a new rhs with existing index"
  (assoc-in indeces [i fk (rhs-pk-key rhs)] rhs))

(deftest test-rhs-access
  (let [before [[] {}]
	after  [[] {String {3 "xxx"}}]]
    (is (= after (rhs-assoc before 1 count String "xxx")))
    (is (= '(("xxx")) (rhses-get (rest after) [first] [String])))))

;;--------------------------------------------------------------------------------
;; this layer encapsulates access to both lhs and rhs at the same time...

(defn- derive-joins [rhs-indeces lhs-fk-keys lhs join-fn]
  (map
   (fn [args] (apply join-fn lhs args))
   (permute [[]] (rhses-get rhs-indeces lhs-fk-keys lhs))))

;;--------------------------------------------------------------------------------

(defn- lhs-upsert [old-indeces lhs-pk-key lhs-fk-keys lhs join-fn joined-model]
  (let [new-indeces (lhs-assoc old-indeces lhs-pk-key lhs-fk-keys lhs)
	joins (derive-joins (rest new-indeces) lhs-fk-keys lhs join-fn)]
    [new-indeces (fn [_] (on-upserts joined-model joins))]))

;;; TODO
(defn- lhs-delete [old-indeces i ks v join-fn])
(defn- lhs-upserts [old-indeces i ks vs join-fn])
(defn- lhs-deletes [old-indeces i ks vs join-fn])

(defn- lhs-view [indeces i lhs-pk-key lhs-fk-keys join-fn joined-model]
  (reify
   View
   ;; singleton changes
   (on-upsert [_ upsertion] ((swap*! indeces lhs-upsert lhs-pk-key lhs-fk-keys upsertion join-fn joined-model) []) nil) ;TODO - pass in views to notifier
   (on-delete [_ deletion]  (swap! indeces lhs-delete lhs-fk-keys deletion join-fn) nil)
   ;; batch changes
   (on-upserts [_ upsertions] (swap! indeces lhs-upserts lhs-fk-keys upsertions join-fn) nil)
   (on-deletes [_ deletions]  (swap! indeces lhs-deletes lhs-fk-keys deletions join-fn) nil)))

;; TODO: return a pair - first is notification fn, second is new-indeces - use swap*! to apply

(defn- rhs-upsert [old-indeces i rhs-pk-key rhs-fk-key lhs-fk-keys rhs join-fn joined-model]
  (let [fk (rhs-fk-key rhs)
	new-indeces (rhs-assoc old-indeces i rhs-pk-key fk rhs)
	lhs-indeces (first old-indeces)
	lhses (lhses-get lhs-indeces (dec i) fk)
	rhs-indeces (rest new-indeces)
	joins (mapcat (fn [lhs] (derive-joins rhs-indeces lhs-fk-keys lhs join-fn)) lhses)
	]
    [new-indeces (fn [_] (on-upserts joined-model joins))]
    ))

;;; TODO
(defn- rhs-delete [old-indeces i ks v join-fn])
(defn- rhs-upserts [old-indeces i ks vs join-fn])
(defn- rhs-deletes [old-indeces i ks vs join-fn])

(defn- rhs-view [indeces i rhs-pk-key rhs-fk-key lhs-fk-keys join-fn joined-model]
  (reify
   View
   ;; singleton changes
   (on-upsert [_ upsertion] ((swap*! indeces rhs-upsert i rhs-pk-key rhs-fk-key lhs-fk-keys upsertion join-fn joined-model) []) nil) ;TODO: pass in views to notifier
   (on-delete [_ deletion]  (swap! indeces rhs-delete i lhs-fk-keys deletion join-fn) nil)
   ;; batch changes
   (on-upserts [_ upsertions] (swap! indeces rhs-upserts i lhs-fk-keys upsertions join-fn) nil)
   (on-deletes [_ deletions]  (swap! indeces rhs-deletes i lhs-fk-keys deletions join-fn) nil)))

;;; attach lhs to all rhses such that any change to an rhs initiates an
;;; attempt to [re]join it to the lhs and vice versa.
(defn- join-views [join-fn [lhs-model lhs-pk-key lhs-fk-keys joined-model] & rhses]
  (let [indeces (atom [(apply vector (repeat (count rhses) {}))])]
    ;; view lhs
    (log2 :info (str " lhs: " lhs-model ", " lhs-fk-keys))
    (attach lhs-model (lhs-view indeces 0 lhs-pk-key lhs-fk-keys join-fn joined-model))
    ;; view rhses
    (doseq [[rhs-model rhs-pk-key rhs-fk-key] rhses]
	(let [i (count @indeces)]   ; figure out offset for this index
	  (log2 :info (str " rhs: " rhs-model ", " i ", " rhs-fk-key))
	  (swap! indeces conj {})	; add index for this rhs-model
	  ;; view rhs model
	  (attach rhs-model (rhs-view indeces i rhs-pk-key rhs-fk-key lhs-fk-keys join-fn joined-model))))
    indeces))

(deftype JoinModel [^String name ^Atom state ^Atom views]
  Model
  (attach [this view] (swap! views conj view) this)
  (detach [this view] (swap! views without view) this)
  (data [_] @state)
  Object
  (^String toString [this] name)
  )

(defn join-model [name joins join-fn]
  (let [state (apply join-views join-fn joins)]
    (->JoinModel name state (atom []))))

;;--------------------------------------------------------------------------------
;; tests

(defrecord A [name ^int version fk-b fk-c]
  Object
  (^String toString [_] (str name)))

(defrecord B [name ^int version b data]
  Object
  (^String toString [_] (str name)))

(defrecord C [name ^int version c data]
  Object
  (^String toString [_] (str name)))

(defrecord ABC [name version b-data c-data]
  Object
  (^String toString [_] (str name)))

;; an example of an aggressive join-fn
;; a "lazy" join fn would define the same interface, but hold references to the A,B and C...
;; a really clever impl might start lazy and become agressive during serialisation..
(defn- ^ABC join-abc [^A a ^B b ^C c]
  (->ABC
   [(:name a) (:name b) (:name c)]
   [ (:version a) (:version b) (:version c)]
   (:data b)
   (:data c)))

(defn- abc-more-recent-than? [[a1 b1 c1][a2 b2 c2]]
  (or (and (> a1 a2) (>= b1 b2) (>= c1 c2))
      (and (> b1 b2) (>= a1 a2) (>= c1 c2))
      (and (> c1 c2) (>= a1 a2) (>= b1 b2))))

(deftest test-join-abc
  (is (= (->ABC [:a1 :b1 :c1] [0 0 0] "b-data" "c-data") 
	 (join-abc (->A :a1 0 :b :c)(->B :b1 0 :b "b-data")(->C :c1 0 :c "c-data")))))

(deftest test-join-model
  (let [as (versioned-optimistic-map-model (str :as) :name :version >)
	bs (versioned-optimistic-map-model (str :bs) :name :version >)
	cs (versioned-optimistic-map-model (str :cs) :name :version >)
	joined-model (versioned-optimistic-map-model (str :joined) :name :version abc-more-recent-than?)
	join (join-model "join-model" [[as :name [:fk-b :fk-c] joined-model][bs :name :b][cs :name :c]] join-abc)
	view (test-view "test")
	a1 (->A :a1 0 :b :c)
	a1v1 (->A :a1 1 :b :c)
	a2 (->A :a2 0 :b :c)
	b1 (->B :b1 0 :b "b1-data")
	b1v1 (->B :b1 1 :b "b1v1-data")
	b2 (->B :b2 0 :b "b2-data")
	c1 (->C :c1 0 :c "c1-data")
	c1v1 (->C :c1 1 :c "c1v1-data")
	c2 (->C :c2 0 :c "c2-data")]
    (is (= [[{}{}]{}{}] (data join)))
    (is (= {} (data joined-model)))

    (attach join view)
    (is (= nil (data view)))

    ;; rhs insertion - no join
    (on-upsert bs b1)
    (is (= [[{}{}]{:b {:b1 b1}} {}] (data join)))
    (is (= {} (data joined-model)))

    ;; rhs insertion - no join
    (on-upsert cs c1)
    (is (= [[{}{}]{:b {:b1 b1}} {:c {:c1 c1}}] (data join)))
    (is (= {} (data joined-model)))

    ;; lhs-insertion - first join
    (on-upsert as a1)
    (is (= [[{:b {:a1 a1}}{:c {:a1 a1}}]{:b {:b1 b1}} {:c {:c1 c1}}] (data join)))
    (is (= {[:a1 :b1 :c1] (->ABC [:a1 :b1 :c1] [0 0 0] "b1-data" "c1-data")} (data joined-model)))

    ;; join - new rhs - b2
    (on-upsert bs b2)
    (is (= [[{:b {:a1 a1}}{:c {:a1 a1}}]{:b {:b1 b1 :b2 b2}} {:c {:c1 c1}}] (data join)))
    (is (= {
	   [:a1 :b1 :c1] (->ABC [:a1 :b1 :c1] [0 0 0] "b1-data" "c1-data")
	   [:a1 :b2 :c1] (->ABC [:a1 :b2 :c1] [0 0 0] "b2-data" "c1-data")}
	   (data joined-model)))

    ;; join - new rhs - c2
    (on-upsert cs c2)
    (is (= [[{:b {:a1 a1}}{:c {:a1 a1}}]{:b {:b1 b1 :b2 b2}} {:c {:c1 c1 :c2 c2}}] (data join)))
    (is (= {
	   [:a1 :b1 :c1] (->ABC [:a1 :b1 :c1] [0 0 0] "b1-data" "c1-data")
	   [:a1 :b2 :c1] (->ABC [:a1 :b2 :c1] [0 0 0] "b2-data" "c1-data")
	   [:a1 :b1 :c2] (->ABC [:a1 :b1 :c2] [0 0 0] "b1-data" "c2-data")
	   [:a1 :b2 :c2] (->ABC [:a1 :b2 :c2] [0 0 0] "b2-data" "c2-data")
	   }
	   (data joined-model)))

    ;; join - new lhs - a2
    (on-upsert as a2)
    (is (= [[{:b {:a1 a1 :a2 a2}}{:c {:a1 a1 :a2 a2}}]{:b {:b1 b1 :b2 b2}} {:c {:c1 c1 :c2 c2}}] (data join)))
    (is (= {
	   [:a1 :b1 :c1] (->ABC [:a1 :b1 :c1] [0 0 0] "b1-data" "c1-data")
	   [:a1 :b2 :c1] (->ABC [:a1 :b2 :c1] [0 0 0] "b2-data" "c1-data")
	   [:a1 :b1 :c2] (->ABC [:a1 :b1 :c2] [0 0 0] "b1-data" "c2-data")
	   [:a1 :b2 :c2] (->ABC [:a1 :b2 :c2] [0 0 0] "b2-data" "c2-data")
	   [:a2 :b1 :c1] (->ABC [:a2 :b1 :c1] [0 0 0] "b1-data" "c1-data")
	   [:a2 :b2 :c1] (->ABC [:a2 :b2 :c1] [0 0 0] "b2-data" "c1-data")
	   [:a2 :b1 :c2] (->ABC [:a2 :b1 :c2] [0 0 0] "b1-data" "c2-data")
	   [:a2 :b2 :c2] (->ABC [:a2 :b2 :c2] [0 0 0] "b2-data" "c2-data")
	   }
	   (data joined-model)))

    ;; lhs update - relevant joins should update
    (on-upsert as a1v1)
    (is (= [[{:b {:a1 a1v1 :a2 a2}}{:c {:a1 a1v1 :a2 a2}}]{:b {:b1 b1 :b2 b2}} {:c {:c1 c1 :c2 c2}}] (data join)))
    (is (= {
	   [:a1 :b1 :c1] (->ABC [:a1 :b1 :c1] [1 0 0] "b1-data" "c1-data")
	   [:a1 :b2 :c1] (->ABC [:a1 :b2 :c1] [1 0 0] "b2-data" "c1-data")
	   [:a1 :b1 :c2] (->ABC [:a1 :b1 :c2] [1 0 0] "b1-data" "c2-data")
	   [:a1 :b2 :c2] (->ABC [:a1 :b2 :c2] [1 0 0] "b2-data" "c2-data")
	   [:a2 :b1 :c1] (->ABC [:a2 :b1 :c1] [0 0 0] "b1-data" "c1-data")
	   [:a2 :b2 :c1] (->ABC [:a2 :b2 :c1] [0 0 0] "b2-data" "c1-data")
	   [:a2 :b1 :c2] (->ABC [:a2 :b1 :c2] [0 0 0] "b1-data" "c2-data")
	   [:a2 :b2 :c2] (->ABC [:a2 :b2 :c2] [0 0 0] "b2-data" "c2-data")
	   }
	   (data joined-model)))

    ;; rhs update - relevant joins should update
    (on-upsert bs b1v1)
    (is (= [[{:b {:a1 a1v1 :a2 a2}}{:c {:a1 a1v1 :a2 a2}}]
	    {:b {:b1 b1v1 :b2 b2}} {:c {:c1 c1 :c2 c2}}] (data join)))
    (is (= {
	   [:a1 :b1 :c1] (->ABC [:a1 :b1 :c1] [1 1 0] "b1v1-data" "c1-data")
	   [:a1 :b2 :c1] (->ABC [:a1 :b2 :c1] [1 0 0] "b2-data" "c1-data")
	   [:a1 :b1 :c2] (->ABC [:a1 :b1 :c2] [1 1 0] "b1v1-data" "c2-data")
	   [:a1 :b2 :c2] (->ABC [:a1 :b2 :c2] [1 0 0] "b2-data" "c2-data")
	   [:a2 :b1 :c1] (->ABC [:a2 :b1 :c1] [0 1 0] "b1v1-data" "c1-data")
	   [:a2 :b2 :c1] (->ABC [:a2 :b2 :c1] [0 0 0] "b2-data" "c1-data")
	   [:a2 :b1 :c2] (->ABC [:a2 :b1 :c2] [0 1 0] "b1v1-data" "c2-data")
	   [:a2 :b2 :c2] (->ABC [:a2 :b2 :c2] [0 0 0] "b2-data" "c2-data")
	   }
	   (data joined-model)))

    ;; rhs update - relevant joins should update
    (on-upsert cs c1v1)
    (is (= [[{:b {:a1 a1v1 :a2 a2}}{:c {:a1 a1v1 :a2 a2}}]
	    {:b {:b1 b1v1 :b2 b2}} {:c {:c1 c1v1 :c2 c2}}] (data join)))
    (is (= {
	   [:a1 :b1 :c1] (->ABC [:a1 :b1 :c1] [1 1 1] "b1v1-data" "c1v1-data")
	   [:a1 :b2 :c1] (->ABC [:a1 :b2 :c1] [1 0 1] "b2-data" "c1v1-data")
	   [:a1 :b1 :c2] (->ABC [:a1 :b1 :c2] [1 1 0] "b1v1-data" "c2-data")
	   [:a1 :b2 :c2] (->ABC [:a1 :b2 :c2] [1 0 0] "b2-data" "c2-data")
	   [:a2 :b1 :c1] (->ABC [:a2 :b1 :c1] [0 1 1] "b1v1-data" "c1v1-data")
	   [:a2 :b2 :c1] (->ABC [:a2 :b2 :c1] [0 0 1] "b2-data" "c1v1-data")
	   [:a2 :b1 :c2] (->ABC [:a2 :b1 :c2] [0 1 0] "b1v1-data" "c2-data")
	   [:a2 :b2 :c2] (->ABC [:a2 :b2 :c2] [0 0 0] "b2-data" "c2-data")
	   }
	   (data joined-model)))

    ;; TODO:
    ;;  amend in/out
    ;;  delete
    ;;  batch operations
    ;;  test listener receives correct events
    ;;  test downstream model receives correct events
    ;;  unjoined models...
    ;;  move prod code out of this file
    ;;  refactor and document
    ))
