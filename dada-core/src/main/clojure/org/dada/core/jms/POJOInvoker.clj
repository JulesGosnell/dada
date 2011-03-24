(ns org.dada.core.jms.POJOInvoker
    (:import
     [java.lang.reflect Method]
     [javax.jms MessageProducer Message Session]
     [org.dada.core Translator SerializeTranslator]
     [org.dada.jms AbstractClient MessageStrategy BytesMessageStrategy Invocation MethodMapper Results]
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

;; TODO: these should be injected, so that we can support e.g. ML over TextMessages
(def ^MessageStrategy message-strategy (BytesMessageStrategy.))
(def ^Translator translator (SerializeTranslator.))

(defn -invoke [#^org.dada.core.jms.POJOInvoker this target #^Session session #^Message request #^MessageProducer producer]
  (AbstractClient/setCurrentSession session) ;; allows object being deserialised to find current session
  ;; TODO: handle URI ClassLoading stuff here...
  (let [[#^MethodMapper mapper] (.state this)
	#^Invocation invocation (.foreign-to-native translator (.readMessage message-strategy request))
	;; TODO - handle Exceptions later
	#^Method method (.getMethod mapper (Integer. (.getMethodIndex invocation)))
	args (.getArgs invocation)
	;;dummy (println "POJO INCOMING:" (str "sessionManager." (.getName method) "(" (apply str (interpose ", " args)) ")"))
	results (.invoke method target args) ;TODO: introspection - slow
	reply-to (.getJMSReplyTo request)
	correlation-id (.getJMSCorrelationID request)]
    (if (and reply-to correlation-id)
      (let [#^Message response (.createMessage message-strategy session)]
	(.setJMSCorrelationID response correlation-id)
	(.writeMessage message-strategy response (.foreign-to-native translator (Results. false results)))
	;;(println "POJO OUTGOING:" results)
	(.send producer reply-to response)
	))
    ))

