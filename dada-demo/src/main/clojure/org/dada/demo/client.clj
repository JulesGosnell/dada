(ns 
    #^{:author "Jules Gosnell" :doc "Demo Client for DADA"}
  org.dada.demo.client
  (:use
   [org.dada core]
   [org.dada.core dql]
   [org.dada.swt new])
  (:import
   [java.net
    URL
    URLClassLoader]
   [java.util.concurrent
    Executors]
   [javax.jms
    Session]
   [clojure.lang
    Keyword]
   [org.apache.activemq
    ActiveMQConnectionFactory]
   [org.dada.core
    Metadata
    Metadata$VersionComparator
    Model
    SessionManagerNameGetter
    SessionManager
    SessionManagerHelper
    View
    ViewNameGetter]
   [org.dada.core.jms
    JMSServiceFactory
    POJOInvoker]
   [org.dada.jms
    QueueFactory
    TopicFactory
    SimpleMethodMapper]
   )
  )

(if (not *compile-files*)
  (do

    ;; install our class-loader in hierarchy
    (let [current-thread (Thread/currentThread)]
      (.setContextClassLoader
       current-thread
       (URLClassLoader.
	(into-array [(URL. "http://localhost:8888/")])
	(.getContextClassLoader current-thread))))

    (def connection-factory (ActiveMQConnectionFactory. "" "" "tcp://localhost:61616"))
    (doto connection-factory
      (.setOptimizedMessageDispatch true)
      (.setObjectMessageSerializationDefered true)
      (.setWatchTopicAdvisories false))
  
    (def connection (.createConnection connection-factory))
    (.start connection)

    (def session (.createSession connection false (Session/DUPS_OK_ACKNOWLEDGE)))
    ;;  (def topic (.createTopic session "MetaModel"

    (def thread-pool (Executors/newFixedThreadPool 2))
    (def session-manager-service-factory
	 (JMSServiceFactory.
	  session
	  SessionManager
	  thread-pool
	  true
	  10000
	  (SessionManagerNameGetter.)
	  (QueueFactory.)
	  (POJOInvoker. (SimpleMethodMapper. SessionManager))
	  "POJO"))
    (def view-service-factory
	 (JMSServiceFactory.
	  session
	  View
	  thread-pool
	  true
	  10000
	  (ViewNameGetter.)
	  (TopicFactory.)
	  (POJOInvoker. (SimpleMethodMapper. View))
	  "POJO"))

    (def session-manager (.client session-manager-service-factory "SessionManager"))
    (SessionManagerHelper/setCurrentSessionManager session-manager)

    ;; (def local-oceans (model "MetaModel" metamodel-metadata))

    ;; (.registerView session-manager "MetaModel"
    ;; 		   (.decouple view-service-factory local-metamodel))

    ;; (inspect-model local-metamodel)

    (def whales-model (model "Whales" (.getMetadata session-manager "Whales")))

    (.registerView session-manager "Whales" (.decouple view-service-factory whales-model))

    (inspect-model whales-model)

    ))