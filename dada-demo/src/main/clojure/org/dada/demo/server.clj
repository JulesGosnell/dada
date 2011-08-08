(ns ^{:author "Jules Gosnell" :doc "Server for DADA Demo"} org.dada.demo.server
    (:use
     [org.dada.clojure web]
     [org.dada core]
     [org.dada.core remote]
     [org.dada.core server]
     [org.dada jms])
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
      SessionManagerImpl
      ]
     [org.dada.core.remote
      MessageStrategy
      Remoter
      SerialiseTranslator
      Translator]
     [org.dada.jms
      BytesMessageStrategy
      JMSRemoter]
     )
    )

;; TODO - most of this should probably live in spring...

(if (not *compile-files*)
  (do
    (let [^ConnectionFactory connection-factory (.getBean #^BeanFactory (ClassPathXmlApplicationContext. "server.xml") "connectionFactory")
	  ^javax.jms.Connection connection (doto (.createConnection connection-factory) (.start))
	  ^javax.jms.Session jms-session (.createSession connection false (javax.jms.Session/DUPS_OK_ACKNOWLEDGE))
	  num-threads 16		;TODO - hardwired
	  timeout 10000			;TODO - hardwired
	  ^ExecutorService threads (Executors/newFixedThreadPool num-threads)
	  ^MessageStrategy strategy (BytesMessageStrategy.)
	  ^Translator translator (SerialiseTranslator.)
	  ^Remoter remoter (JMSRemoter. jms-session threads strategy translator timeout "org.dada.POJO")]
      (def ^SessionManager session-manager
	   (session-manager-server (SessionManagerImpl. "SessionManager" *metamodel*) remoter)))

    ;; move this into SessionManagerImpl
    (def jetty (start-jetty 8888))
    ))
