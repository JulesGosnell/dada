(ns
 org.dada.core.SessionManagerImpl
 (:use
  [clojure.tools logging]
  [org.dada core]
  [org.dada.core utils])
 (:require
  [org.dada.core 
   SessionImpl])
 (:import
  [clojure.lang Atom IFn]
  [java.util Timer TimerTask]
  [org.dada.core Model Session SessionManager SessionImpl]
  )
 (:gen-class
  :implements [org.dada.core.SessionManager]
  :constructors {[String org.dada.core.Model] []}
  :methods [[addCloseHook [clojure.lang.IFn] void]]
  :init init
  :state state
  )
 )

(defrecord ImmutableState [^String name ^Model metamodel ^Timer timer ^Atom sessions ^Atom close-hooks])

;;------------------------------------------------------------------------------

(defn cleave [s f & i]
  (reduce (fn [[y n] e] (if (f e) [(conj y e) n] [y (conj n e)])) i s))

(defn sweep-sessions [sessions]
  (trace "sweeping sessions")
  (let [threshold (- (System/currentTimeMillis) 10000) ;TODO - hardwired
	[live dead] (swap2! sessions cleave (fn [^Session s] (> (.getLastPingTime s) threshold)) #{})]
    (if (not (empty? dead))
      (do
	(debug "dead sessions detected: " dead)
	(doseq [^Session session dead] (.close session))))))

;;------------------------------------------------------------------------------

(defn -init [^String name ^Model metamodel]
  (let [sessions (atom #{})
	close-hooks (atom [])
	^Timer timer (doto (Timer.) (.schedule (proxy [TimerTask][](run [] (sweep-sessions sessions))) 0 10000))] ;TODO - hardwired
    [ ;; super ctor args
     []
     ;; instance state
     (ImmutableState. name metamodel timer sessions close-hooks)]))

(defn ^ImmutableState immutable  [^org.dada.core.SessionManagerImpl this]
  (.state this))

(defn ^Session -createSession [^org.dada.core.SessionManagerImpl this]
  (with-record
   (immutable this)
   [metamodel sessions]
   (let [session (SessionImpl. metamodel)]
     (.addCloseHook session (fn [session] (swap! sessions disj session)))
     (swap! sessions conj session)
     (debug "createSession" session)
     session)))

(defn -close [^org.dada.core.SessionManagerImpl this]
  (debug "close")
  (with-record
   (immutable this)
   [^Timer timer sessions close-hooks]
   (.cancel timer)
   (doseq [^Session session (second (swap2! sessions (fn [sessions] [nil sessions])))] (.close session))
   (doseq [close-hook @close-hooks] (close-hook this))))

(defn ^String -getName [^org.dada.core.SessionManagerImpl this]
  (with-record (immutable this) [name] name))

(defn -addCloseHook [^org.dada.core.SessionManagerImpl this ^IFn close-hook]
  (with-record
   (immutable this)
   [close-hooks]
   (swap! close-hooks conj close-hook)))
