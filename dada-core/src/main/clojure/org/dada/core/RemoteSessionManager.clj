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
    SyncMessageClient
    Translator
    ]
   )
  (:gen-class
   :implements [org.dada.core.SessionManager]
   :constructors {[String org.dada.core.remote.Remoter] []}
   :methods []
   :init init
   :state state
   )
  )

(defrecord ImmutableState
  [^Remoter remoter
   ^SyncMessageClient client
   ^SessionManager peer
   ^Atom sessions])

;; proxy for a remote session manager
;; intercept outward bound local Views and replace with proxies

(defproxy-type SessionManagerProxy SessionManager)

(defn -init [^String name ^Remoter remoter]

  (let [send-to (.endPoint remoter name)
	^SyncMessageClient client (.syncClient remoter send-to)
	^SessionManager peer (SessionManagerProxy. (fn [i] (.sendSync client i)) (fn [i] (.sendAsync client i)))]
    [ ;; super ctor args
     []
     ;; instance state
     (ImmutableState. remoter client peer (atom nil))]))

(defn ^ImmutableState immutable [^org.dada.core.RemoteSessionManager this]
  (.state this))

(defn destroy-session [^Atom sessions ^Session session]
  ;; TODO - should split sessions into to-keep and to-dstroy and use swap2!
  (swap! sessions (fn [sessions] (remove (fn [s] (= s session)) sessions)))
  (.close session))

(defn -close [^org.dada.core.RemoteSessionManager this]
  (with-record
    (immutable this)
    [^Remoter remoter ^SyncMessageClient client ^SessionManager peer sessions]
    (doseq [^Session session @sessions] (destroy-session sessions session))
    (.close client)
    (.close remoter)))

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
