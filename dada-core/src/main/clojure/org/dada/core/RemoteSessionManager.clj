(ns org.dada.core.RemoteSessionManager
  (:use
   [clojure.contrib logging]
   )
  (:require
   [org.dada.core.jms JMSServiceFactory POJOInvoker])
  (:import
   [java.net
    URL
    URLClassLoader]
   [java.util
    Collection]
   [java.util.concurrent
    Executors]
   [javax.jms
    Connection
    ConnectionFactory
    Session]
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
	^Executor thread-pool (Executors/newFixedThreadPool num-threads)
	
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
	^ServiceFactory view-service-factory (JMSServiceFactory.
					      session
					      View
					      thread-pool
					      true
					      10000
					      (ViewNameGetter.)
					      (QueueFactory.)
					      (POJOInvoker. (SimpleMethodMapper. View))
					      protocol)
	^SessionManager peer (.client session-manager-service-factory "SessionManager")

	;; TODO: should we remember views that we have decoupled ?
	;; yes - and when they deregister, we should tidy them up
	;; somehow...
	remote-view (fn [^RemoteModel model ^View view]
			;; TODO:
			(println "ENDPOINT: " (.getEndPoint model))
			(.decouple view-service-factory view))
	]
    [ ;; super ctor args
     []
     ;; instance state
     [peer remote-view]]))

(defn -post-init [this & _]
  (SessionManagerHelper/setCurrentSessionManager this))

(defn ^Model -find [this ^Model model key]
  (let [[^SessionManager peer] (.state this)]
    (.find peer model key)))

(defn ^Data -registerView [this ^Model model ^View view]
  (println "RemoteSessionManager: registerView " model view)
  (let [[^SessionManager peer remote-view] (.state this)]
    (.registerView peer model (remote-view model view))))

(defn ^Data -deregisterView [this ^Model model ^View view]
  (println "RemoteSessionManager: deregisterView " model view)
  (let [[^SessionManager peer remote-view] (.state this)]
    (.deregisterView peer model (remote-view model view)))) ;stop server for this view

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

