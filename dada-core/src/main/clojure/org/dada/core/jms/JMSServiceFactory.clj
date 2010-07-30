(ns org.dada.core.jms.JMSServiceFactory
    (:import
     [java.lang.reflect Proxy]
     [java.util.concurrent ExecutorService]
     [javax.jms Message MessageListener MessageProducer ObjectMessage Session]
     [org.dada.core Getter]
     [org.dada.jms AbstractClient Invocation JMSServiceFactory$DestinationFactory MethodMapper Results SimpleMethodMapper SynchronousClient]
     )
    (:gen-class
     :implements [org.dada.core.ServiceFactory]
     :init init
     :constructors {[javax.jms.Session Class java.util.concurrent.ExecutorService Boolean Long org.dada.core.Getter org.dada.jms.JMSServiceFactory$DestinationFactory] []
     }
     :methods []
     :state state
     )
    )

;; TODO: use primitive types

(defmulti invoke (fn [target message session mapper producer] (class message)))

;; TODO:
;; error handling
;; true async needs no response
;; one endpoint per protocol
;; reimplement method mapping to avoid introspection overhead
;; complete client and decouple methods


;; POJOS
(defmethod invoke ObjectMessage [target #^ObjectMessage request #^Session session #^MethodMapper mapper #^MessageProducer producer]
  (AbstractClient/setCurrentSession session) ;; allows object being deserialised to find current session
  ;; TODO: handle URI ClassLoading stuff here...
  (let [#^Invocation invocation (.getObject request)
	;; TODO - handle Exceptions later
	method (.getMethod mapper (.getMethodIndex invocation))
	dummy (println "invoking: " method)
	results (.invoke method target (.getArgs invocation)) ;TODO: introspection - slow
	reply-to (.getJMSReplyTo request)
	correlation-id (.getJMSCorrelationID request)]
    (if (and reply-to correlation-id)
      (let [#^ObjectMessage response (.createObjectMessage session)]
	(.setJMSCorrelationID response correlation-id)
	(.setObject response (Results. false results))
	(.send producer reply-to response)
	))
    ))

(defn -init [#^Session session
	     #^Class interface
	     #^ExecutorService executor-service
	     #^Boolean true-async
	     #^Long timeout
	     #^Getter name-getter
	     #^JMSServiceFactory$DestinationFactory destination-factory]

  [ ;; super ctor args
   []

   (let [#^MethodMapper mapper (SimpleMethodMapper. interface)
	 #^MessageProducer producer (.createProducer session nil)

	 server-fn (fn [target end-point]
		       (println "SERVER INSTANCE" target)
		       (.setMessageListener
			(.createConsumer session (.createDestination destination-factory session end-point))
			(proxy [MessageListener] [] (onMessage [message] (.execute executor-service (proxy [Runnable] [] (run [] (invoke target message session mapper producer))))))))

	 client-fn (fn [#^String end-point]
		       (println "CLIENT INTERFACE" interface)
		       (Proxy/newProxyInstance
			(.getContextClassLoader (Thread/currentThread))
			(into-array Class [interface])
			(SynchronousClient. session (.createDestination destination-factory session end-point) interface timeout true-async)) ;; TODO: implement SynchronousClient here...
		       )

	 decouple-fn (fn [#^Object target]
			 (let [server-name (.get name-getter target)]
			   (server-fn target server-name)
			   (client-fn server-name)
			   ))]

     ;; instance state
     [decouple-fn client-fn server-fn])])

(defn -decouple [#^org.dada.core.jms.JMSServiceFactory this #^Object target]
  (let [[decouple-fn] (.state this)]
    (decouple-fn)))

(defn -client [#^org.dada.core.jms.JMSServiceFactory this #^String end-point]
  (let [[_ client-fn] (.state this)]
    (client-fn end-point)))

(defn -server [#^org.dada.core.jms.JMSServiceFactory this #^Object target #^String end-point]
  (let [[_ _ server-fn] (.state this)]
    (server-fn target end-point)
    target))
