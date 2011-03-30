(ns
 org.dada.core.SessionImpl
 (:use
  [clojure.contrib logging]
  [org.dada.core utils])
 (:require
  [org.dada.core RemoteView])
 (:import
  [clojure.lang Atom IFn]
  [java.util Map]
  [org.dada.core Data Model RemoteView View]
  [org.dada.jms ServiceFactory]
  )
 (:gen-class
  :implements [org.dada.core.Session]
  :constructors {[org.dada.core.Model clojure.lang.IFn org.dada.jms.ServiceFactory] []}
  :init init
  :state state
  )
 )

(defrecord MutableState [^Long lastPing ^Map views])
(defrecord ImmutableState [^Model metamodel ^IFn close-hook ^ServiceFactory service-factory ^Atom mutable])

(defn -init [^Model metamodel ^IFn close-hook ^ServiceFactory service-factory]
  (debug "init")
  [ ;; super ctor args
   []
   ;; instance state
   (ImmutableState. metamodel close-hook service-factory (atom (MutableState. (System/currentTimeMillis) {})))])

(defn ^ImmutableState immutable [^org.dada.core.SessionImpl this]
   (.state this))

(defn ^Atom mutable [^org.dada.core.SessionImpl this]
  (.mutable (immutable this)))

(defn -ping [^org.dada.core.SessionImpl this]
  (trace "ping")
  (swap! (mutable this) assoc :lastPing (System/currentTimeMillis))
  true)

(defn -getLastPingTime [^org.dada.core.SessionImpl this]
  (with-record ^MutableState @(mutable this) [lastPing] lastPing))

(defn -close [^org.dada.core.SessionImpl this]
  (with-record
   (immutable this)
   [close-hook mutable]
   (close-hook this)
   (debug "close")
   (doseq [[^View view models] (:views @mutable)]
       (doseq [^Model model models] (.deregisterView model view)))))

(defn ^Data -registerView [^org.dada.core.SessionImpl this ^Model model ^View view]
  (debug "registerView")
  (with-record
   (immutable this)
   [^Model metamodel mutable ^ServiceFactory service-factory]
   ;; TODO - temporary hack
   (if (instance? RemoteView view) (.hack view service-factory))
   (let [model-name (.getName model)
	 ^Model model (.find metamodel model-name)]
     (if (nil? model)
       (throw (IllegalArgumentException. (str "no Model for name: " model-name)))
       (let [data (.registerView model view)]
	 (swap! mutable (fn [state] (assoc (assoc2m state :views view model) :lastPing (System/currentTimeMillis))))
	 data)))))

(defn ^Data -deregisterView [^org.dada.core.SessionImpl this ^Model model ^View view]
  (debug "deregisterView")
  (with-record
   (immutable this)
   [^Model metamodel mutable]
   (let [model-name (.getName model)
	 ^Model model (.find metamodel model-name)]
     (if (nil? model)
       (throw (IllegalArgumentException. (str "no Model for name: " model-name)))
       (let [data (.deregisterView model view)]
	 (swap! mutable (fn [state] (assoc (dissoc2m state :views view model) :lastPing (System/currentTimeMillis))))
	 data)))))

(defn ^Model -find [^org.dada.core.SessionImpl this ^Model model key]
  (with-record
   (immutable this)
   [^Model metamodel mutable]
   (swap! mutable assoc :lastPing (System/currentTimeMillis))
   (.find ^Model (.find metamodel (.getName model)) key)))

(defn ^Data -getData [^org.dada.core.SessionImpl this ^Model model]
  (with-record
   (immutable this)
   [^Model metamodel mutable]
   (swap! mutable assoc :lastPing (System/currentTimeMillis))
   (.getData (.find metamodel (.getName model)))))

(defn ^Model -query [^org.dada.core.SessionImpl this ^String query]
  (with-record
   (immutable this)
   [^Model metamodel mutable]
   (swap! mutable assoc :lastPing (System/currentTimeMillis))
   (throw (UnsupportedOperationException. "QUERY - NYI"))))
