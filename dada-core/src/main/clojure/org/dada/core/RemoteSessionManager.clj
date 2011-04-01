(ns org.dada.core.RemoteSessionManager
  (:use
   [clojure.contrib logging]
   [org.dada.core utils]
   [org.dada.core proxy]
   [org.dada.core remote]
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
   [clojure.lang
    Atom]
   [org.dada.core
    Data
    Metadata
    Model
    RemoteModel
    RemoteSession
    Session
    SessionManager
    SessionManagerHelper
    View]
   [org.dada.core.remote
    MessageStrategy
    Remoter
    SerializeTranslator
    SyncMessageClient
    Translator
    ]
   ;; TODO - lose jms references
   [javax.jms
    Connection
    ConnectionFactory
    Queue
    Topic]
   [org.dada.jms
    BytesMessageStrategy
    JMSRemoter]
   )
  (:gen-class
   :implements [org.dada.core.SessionManager]
   :constructors {[String javax.jms.ConnectionFactory Integer] []}
   :methods []
   :init init
   :state state
   )
  )

(defrecord ImmutableState
  [^Connection connection
   ^javax.jms.Session jms-session
   ^ExecutorService thread-pool
   ^Remoter remoter
   ^SyncMessageClient client
   ^SessionManager peer
   ^Atom sessions])

;; proxy for a remote session manager
;; intercept outward bound local Views and replace with proxies

(defproxy-type SessionManagerProxy SessionManager)

(def ^MessageStrategy strategy (BytesMessageStrategy.))
(def ^Translator translator (SerializeTranslator.))

(defn -init [^String name ^ConnectionFactory connection-factory ^Integer num-threads]

  (let [^Connection connection (doto (.createConnection connection-factory) (.start))
	^javax.jms.Session jms-session (.createSession connection false (javax.jms.Session/DUPS_OK_ACKNOWLEDGE))
	^ExecutorService thread-pool (Executors/newFixedThreadPool num-threads)
	^Remoter remoter (JMSRemoter. jms-session thread-pool strategy translator 10000) ;TODO - hardwired
	^Queue send-to (.createQueue jms-session name)
	^SyncMessageClient client (.syncClient remoter send-to)
	^SessionManager peer (SessionManagerProxy. (fn [i] (.sendSync client i)) (fn [i] (.sendAsync client i)))]
    [ ;; super ctor args
     []
     ;; instance state
     (ImmutableState. connection jms-session thread-pool remoter client peer (atom nil))]))

(defn ^ImmutableState immutable [^org.dada.core.RemoteSessionManager this]
  (.state this))

(defn destroy-session [^Atom sessions ^Session session]
  ;; TODO - should split sessions into to-keep and to-dstroy and use swap2!
  (swap! sessions (fn [sessions] (remove (fn [s] (= s session)) sessions)))
  (.close session))

(defn -close [^org.dada.core.RemoteSessionManager this]
  (with-record
   (immutable this)
   [^Connection connection ^javax.jms.Session jms-session ^ExecutorService thread-pool ^SyncMessageClient client ^SessionManager peer sessions]
   (doseq [^Session session @sessions] (destroy-session sessions session))
   (.close client)
   (.shutdown thread-pool)		;TODO: should we be responsible for this ?
   (.close jms-session)
   (.stop connection)
   (.close connection)))

(defn ^Session -createSession [^org.dada.core.RemoteSessionManager this]
  (with-record
   (immutable this)
   [^Remoter remoter ^SessionManager peer sessions]
   (let [session (doto ^RemoteSession (.createSession peer) (.hack remoter))]
     (swap! sessions conj session)
     (SessionManagerHelper/setCurrentSession session) ;; TODO - temporary hack
     ;; TODO : aargh! - we need to wrap session in a proxy that will remove it from our list on closing
     ;; we could reuse this pattern in Local SessionManager and Session ?
     session)))
