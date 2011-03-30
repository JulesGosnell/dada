(ns
 org.dada.core.SessionManagerImpl
 (:use
  [clojure.contrib logging]
  [org.dada core]
  [org.dada.core utils]
  [org.dada jms])
 (:require
  [org.dada.core SessionImpl RemoteSession])
 (:import
  [clojure.lang Atom]
  [java.util Collection Timer TimerTask]
  [org.dada.core Model RemoteSession Session SessionManager SessionImpl View]
  [org.dada.jms MessageServer ServiceFactory] ;TODO - these two should migrate to core
  )
 (:gen-class
  :implements [org.dada.core.SessionManager]
  :constructors {[String Object org.dada.core.Model] []} ;TODO - should be ServiceFactory
  :methods []
  :init init
  :state state
  :post-init post-init
  )
 )

(defrecord MutableState [sessions ^MessageServer server])

(defrecord ImmutableState [^String name ^Model metamodel ^ServiceFactory service-factory ^Timer timer ^Atom mutable])

;;------------------------------------------------------------------------------

(defn sweep-sessions [mutable]
  (trace "sweeping sessions")
  (let [threshold (- (System/currentTimeMillis) 10000)
	dead-sessions (keys (filter (fn [[^Session session]] (< (.getLastPingTime session) threshold)) (:sessions @mutable)))] ;TODO - hardwired
    (if (not (empty? dead-sessions))
      (do
	(debug "dead sessions detected: " dead-sessions)
	(doseq [^Session session dead-sessions] (.close session))))))

;;------------------------------------------------------------------------------

(defn -init [^String name ^ServiceFactory service-factory ^Model metamodel]
  (let [mutable (atom (MutableState. {} nil))
	^Timer timer (doto (Timer.) (.schedule (proxy [TimerTask][](run [] (sweep-sessions mutable))) 0 10000))] ;TODO - hardwired
    [ ;; super ctor args
     []
     ;; instance state
     (ImmutableState. name metamodel service-factory timer mutable)]))

(defn ^ImmutableState immutable  [^org.dada.core.SessionManagerImpl this]
  (.state this))

(defn -post-init [^org.dada.core.SessionManagerImpl this & _]
  (with-record
   (immutable this)
   [^ServiceFactory service-factory name mutable]
   (let [server (.server service-factory this (.endPoint service-factory name))]
     ;; TODO - should not need jms session here
     (swap! mutable assoc :server server))))

(defn destroy-session [^org.dada.core.SessionManagerImpl this ^org.dada.core.SessionImpl session]
  (with-record
   (immutable this)
   [mutable]
   (let [[_ [queue ^MessageServer server]]
	 (swap2!			;N.B. NOT swap!
	  mutable
	  (fn [state]
	      (let [sessions (:sessions state)
		    details (sessions session)]
		[(assoc state :sessions (dissoc sessions session)) details])))]
     (.close server)
     (.delete queue))))			;TODO - service-factory should look after this

(defn ^Session -createSession [^org.dada.core.SessionManagerImpl this]
  (with-record
   (immutable this)
   [metamodel ^ServiceFactory service-factory mutable]
   (let [close-fn (fn [session] (destroy-session this session))
	 session (SessionImpl. metamodel close-fn service-factory)
	 queue (.endPoint service-factory) ;TODO - should not need jms-session here
	 server (.server service-factory session queue)
	 client (RemoteSession. queue)]
     (swap! mutable (fn [state] (assoc state :sessions (conj (:sessions state) [session [queue server]]))))
     (debug "createSession" client)
     client)))

(defn -close [^org.dada.core.SessionManagerImpl this]
  (debug "close")
  (with-record
   (immutable this)
   [^Timer timer mutable]
   (.cancel timer)
   (with-record
    ^MutableState @mutable
    [sessions ^MessageServer server]
    (doseq [[^Session session] sessions](.close session))
    (.close server))))

(defn ^String -getName [^org.dada.core.SessionManagerImpl this]
  (with-record (immutable this) [name] name))
