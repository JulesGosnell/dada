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
    Collection
    Timer
    TimerTask]
   [java.util.concurrent
    Executors
    ExecutorService]
   [javax.jms
    Connection
    ConnectionFactory
    Session
    Topic]
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
    RemotingFactory$Server
    SynchronousClient
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
	^ExecutorService thread-pool (Executors/newFixedThreadPool num-threads)
	
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

	ping-period 5000
	client-id "JULES"
	^Timer ping-timer (doto (Timer.) (.schedule (proxy [TimerTask][] (run [] (.ping peer client-id))) 0 ping-period))
	
	^RemotingFactory remoting-factory (RemotingFactory. session View 10000)

	view-map (atom {})
	;; TOOD - these fns need rethinking to take a properly atomic approach - there are race conditions in these impls
	register-view-fn (fn [^RemoteModel model ^View view]
			     (let [topic (.createTopic session (str "DADA." (.getName model)))
				   server (.createServer2 remoting-factory view topic thread-pool)
				   client (.createSynchronousClient remoting-factory topic true)]
			       (swap! view-map assoc view [topic client server])
			       (.registerView peer model client)
			       ))
	
	deregister-view-fn (fn [^RemoteModel model ^View view]
			     (if-let [[^Topic topic ^Proxy client ^RemotingFactory$Server server] (@view-map view)]
			       (let [data (.deregisterView peer model client)]
				 (.close server)
				 (.close ^SynchronousClient (Proxy/getInvocationHandler client))
				 (swap! view-map dissoc view)
				 data)))

	close-fn (fn []
		   ;; TODO - close or warn about any outstanding Views
		   (.cancel ping-timer)
		   (.close ^SynchronousClient (Proxy/getInvocationHandler peer))
		   (.shutdown thread-pool)
		   (.close session)
		   (.stop connection)
		   (.close connection))		   
	]
    [ ;; super ctor args
     []
     ;; instance state
     [peer register-view-fn deregister-view-fn close-fn]]))

(defn -post-init [^org.dada.core.RemoteSessionManager this & _]
  (SessionManagerHelper/setCurrentSessionManager this))

(defn -close [^org.dada.core.RemoteSessionManager this]
  (let [[_ _ _ close-fn] (.state this)]
    (close-fn)))

(defn -ping [^org.dada.core.RemoteSessionManager this ^String client-id]
  (println "SHOULD NOT BE CALLED - REMOTE SESSION MANAGER - PING" client-id)
  true)

(defn ^Model -find [^org.dada.core.RemoteSessionManager this ^Model model key]
  (let [[^SessionManager peer] (.state this)]
    (.find peer model key)))

(defn ^Data -registerView [^org.dada.core.RemoteSessionManager this ^Model model ^View view]
  (println "RemoteSessionManager: registerView " model view)
  (let [[^SessionManager peer register-view-fn] (.state this)]
    (register-view-fn model view)))

(defn ^Data -deregisterView [^org.dada.core.RemoteSessionManager this ^Model model ^View view]
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

