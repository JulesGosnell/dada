(ns org.dada2.core
 (:import
  [clojure.lang
   Atom])
 (:use
  [clojure test]
  [clojure.tools logging]
  [org.dada2 utils])
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
  (on-upserts [_ upsertions])
  (on-deletes [_ deletions]))

(defprotocol Model
  "an MVC Model"
  (attach [_ ^View view])
  (detach [_ ^View view])
  (data [_]))

;;--------------------------------------------------------------------------------
;; "a basic DADA2 Model, supporting Views" - add as metadata somehow
;; on-upsert-fn: ([[old-state, old-change] change]) -> [new-state new-change]
(defn without [coll item] (remove (fn [i] (identical? item i)) coll))

(defn log2 [_ message]
  (println "LOG: " message))

;; should state and views be stored in the same atom ?
(deftype ModelView [^String name ^Atom state ^Atom views on-upsert-fn on-delete-fn on-upserts-fn on-deletes-fn]
  Model
  (attach [this view] (swap! views conj view) this)
  (detach [this view] (swap! views without view) this)
  (data [_] @state)
  View
  (on-upsert [this upsertion]
	     (log2 :info (str this " - on-upsert: " upsertion))
	     (if-let [notifier (swap*! state on-upsert-fn upsertion)] (notifier @views))
	     this)
  (on-delete [this deletion]
	     (log2 :info (str this " - on-delete: " deletion))
	     (if-let [notifier (swap*! state on-delete-fn deletion)] (notifier @views))
	     this)
  (on-upserts [this upsertions]
	     (log2 :info (str this " - on-upserts: " upsertions))
	     (if-let [notifier (swap*! state on-upserts-fn upsertions)] (notifier @views))
	      this)
  (on-deletes [this deletions]
	      (log2 :info (str this " - on-deletes: " deletions))
	      (if-let [notifier (swap*! state on-deletes-fn deletions)] (notifier @views))
	      this)
  Object
  (^String toString [this]
	   name)
  )

;;--------------------------------------------------------------------------------

(defn ^ModelView simple-counting-model []
  (->ModelView
   "simple-counting"
   (atom 0)
   (atom [])
   (fn [current upsertion]
       (let [new (inc current)][new (fn [views] (doseq [view views] (on-upsert view new)))]))
   (fn [current deletion]
       (let [new (dec current)][new (fn [views] (doseq [view views] (on-delete view new)))]))
   nil
   nil
   ))

;;--------------------------------------------------------------------------------

(defn ^ModelView simple-sequence-model []
  (->ModelView
   "simple-sequence"
   (atom nil)
   (atom [])
   (fn [current upsertion]
       [(conj current upsertion) (fn [views] (doseq [view views] (on-upsert view upsertion)))])
   nil					;TODO
   nil
   nil
   ))

;;--------------------------------------------------------------------------------

(defn ^ModelView simple-hashset-model []
  (->ModelView
   "simple-hashset"
   (atom #{})
   (atom [])
   (fn [current upsertion]
       (let [next (conj current upsertion)]
	 [next (if (identical? current next) nil (fn [views] (doseq [view views] (on-upsert view upsertion))))]))
   (fn [current deletion]
       (let [next (remove (fn [i] (= i deletion)) current)] ;TODO: is this the best way ?
	 [next (if (identical? current next) nil (fn [views] (doseq [view views] (on-delete view deletion))))]))
   nil
   nil
   ))

;;--------------------------------------------------------------------------------

(defn ^ModelView simple-hashmap-model [key-fn]
  (->ModelView
   "simple-hashmap"
   (atom {})
   (atom [])
   (fn [current upsertion]
       (let [next (assoc current (key-fn upsertion) upsertion)]
	 [next (if (identical? current next) nil (fn [views] (doseq [view views] (on-upsert view upsertion))))]))
   (fn [current deletion]
       (let [next (dissoc current (key-fn deletion))]
	 [next (if (identical? current next) nil (fn [views] (doseq [view views] (on-delete view deletion))))]))
   nil
   nil
   ))

;;--------------------------------------------------------------------------------
