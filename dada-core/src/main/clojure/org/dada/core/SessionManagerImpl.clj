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
  :post-init post-init
  :state state
  )
 )

(defrecord ImmutableState [^String name ^Model metamodel ^Timer timer ^Atom sessions ^Atom close-hooks])

;;------------------------------------------------------------------------------

(defn cleave [s f & i]
  (reduce (fn [[y n] e] (if (f e) [(conj y e) n] [y (conj n e)])) i s))

(defn sweep-sessions [sessions time-to-live]
  (trace "sweeping sessions")
  (let [threshold (- (System/currentTimeMillis) time-to-live)
	[live dead] (swap2! sessions cleave (fn [^Session s] (> (.getLastPingTime s) threshold)) #{})]
    (if (not (empty? dead))
      (do
	(info "dead sessions detected: " dead)
	(doseq [^Session session dead] (.close session))))))

;;------------------------------------------------------------------------------

(defn -init [^String name ^Model metamodel]
  (let [session-time-to-live (long (* 5 60 1000)) ;TODO - hardwired
	sessions (atom #{})
	close-hooks (atom [])
	^Timer timer (doto (Timer.)
                           (.schedule
                            (proxy [TimerTask][](run [] (sweep-sessions sessions session-time-to-live)))
                            0
                            session-time-to-live))]
    [ ;; super ctor args
     []
     ;; instance state
     (ImmutableState. name metamodel timer sessions close-hooks)]))

(defn -post-init [^org.dada.core.SessionManagerImpl this & _]
  (info (str this ": open")))

(defn ^ImmutableState immutable  [^org.dada.core.SessionManagerImpl this]
  (.state this))

(defn ^Session -createSession [^org.dada.core.SessionManagerImpl this ^String user-name ^String application-name ^String application-version]
  (with-record
   (immutable this)
   [metamodel sessions]
   (let [session (SessionImpl. metamodel user-name application-name application-version)]
     (.addCloseHook session (fn [session] (swap! sessions disj session)))
     (swap! sessions conj session)
     (info (str this ": create " session))
     session)))

(defn -close [^org.dada.core.SessionManagerImpl this]
  (info (str this ": close"))
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

(defn ^String -toString [^org.dada.core.SessionManagerImpl this]
  (print-object this))
