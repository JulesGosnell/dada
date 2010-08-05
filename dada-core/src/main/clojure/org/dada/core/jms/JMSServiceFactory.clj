(ns org.dada.core.jms.JMSServiceFactory
    (:import
     [java.lang.reflect Proxy]
     [java.util.concurrent ExecutorService]
     [javax.jms Message MessageListener Session]
     [org.dada.core Getter View]
     [org.dada.core.jms Invoker]
     [org.dada.jms DestinationFactory SynchronousClient]
     )    
    (:gen-class
     :implements [org.dada.core.ServiceFactory]
     :init init
     :constructors {[javax.jms.Session Class java.util.concurrent.ExecutorService Boolean Long org.dada.core.Getter org.dada.jms.DestinationFactory org.dada.core.jms.Invoker String] []
     }
     :methods []
     :state state
     )
    )

(defn -init [#^Session session
	     #^Class interface
	     #^ExecutorService executor-service
	     #^Boolean true-async
	     #^Long timeout
	     #^Getter name-getter
	     #^DestinationFactory destination-factory
	     #^Invoker invoker
	     #^String protocol]

  [ ;; super ctor args
   []
   ;; instance state
   [session destination-factory executor-service (.createProducer session nil) interface timeout true-async name-getter invoker protocol]
   ])

(defn -decouple [#^org.dada.core.jms.JMSServiceFactory this #^Object target]
  (let [[_ _ _ _ _ _ _ name-getter] (.state this)
	server-name (.get name-getter target)]
    (.server this target server-name)
    (.client this server-name)
    ))

(defn -client [#^org.dada.core.jms.JMSServiceFactory this #^String end-point]
  (let [[session destination-factory _ _ interface timeout true-async _ _ protocol] (.state this)]
    (Proxy/newProxyInstance
     (.getContextClassLoader (Thread/currentThread))
     (into-array Class [interface])
     (SynchronousClient. session (.createDestination destination-factory session (str end-point "." protocol)) interface timeout true-async))))

(defn -server [#^org.dada.core.jms.JMSServiceFactory this #^Object target #^String end-point]
  (let [[session destination-factory executor-service producer _ _ _ _ invoker protocol] (.state this)]
    (.setMessageListener
     (.createConsumer session (.createDestination destination-factory session (str end-point "." protocol)))
     (proxy [MessageListener] [] (onMessage [message] (.execute executor-service (fn [] (.invoke invoker target session message producer))))))
    target))
