(ns 
 #^{:author "Jules Gosnell" :doc "Demo Server for DADA"}
 org.dada.demo.server
 (:use
  [org.dada web]
  [org.dada core])
 (:import
  [org.dada.core
   ServiceFactory
   SessionManager
   SessionManagerNameGetter
   SessionManagerImpl
   View
   ViewNameGetter]
  [org.dada.core.jms
   JMSServiceFactory
   POJOInvoker]
  [org.dada.jms
   SimpleMethodMapper]
  [org.springframework.beans.factory
   BeanFactory])
 )

(if (not *compile-files*)
  (do
    
    (start-server)
  
    (def jetty (start-jetty 8888))
  
    (def ^ServiceFactory *pojo-view-service-factory*
	 (JMSServiceFactory.
	  (.getBean ^BeanFactory *spring-context* "session")
	  View
	  (.getBean ^BeanFactory *spring-context* "executorService")
	  true
	  10000
	  (ViewNameGetter.)
	  (.getBean ^BeanFactory *spring-context* "topicFactory")
	  (POJOInvoker. (SimpleMethodMapper. View))
	  "POJO"))
      
    (def ^SessionManager *pojo-session-manager*
	 (SessionManagerImpl. "SessionManager" *metamodel* *pojo-view-service-factory*))

    (def ^ServiceFactory *pojo-session-manager-service-factory*
	 (JMSServiceFactory.
	  (.getBean ^BeanFactory *spring-context* "session") 
	  SessionManager
	  (.getBean ^BeanFactory *spring-context* "executorService")
	  true
	  10000
	  (SessionManagerNameGetter.)
	  (.getBean ^BeanFactory *spring-context* "queueFactory")
	  (POJOInvoker. (SimpleMethodMapper. SessionManager))
	  "POJO"))

    (.server ^JMSServiceFactory *pojo-session-manager-service-factory* *pojo-session-manager* "SessionManager")

    ))
