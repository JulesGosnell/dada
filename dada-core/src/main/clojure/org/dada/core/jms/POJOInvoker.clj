(ns org.dada.core.jms.POJOInvoker
    (:import
     [java.lang.reflect Method]
     [javax.jms MessageProducer BytesMessage Session]
     [org.dada.jms AbstractClient Invocation MethodMapper Results Utils]
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

(defn -invoke [#^org.dada.core.jms.POJOInvoker this target #^Session session #^BytesMessage request #^MessageProducer producer]
  (AbstractClient/setCurrentSession session) ;; allows object being deserialised to find current session
  ;; TODO: handle URI ClassLoading stuff here...
  (let [[#^MethodMapper mapper] (.state this)
	#^Invocation invocation (Utils/readObject request)
	;; TODO - handle Exceptions later
	#^Method method (.getMethod mapper (Integer. (.getMethodIndex invocation)))
	args (.getArgs invocation)
	;;dummy (println "POJO INCOMING:" (str "sessionManager." (.getName method) "(" (apply str (interpose ", " args)) ")"))
	results (.invoke method target args) ;TODO: introspection - slow
	reply-to (.getJMSReplyTo request)
	correlation-id (.getJMSCorrelationID request)]
    (if (and reply-to correlation-id)
      (let [#^BytesMessage response (.createBytesMessage session)]
	(.setJMSCorrelationID response correlation-id)
	(Utils/writeObject response (Results. false results))
	;;(println "POJO OUTGOING:" results)
	(.send producer reply-to response)
	))
    ))

