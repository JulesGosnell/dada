(ns org.dada.core.RemoteSessionManager
 (:use
  [clojure.contrib logging]
  [org.dada.core utils]
  )
 (:import
  [java.lang.reflect
   Proxy]
  [java.net
   URL
   URLClassLoader]
  [java.util
   Collection
   Timer
   TimerTask]
  [java.util.concurrent
   Executors
   ExecutorService]
  [javax.jms
   Connection
   ConnectionFactory
   Queue
   Topic]
  [clojure.lang
   Atom]
  [org.dada.core
   Data
   Metadata
   Model
   RemoteModel
   Session
   SessionManager
   SessionManagerHelper
   View]
  [org.dada.jms
   RemotingFactory
   RemotingFactory$Server
   SynchronousClient]
  )
 (:gen-class
  :implements [org.dada.core.SessionManager]
  :constructors {[String javax.jms.ConnectionFactory String Integer] []}
  :methods []
  :init init
  :state state
  )
 )

(defrecord ImmutableState
  [^Connection connection
   ^javax.jms.Session jms-session
   ^ExecutorService thread-pool
   ^SessionManager peer
   ^Atom sessions])

;; proxy for a remote session manager
;; intercept outward bound local Views and replace with proxies

(defn -init [^String name ^ConnectionFactory connection-factory ^String classes-url ^Integer num-threads]

  (let [^Connection connection (doto (.createConnection connection-factory) (.start))
	^javax.jms.Session jms-session (.createSession connection false (javax.jms.Session/DUPS_OK_ACKNOWLEDGE))
	^ExecutorService thread-pool (Executors/newFixedThreadPool num-threads)
	^RemotingFactory session-manager-remoting-factory (RemotingFactory. jms-session SessionManager 10000) ;TODO - hardwired
	^Queue session-manager-queue (.createQueue jms-session name)
	^SessionManager peer (.createSynchronousClient session-manager-remoting-factory session-manager-queue true)
	^RemotingFactory view-remoting-factory (RemotingFactory. jms-session View 10000)] ;TODO - hardwired
    [ ;; super ctor args
     []
     ;; instance state
     (ImmutableState. connection jms-session thread-pool peer (atom nil))]))

(defn ^ImmutableState immutable [^org.dada.core.RemoteSessionManager this]
  (.state this))

(defn destroy-session [^Atom sessions ^Session session]
  ;; TODO - should split sessions into to-keep and to-dstroy and use swap2!
  (swap! sessions (fn [sessions] (remove (fn [s] (= s session)) sessions)))
  (.close session))

(defn -close [^org.dada.core.RemoteSessionManager this]
  (with-record
   (immutable this)
   [^Connection connection ^javax.jms.Session jms-session ^ExecutorService thread-pool ^SessionManager peer sessions]
   (doseq [^Session session @sessions] (destroy-session sessions session))
   (.close ^SynchronousClient (Proxy/getInvocationHandler peer))
   (.shutdown thread-pool)		;TODO: should we be responsible for this ?
   (.close jms-session)
   (.stop connection)
   (.close connection)))

(defn ^Session -createSession [^org.dada.core.RemoteSessionManager this]
  (with-record
   (immutable this)
   [^javax.jms.Session jms-session ^ExecutorService thread-pool ^SessionManager peer sessions]
   (let [session (doto (.createSession peer) (.hack jms-session thread-pool))]
     (swap! sessions conj session)
     (SessionManagerHelper/setCurrentSession session) ;; TODO - temporary hack
     ;; TODO : aargh! - we need to wrap session in a proxy that will remove it from our list on closing
     ;; we could reuse this pattern in Local SessionManager and Session ?
     session)))
