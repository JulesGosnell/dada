(ns 
 #^{:author "Jules Gosnell" :doc "Demo Server for DADA"}
 org.dada.demo.server
 (:use
  [org.dada web]
  [org.dada core])
 (:import
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
  [org.dada.jms
   SimpleMethodMapper]
  [org.springframework.beans.factory
   BeanFactory])
 )

(if (not *compile-files*)
  (do

    (def ^ConnectionFactory connection-factory (.getBean #^BeanFactory (ClassPathXmlApplicationContext. "server.xml") "connectionFactory"))
    (def ^SessionManager session-manager (SessionManagerImpl. "SessionManager.POJO" connection-factory *metamodel*))
    
    ;; move this into SessionManagerImpl
    (def jetty (start-jetty 8888))
    ))
