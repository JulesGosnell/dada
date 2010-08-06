(ns org.dada.core.jms.POJOInvoker
    (:import
     [java.lang.reflect Method]
     [javax.jms MessageProducer ObjectMessage Session]
     [org.dada.jms AbstractClient Invocation MethodMapper Results]
     )
    (:gen-class
     :implements [org.dada.core.jms.Invoker]
     :init init
     :constructors {[org.dada.jms.MethodMapper][]}
     :methods []
     :state state
     )
    )

(defn -init [#^MethodMapper mapper]
  [ ;; super ctor args
   []
   ;; instance state
   [mapper]
   ])

(defn -invoke [#^org.dada.core.jms.POJOInvoker this target #^Session session #^ObjectMessage request #^MessageProducer producer]
  (AbstractClient/setCurrentSession session) ;; allows object being deserialised to find current session
  ;; TODO: handle URI ClassLoading stuff here...
  (let [[#^MethodMapper mapper] (.state this)
	#^Invocation invocation (.getObject request)
	;; TODO - handle Exceptions later
	#^Method method (.getMethod mapper (.getMethodIndex invocation))
	args (.getArgs invocation)
	dummy (println "POJO INCOMING:" (str "sessionManager." (.getName method) "(" (apply str (interpose ", " args)) ")"))
	results (.invoke method target args) ;TODO: introspection - slow
	reply-to (.getJMSReplyTo request)
	correlation-id (.getJMSCorrelationID request)]
    (if (and reply-to correlation-id)
      (let [#^ObjectMessage response (.createObjectMessage session)]
	(.setJMSCorrelationID response correlation-id)
	(.setObject response (Results. false results))
	(println "POJO OUTGOING:" results)
	(.send producer reply-to response)
	))
    ))

