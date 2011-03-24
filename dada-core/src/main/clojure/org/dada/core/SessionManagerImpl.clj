(ns
 org.dada.core.SessionManagerImpl
 (:use
  [clojure.contrib logging]
  [org.dada.core utils])
 (:require
  [org.dada.core SessionImpl RemoteSession])
 (:import
  [clojure.lang Atom]
  [java.util Collection Timer TimerTask]
  [java.util.concurrent Executors ExecutorService]
  [javax.jms TemporaryQueue Queue]
  [org.dada.core Data Metadata Model Session SessionManager SessionImpl RemoteSession View]
  [org.dada.jms RemotingFactory RemotingFactory$Server]
  )
 (:gen-class
  :implements [org.dada.core.SessionManager]
  :constructors {[String javax.jms.ConnectionFactory org.dada.core.Model] []}
  :methods []
  :init init
  :state state
  :post-init post-init
  )
 )

(defrecord MutableState [sessions ^RemotingFactory$Server server])

(defrecord ImmutableState [^String name ^Model metamodel ^RemotingFactory session-manager-remoting-factory ^javax.jms.Session jms-session ^ExecutorService thread-pool ^RemotingFactory session-remoting-factory ^Timer session-timer ^Atom mutable])

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

(defn -init [^String name ^javax.jms.ConnectionFactory connection-factory ^Model metamodel]
  (let [^javax.jms.Connection connection (doto (.createConnection connection-factory) (.start))
	^javax.jms.Session jms-session (.createSession connection false (javax.jms.Session/DUPS_OK_ACKNOWLEDGE))
	num-threads 16			;TODO - hardwired
	^ExecutorService thread-pool (Executors/newFixedThreadPool num-threads)
	^RemotingFactory session-manager-remoting-factory (RemotingFactory. jms-session SessionManager 10000) ;TODO - hardwired
	^RemotingFactory session-remoting-factory (RemotingFactory. jms-session Session 10000) ;TODO - hardwired
	mutable (atom (MutableState. {} nil))
	^Timer session-timer (doto (Timer.) (.schedule (proxy [TimerTask][](run [] (sweep-sessions mutable))) 0 10000)) ;TODO - hardwired
	]
    [ ;; super ctor args
     []
     ;; instance state
     (ImmutableState. name metamodel session-manager-remoting-factory jms-session thread-pool session-remoting-factory session-timer mutable)]))

(defn ^ImmutableState immutable  [^org.dada.core.SessionManagerImpl this]
  (.state this))

(defn -post-init [^org.dada.core.SessionManagerImpl this & _]
  (with-record
   (immutable this)
   [^RemotingFactory session-manager-remoting-factory ^javax.jms.Session jms-session name thread-pool mutable]
   (let [server (.createServer2 session-manager-remoting-factory this (.createQueue jms-session name) thread-pool)]
     (swap! mutable assoc :server server))))

(defn destroy-session [^org.dada.core.SessionManagerImpl this ^org.dada.core.SessionImpl session]
  (with-record
   (immutable this)
   [mutable]
   (let [[_ [^TemporaryQueue queue ^RemotingFactory$Server server]]
	 (swap2!			;N.B. NOT swap!
	  mutable
	  (fn [state]
	      (let [sessions (:sessions state)
		    details (sessions session)]
		[(assoc state :sessions (dissoc sessions session)) details])))]
     (.close server)
     (.delete queue))))

(defn ^Session -createSession [^org.dada.core.SessionManagerImpl this]
  (with-record
   (immutable this)
   [metamodel ^javax.jms.Session jms-session ^RemotingFactory session-remoting-factory thread-pool mutable]
   (let [close-fn (fn [session] (destroy-session this session))
	 session (SessionImpl. metamodel close-fn)
	 queue (.createTemporaryQueue jms-session)
	 server (.createServer2 session-remoting-factory session queue thread-pool)
	 client (RemoteSession. queue true)]
     (swap! mutable (fn [state] (assoc state :sessions (conj (:sessions state) [session [queue server]]))))
     (debug "createSession" client)
     client)))

(defn -close [^org.dada.core.SessionManagerImpl this]
  (debug "close")
  (with-record
   (immutable this)
   [^Timer session-timer mutable]
   (.cancel session-timer)
   (with-record
    ^MutableState @mutable
    [sessions ^RemotingFactory$Server server]
    (doseq [[^Session session] sessions](.close session))
    (.close server))))

(defn ^String -getName [^org.dada.core.SessionManagerImpl this]
  (with-record (immutable this) [name] name))
