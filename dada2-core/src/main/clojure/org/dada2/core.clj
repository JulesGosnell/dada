(ns org.dada2.core
 (:import
  [clojure.lang
   Atom])
 (:use
  [clojure test]
  [clojure.tools logging])
 )

(set! *warn-on-reflection* true)

;; dada - a new approach
;; - sacrifice speed for simplicity
;; - leverage as much as possible of idiomatic clojure and its ecosystem
;; - overtake dada1 in terms of fn-ality

;;--------------------------------------------------------------------------------

(defprotocol View
  "an MVC View"
  ;; singleton changes
  (on-upsert [_ upsertion])
  (on-delete [_ deletion])
  ;; batch changes
  (on-upserts [_ upsertion])
  (on-deletes [_ deletion]))

(defprotocol Model
  "an MVC Model"
  (attach [_ ^View view])
  (detach [_ ^View view])
  (data [_]))

;;--------------------------------------------------------------------------------
;; "a basic DADA2 Model, supporting Views" - add as metadata somehow
;; on-upsert-fn: ([[old-state, old-change] change]) -> [new-state new-change]
(deftype ModelView [^Atom state on-upsert-fn on-delete-fn on-upserts-fn on-deletes-fn]
  Model
  (attach [this view] (add-watch state view (fn [view state old [new delta]] (if delta (delta view)))) this)
  (detach [this view] (remove-watch state view) this)
  (data [_] (first @state))
  View
  (on-upsert [this upsertion]
	     (swap! state (fn [[current] upsertion] (on-upsert-fn current upsertion)) upsertion) this)
  (on-delete [this deletion]
	     (swap! state (fn [[current] deletion] (on-delete-fn current deletion)) deletion) this)
  (on-upserts [this upsertions]
	      (swap! state (fn [[current] upsertions] (on-upserts-fn current upsertions)) upsertions) this)
  (on-deletes [this deletions]
	      (swap! state (fn [[current] deletions] (on-deletes-fn current deletions)) deletions) this)
  )

;;--------------------------------------------------------------------------------

(defn ^ModelView simple-counting-model []
  (->ModelView
   (atom [0])
   (fn [current upsertion] (let [new (inc current)][new (fn [view] (on-upsert view new))]))
   (fn [current deletion] (let [new (dec current)][new (fn [view] (on-delete view new))]))
   nil
   nil
   ))

;;--------------------------------------------------------------------------------

(defn ^ModelView simple-sequence-model []
  (->ModelView
   (atom [])
   (fn [current upsertion] [(conj current upsertion) (fn [view] (on-upsert view  upsertion))])
   nil					;TODO
   nil
   nil
   ))

;;--------------------------------------------------------------------------------

(defn ^ModelView simple-hashset-model []
  (->ModelView
   (atom [#{}])
   (fn [current upsertion]
       (let [next (conj current upsertion)]
	 [next (if (identical? current next) nil (fn [view] (on-upsert view upsertion)))]))
   (fn [current deletion]
       (let [next (remove (fn [i] (= i deletion)) current)] ;TODO: is this the best way ?
	 [next (if (identical? current next) nil (fn [view] (on-delete view deletion)))]))
   nil
   nil
   ))

;;--------------------------------------------------------------------------------

(defn ^ModelView simple-hashmap-model [key-fn]
  (->ModelView
   (atom [{}])
   (fn [current upsertion]
       (let [next (assoc current (key-fn upsertion) upsertion)]
	 [next (if (identical? current next) nil (fn [view] (on-upsert view upsertion)))]))
   (fn [current deletion]
       (let [next (dissoc current (key-fn deletion))]
	 [next (if (identical? current next) nil (fn [view] (on-delete view deletion)))]))
   nil
   nil
   ))

;;--------------------------------------------------------------------------------
