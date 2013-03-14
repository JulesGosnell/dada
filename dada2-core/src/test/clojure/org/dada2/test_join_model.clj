(ns org.dada2.test-join-model
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 core]
     [org.dada2 test-core]
     [org.dada2 map-model]
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

(defn- rhs-upsert [old-indeces i k v]
  (let [old-index (nth old-indeces i)
	key (k v)
	old-value (or (old-index key) [])
	new-value (conj old-value v)
	new-index (assoc old-index key new-value)
	new-indeces (assoc old-indeces i new-index)]
    new-indeces))

;;; TODO
(defn- rhs-delete [old-indeces i k v])
(defn- rhs-upserts [old-indeces i k vs])
(defn- rhs-deletes [old-indeces i k vs])

;;; attach lhs to all rhses such that any change to an rhs initiates an
;;; attempt to [re]join it to the lhs and vice versa.
(defn- join-views [[lhs-model lhs-key] & rhses]
  (let [indeces (atom [(apply vector (repeat (count rhses) {}))])]
    ;; view lhs
    (log2 :info (str " lhs: " lhs-model ", " lhs-key))
    ;; view rhses
    (doseq [[model key] rhses]
	(log2 :info (str " rhs: " model ", " key))
      (let [i (count @indeces)]	    ; figure out offset for this index
	(swap! indeces conj {})	    ; add index for this model
	;; view rhs model
	(attach 
	 model
	 (reify
	  View
	  ;; singleton changes
	  (on-upsert [_ upsertion] (swap! indeces rhs-upsert i key upsertion) nil)
	  (on-delete [_ deletion]  (swap! indeces rhs-delete i key deletion) nil)
	  ;; batch changes
	  (on-upserts [_ upsertions] (swap! indeces rhs-upserts i key upsertions) nil)
	  (on-deletes [_ deletions]  (swap! indeces rhs-deletes i key deletions) nil)))))
    (println "INDECES: " indeces)
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
  (let [state (apply join-views joins)]
    (->JoinModel name state (atom []))))

;;--------------------------------------------------------------------------------
;; tests

(defrecord D [name ^int version e]
  Object
  (^String toString [_] (str name)))

(defrecord B [name ^int version c]
  Object
  (^String toString [_] (str name)))

(defrecord A [name ^int version ^B b ^D d]
  Object
  (^String toString [_] (str name)))

(defrecord ACE [a ^int version c e]
  Object
  (^String toString [_] (str a)))

;; an example of an aggressive join-fn
;; a "lazy" join fn would define the same interface, but hold references to the A,B and C...
;; a really clever impl might start lazy and become agressive during serialisation..
(defn- ^ACE join-ace [^A a ^B b ^D d]
  (->ACE (:name a) (+ (:version a) (:version b) (:version d)) (:c b) (:e d)))

(deftest test-join-ace
  (is (= (->ACE :a 0 :c :e) 
	 (join-ace (->A :a 0 :b :d)(->B :b 0 :c)(->D :d 0 :e)))))

(deftest test-join-model
  (let [as (versioned-optimistic-map-model (str :as) :name :version >)
	bs (versioned-optimistic-map-model (str :bs) :name :version >)
	ds (versioned-optimistic-map-model (str :ds) :name :version >)
	join (join-model "join-model" [[as :b][bs :name][ds :name]] join-ace)
	view (test-view "test")
	a (->A :a 0 :b :d)
	b (->B :b 0 :c)
	d (->D :d 0 :e)]
    (is (= [[{}{}]{}{}] (data join)))

    (attach join view)
    (is (= nil (data view)))

    (on-upsert bs b)
    (is (= [[{}{}]{:b [b]} {}] (data join)))

    (on-upsert ds d)
    (is (= [[{}{}]{:b [b]} {:d [d]}] (data join)))

    ;; (on-delete join james)
    ;; (on-upserts join [john steve])
    ;; (on-deletes join [john steve])

    ))
