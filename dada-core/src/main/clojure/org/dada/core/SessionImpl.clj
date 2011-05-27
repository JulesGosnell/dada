(ns
 org.dada.core.SessionImpl
 (:use
  [clojure.tools logging]
  [org.dada.core utils])
 (:import
  [clojure.lang Atom IFn]
  [java.util Map]
  [org.dada.core Data Model View]
  )
 (:gen-class
  :implements [org.dada.core.Session]
  :constructors {[org.dada.core.Model String String String] []}
  :methods [[addCloseHook [clojure.lang.IFn]  void]]
  :init init
  :state state
  )
 )

(defrecord MutableState [^Long lastPing ^Map views close-hooks])
(defrecord ImmutableState [^Model metamodel ^String user-name ^String application-name ^String application-version ^Atom mutable])

(defn -init [^Model metamodel ^String user-name ^String application-name ^String application-version]
  [ ;; super ctor args
   []
   ;; instance state
   (ImmutableState. metamodel user-name application-name application-version
		    (atom (MutableState. (System/currentTimeMillis) {} [])))])

(defn ^ImmutableState immutable [^org.dada.core.SessionImpl this]
   (.state this))

(defn ^Atom mutable [^org.dada.core.SessionImpl this]
  (.mutable (immutable this)))

(defn -ping [^org.dada.core.SessionImpl this]
  (trace "ping")
  (swap! (mutable this) assoc :lastPing (System/currentTimeMillis))
  (* 5 60 1000))			;TODO - should be session-ttl

(defn -getLastPingTime [^org.dada.core.SessionImpl this]
  (with-record ^MutableState @(mutable this) [lastPing] lastPing))

(defn -close [^org.dada.core.SessionImpl this]
  (with-record
    (immutable this)
    [mutable]
    ;; TODO - should all be done as one atomic action...
    (doseq [close-hook (:close-hooks @mutable)] (close-hook this))
    (info (str this ": close"))
    (doseq [[^View view models] (:views @mutable)]
      (doseq [^Model model models] (.detach model view)))))

;; drill into v1 with k1 then k2 and add v4 to the resulting vector, rebuilding structure on way out

(defn assoc2m [^Map v1 k1 k2 v4]
  (let [^Map v2 (or (.get v1 k1) {})
	v3 (or (.get v2 k2) [])]
    (assoc v1 k1 (assoc v2 k2 (conj v3 v4)))))

(defn ^Data -attach [^org.dada.core.SessionImpl this ^Model model ^View view]
  (info (str this ": attach " view " to " model))
  (with-record
   (immutable this)
   [^Model metamodel mutable]
   (let [model-name (.getName model)
	 ^Model model (.find metamodel model-name)]
     (if (nil? model)
       (throw (IllegalArgumentException. (str "no Model for name: " model-name)))
       (let [data (.attach model view)]
	 (swap! mutable (fn [state] (assoc (assoc2m state :views view model) :lastPing (System/currentTimeMillis))))
	 data)))))

;; drill into v1 with k1 then k2 and remove v4 to the resulting vector, rebuilding structure on way out

(defn dissoc2m [^Map v1 k1 k2 v4]
  (let [^Map v2 (or (.get v1 k1) {})
	v3 (or (.get v2 k2) [])]
    (assoc v1 k1 (assoc v2 k2 (remove (fn [v] (= v v4)) v3)))))

(defn ^Data -detach [^org.dada.core.SessionImpl this ^Model model ^View view]
  (info (str this ": detach " view " from " model))
  (with-record
   (immutable this)
   [^Model metamodel mutable]
   (let [model-name (.getName model)
	 ^Model model (.find metamodel model-name)]
     (if (nil? model)
       (throw (IllegalArgumentException. (str "no Model for name: " model-name)))
       (let [data (.detach model view)]
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
   (.getData ^Model (.find metamodel (.getName model)))))

(defn ^Model -query [^org.dada.core.SessionImpl this ^String query]
  (with-record
   (immutable this)
   [^Model metamodel mutable]
   (swap! mutable assoc :lastPing (System/currentTimeMillis))
   (throw (UnsupportedOperationException. "QUERY - NYI"))))

(defn -addCloseHook [^org.dada.core.SessionImpl this ^IFn close-hook]
  (with-record
   (immutable this)
   [mutable]
   (swap! mutable (fn [mutable close-hook] (assoc mutable :close-hooks (conj (:close-hooks mutable) close-hook))) close-hook)))

(defn ^String -toString [^org.dada.core.SessionImpl this]
  (let [{user-name :user-name} (immutable this)]
    (print-object this user-name)))
