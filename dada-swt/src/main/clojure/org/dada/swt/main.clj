(ns org.dada.swt.main
    (:use org.dada.demo.whales) ;hack - we shouldn't need to run server in-vm with us.
    (:use org.dada.swt.GridView)
    (:import
     (java.io Serializable)
     (java.util.concurrent Executors)
     (org.apache.activemq ActiveMQConnectionFactory)
     (javax.jms Session)
     (org.dada.core SessionManager SessionManagerNameGetter Update View ViewNameGetter)
     (org.dada.jms SimpleMethodMapper QueueFactory)
     (org.dada.core.jms JMSServiceFactory POJOInvoker)
     (org.dada.swt GridView)
     ))

(let [uri "tcp://localhost:61616"
      uri "vm://DADA?marshal=false&broker.persistent=false&create=false"
      connection (.createConnection (ActiveMQConnectionFactory. uri))
      session (.createSession connection false (Session/DUPS_OK_ACKNOWLEDGE))
      thread-pool (Executors/newFixedThreadPool 32)]
  (.start connection)
  
  (def session-manager-service-factory 
       (JMSServiceFactory.
	session
	SessionManager
	thread-pool
	true	     ;; true async
	(long 10000) ;; 10 sec timeout
	(SessionManagerNameGetter.)
	(QueueFactory.)
	(POJOInvoker.
	 (SimpleMethodMapper. SessionManager))
	"POJO"))

  (def view-service-factory 
       (JMSServiceFactory.
	session
	View
	thread-pool
	true	     ;; true async
	(long 10000) ;; 10 sec timeout
	(ViewNameGetter.)
	(QueueFactory.)
	(POJOInvoker.
	 (SimpleMethodMapper. View))
	"POJO"))
  )

;; create a projection of the remote session manager into our address space
(def session-manager (.client session-manager-service-factory "SessionManager"))

;; TODO: dirty hack - sets static :-(
(org.dada.core.SessionManagerHelper/setCurrentSessionManager session-manager)

;; get first model
(def model (.find session-manager "MetaModel" "MetaModel"))

;; create a local View
(.start
 (Thread.
  (fn [] (.start (make-grid-view model session-manager view-service-factory)))))

;; try a query
(.query session-manager "org.dada.dsl" "(? (ccount)(from \"Whales\"))")
