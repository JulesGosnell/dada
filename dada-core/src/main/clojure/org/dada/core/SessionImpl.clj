(ns
 org.dada.core.SessionImpl
 (:use
  [clojure.contrib logging]
  [org.dada.core utils])
 (:require
  [org.dada.core RemoteSession])
 (:import
  [clojure.lang IFn]
  [java.util Map]
  [org.dada.core Data Model RemoteSession View]
  )
 (:gen-class
  :implements [org.dada.core.Session]
  :constructors {[org.dada.core.Model clojure.lang.IFn] []}
  :init init
  :state state
  )
 )

(defrecord ImmutableState [^Model metamodel ^IFn close-hook])
(defrecord MutableState [^Long lastPing ^Map views])

(defn -init [^Model metamodel ^IFn close-hook]
  (debug "init")
  [ ;; super ctor args
   []
   ;; instance state
   (let [mutable (MutableState. (System/currentTimeMillis) {})
	 immutable (ImmutableState. metamodel close-hook)]
     [immutable (atom mutable)])])

(defn immutable [^org.dada.core.SessionImpl this]
  (first (.state this)))

(defn mutable [^org.dada.core.SessionImpl this]
  (second (.state this)))

(defn -ping [^org.dada.core.SessionImpl this]
  (trace "ping")
  (swap! (mutable this) assoc :lastPing (System/currentTimeMillis))
  true)

(defn -getLastPingTime [^org.dada.core.SessionImpl this]
  (with-record @(mutable this) [lastPing] lastPing))

(defn -close [^org.dada.core.SessionImpl this]
  (let [[immutable mutable] (.state this)]
    (with-record
     immutable
     [close-hook]
     (close-hook this)
     (debug "close")
     (doseq [[^View view models] (:views @mutable)]
	 (doseq [^Model model models] (.deregisterView model view))))))

(defn ^Data -registerView [^org.dada.core.SessionImpl this ^Model model ^View view]
  (debug "registerView")
  (let [[immutable mutable] (.state this)]
    (with-record
     immutable
     [^Model metamodel]
     (let [model-name (.getName model)
	   ^Model model (.find metamodel model-name)]
       (if (nil? model)
	 (throw (IllegalArgumentException. (str "no Model for name: " model-name)))
	 (let [data (.registerView model view)]
	   (swap! mutable (fn [state] (assoc (assoc2m state :views view model) :lastPing (System/currentTimeMillis))))
	   data))))))

(defn ^Data -deregisterView [^org.dada.core.SessionImpl this ^Model model ^View view]
  (debug "deregisterView")
  (let [[immutable mutable] (.state this)]
    (with-record
     immutable
     [^Model metamodel]
     (let [model-name (.getName model)
	   ^Model model (.find metamodel model-name)]
       (if (nil? model)
	 (throw (IllegalArgumentException. (str "no Model for name: " model-name)))
	 (let [data (.deregisterView model view)]
	   (swap! mutable (fn [state] (assoc (dissoc2m state :views view model) :lastPing (System/currentTimeMillis))))
	   data))))))

(defn ^Data -find [^org.dada.core.SessionImpl this ^Model model key]
  (let [[immutable mutable] (.state this)]
    (with-record
     immutable
     [^Model metamodel]
     (swap! mutable assoc :lastPing (System/currentTimeMillis))
     (.find ^Model (.find metamodel (.getName model)) key))))
