(ns 
    #^{:author "Jules Gosnell" :doc "Demo Server for DADA"}
  org.dada.demo.server
  (:use
   [org.dada web]
   [org.dada core]
   [org.dada.core remote])
  (:import
   [java.util.concurrent
    Executors
    ExecutorService]
   [javax.jms
    ConnectionFactory]
   [org.springframework.context.support
    ClassPathXmlApplicationContext]
   [org.springframework.beans.factory
    BeanFactory]
   [org.dada.core
    SessionManager
    SessionManagerNameGetter
    SessionManagerImpl
    View
    ViewNameGetter]
   [org.dada.core.remote
    MessageStrategy
    Remoter
    SerializeTranslator
    Translator
    ]
   [org.dada.jms
    BytesMessageStrategy
    JMSRemoter]
   )
  )

(if (not *compile-files*)
  (do
    (let [^ConnectionFactory connection-factory (.getBean #^BeanFactory (ClassPathXmlApplicationContext. "server.xml") "connectionFactory")
	  ^javax.jms.Connection connection (doto (.createConnection connection-factory) (.start))
	  ^javax.jms.Session jms-session (.createSession connection false (javax.jms.Session/DUPS_OK_ACKNOWLEDGE))
	  num-threads 16		;TODO - hardwired
	  timeout 10000			;TODO - hardwired
	  ^ExecutorService threads (Executors/newFixedThreadPool num-threads)
	  ^MessageStrategy strategy (BytesMessageStrategy.)
	  ^Translator translator (SerializeTranslator.)
	  ^Remoter remoter (JMSRemoter. jms-session threads strategy translator timeout)]
      (def ^SessionManager session-manager (SessionManagerImpl. "SessionManager.POJO" remoter *metamodel*)))

    ;; move this into SessionManagerImpl
    (def jetty (start-jetty 8888))
    ))
