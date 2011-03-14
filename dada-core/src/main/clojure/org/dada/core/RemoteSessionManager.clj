(ns org.dada.core.RemoteSessionManager
  (:use
   [clojure.contrib logging]
   )
  (:require
   [org.dada.core.jms JMSServiceFactory POJOInvoker])
  (:import
   [java.lang.reflect
    Proxy]
   [java.net
    URL
    URLClassLoader]
   [java.util
    Collection]
   [java.util.concurrent
    Executors]
   [java.util.concurrent.locks
    ReentrantLock]
   [javax.jms
    Connection
    ConnectionFactory
    Session
    TemporaryQueue]
   [org.dada.core
    Data
    Metadata
    Model
    RemoteModel
    SessionManager
    SessionManagerHelper
    SessionManagerNameGetter
    ServiceFactory
    View
    ViewNameGetter]
   [org.dada.core.jms
    JMSServiceFactory
    POJOInvoker]
   [org.dada.jms
    DestinationFactory
    QueueFactory
    RemotingFactory
    TopicFactory
    SimpleMethodMapper]
   )
  (:gen-class
   :implements [org.dada.core.SessionManager]
   :constructors {[javax.jms.ConnectionFactory String String Integer] []}
   :methods []
   :init init
   :post-init post-init
   :state state
   )
  )

(defmacro with-lock [lock & body]
  `(let [lock# ~lock]
     (.lock lock#)
     (try ~@body (finally (.unlock lock#)))))

;; proxy for a remote session manager
;; intercept outward bound local Views and replace with proxies

(defn -init [^ConnectionFactory connection-factory ^String classes-url ^String protocol ^Integer num-threads]

  ;; install our class-loader in hierarchy
  ;; (let [current-thread (Thread/currentThread)]
  ;;   (.setContextClassLoader
  ;;    current-thread
  ;;    (URLClassLoader.
  ;;     (into-array [(URL. classes-url)])
  ;;     (.getContextClassLoader current-thread))))

  (let [^Connection connection (doto (.createConnection connection-factory) (.start))
	^Session  session (.createSession connection false (Session/DUPS_OK_ACKNOWLEDGE))
	^Executors thread-pool (Executors/newFixedThreadPool num-threads)
	
	^ServiceFactory session-manager-service-factory (JMSServiceFactory.
							 session
							 SessionManager
							 thread-pool
							 true
							 10000
							 (SessionManagerNameGetter.)
							 (QueueFactory.)
							 (POJOInvoker. (SimpleMethodMapper. SessionManager))
							 protocol)
	^SessionManager peer (.client session-manager-service-factory "SessionManager")

	^RemotingFactory remoting-factory (RemotingFactory. session View 10000)

	lock (ReentrantLock.)
	view-map (atom {})
	register-view-fn (fn [^RemoteModel model ^View view]
			     (let [queue (.createTemporaryQueue session)
				   server (.createServer2 remoting-factory view queue thread-pool)
				   client (.createSynchronousClient remoting-factory queue true)]
			       (swap! view-map assoc view [queue client server])
			       (.registerView peer model client)
			       ))
	
	deregister-view-fn (fn [^RemoteModel model ^View view]
			     (if-let [[^TemporaryQueue queue ^Proxy client server] (@view-map view)]
			       (let [data (.deregisterView peer model client)]
				 (.close server)
				 (.close (Proxy/getInvocationHandler client))
				 (.delete queue)
				 (swap! view-map dissoc view)
				 data)))
	]
    [ ;; super ctor args
     []
     ;; instance state
     [peer register-view-fn deregister-view-fn]]))

(defn -post-init [this & _]
  (SessionManagerHelper/setCurrentSessionManager this))

(defn ^Model -find [this ^Model model key]
  (let [[^SessionManager peer] (.state this)]
    (.find peer model key)))

(defn ^Data -registerView [this ^Model model ^View view]
  (println "RemoteSessionManager: registerView " model view)
  (let [[^SessionManager peer register-view-fn] (.state this)]
    (register-view-fn model view)))

(defn ^Data -deregisterView [this ^Model model ^View view]
  (println "RemoteSessionManager: deregisterView " model view)
  (let [[^SessionManager peer _ deregister-view-fn] (.state this)]
    (deregister-view-fn model view)))

;; implemented - but should not have to

(defn ^Data -getData [^org.dada.core.RemoteSessionManager this ^String model-name]
  ;;warn("RemoteSessionManager: calling getData(), but should not have to")
  (let [[^SessionManager peer] (.state this)]
    (.getData peer model-name)))

;; don't implement - yet

(defn ^String -getName [^org.dada.core.RemoteSessionManager this]
  (throw (UnsupportedOperationException. "NYI")))



(defn ^Model -getModel [^org.dada.core.RemoteSessionManager this ^String name]
  (throw (UnsupportedOperationException. "NYI")))

(defn ^Metadata -getMetadata [^org.dada.core.RemoteSessionManager this ^String name]
  (throw (UnsupportedOperationException. "NYI")))

(defn ^Collection -query[^org.dada.core.RemoteSessionManager this namespace-name query-string]
  (throw (UnsupportedOperationException. "NYI")))

