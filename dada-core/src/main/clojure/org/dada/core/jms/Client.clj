(ns org.dada.core.jms.Client
    (:import
     [java.io
      IOException ObjectInputStream ObjectOutputStream Serializable]
     [java.lang.reflect
      InvocationHandler Method Proxy]
     [java.util
      Map UUID]
     [javax.jms
      Destination JMSException Message ObjectMessage Session MessageConsumer MessageListener MessageProducer Queue]
     [org.dada.slf4j
      Logger LoggerFactory]
     [org.dada.jms
      SynchronousClient]
     )
    (:gen-class
     :implements [java.lang.reflect.InvocationHandler java.io.Serializable] ;;  MessageListener
     :constructors {[javax.jms.Session javax.jms.Destination Class Long Boolean] []}
     :methods []
     :init init
     :state state
     )
    )

;; TODO: need getDestination method

(defn -init [^Session session
	     ^Destination destination
	     ^Class interface
	     ^Long timeout
	     ^Boolean true-async]

  [ ;; super ctor args
   []
   ;; instance state
   (let [invoke (fn []
		    ;; public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		    ;; 	ObjectMessage message = session.createObjectMessage();
		    ;; 	Integer methodIndex = mapper.getKey(method);
		    ;; 	if (methodIndex == null) {
		    ;; 		// log.warn("unproxied method invoked: {}", method);
		    ;; 		return method.invoke(this, args);
		    ;; 	}
		    ;; 	message.setObject(new Invocation(methodIndex, args));

		    ;; 	// TODO: whether a method is to be used asynchronously should be stored with it to save runtime overhead...
		    ;; 	boolean async = trueAsync && method.getReturnType().equals(Void.TYPE) && method.getExceptionTypes().length == 0;

		    ;; 	if (async) {
		    ;; 		LOGGER.trace("SENDING ASYNC: {} -> {}", method, destination);
		    ;; 		producer.send(destination, message);
		    ;; 		return null;
		    ;; 	} else {
		    ;; 		String correlationId = "" + count++;
		    ;; 		LOGGER.trace(System.identityHashCode(this) + ": setting correlationId: {}:{}", System.identityHashCode(this), correlationId);
		    ;; 		message.setJMSCorrelationID(correlationId);
		    ;; 		message.setJMSReplyTo(resultsQueue);
		    ;; 		Exchanger<Results> exchanger = new Exchanger<Results>();
		    ;; 		correlationIdToResults.put(correlationId, exchanger);
		    ;; 		LOGGER.trace("SENDING SYNC: {} -> {}", method, destination);
		    ;; 		producer.send(destination, message);
		    ;; 		long start = System.currentTimeMillis();
		    ;; 		try {
		    ;; 			Results results = exchanger.exchange(null, timeout, TimeUnit.MILLISECONDS);
		    ;; 			Object value = results.getValue();
		    ;; 			if (results.isException())
		    ;; 				throw (Exception) value;
		    ;; 			else
		    ;; 				return value;
		    ;; 		} catch (TimeoutException e) {
		    ;; 			long elapsed = System.currentTimeMillis() - start;
		    ;; 			correlationIdToResults.remove(correlationId);
		    ;; 			LOGGER.warn("timeout was: {}", timeout);
		    ;; 			LOGGER.warn("timed out, after " + elapsed + " millis, waiting for results from invocation: " + method + " on " + destination); // TODO: SLF4j-ise
		    ;; 			throw e;
		    ;; 		}
		    ;; 	}

		    ;; }
		    )

	 readObject (fn [^ObjectInputStream ois]
			;; mutable state should go into an atom
			(let [session :todo
			      ;; Session session = getCurrentSession();
			      destination (.readObject ois)
			      interface (.readObject ois)
			      timeout (.readLong ois)
			      true-async (.readBoolean ois)]
			  ;; now mutate our own state to match this...
			  )
			)

	 writeObject (fn [^ObjectOutputStream oos]
			 (.writeObject oos destination)
			 (.writeObject oos interface)
			 (.writeLong oos timeout)
			 (.writeBoolean oos true-async))

	 equals (fn [^Object object]
		    (and (not (nil? object))
			 (let [that (if (Proxy/isProxyClass (.getClass object)) (Proxy/getInvocationHandler object) object)]
			   (and (instance? SynchronousClient that)
				(let [^Destination that-destination (.getDestination ^SynchronousClient that)]
				  (.equals that-destination destination))))))

	 hashCode (fn [] (.hashCode destination))

	 toString (fn [this]
		      "Client"
		      ;;return "<" + getClass().getSimpleName() + ": " + destination + ">";
		      )
	 ]
     [invoke readObject equals hashCode toString])
   ])


(defn -invoke [^org.dada.core.jms.Client this proxy ^Method method args]
  (let [[invoke] (.state this)]		;should just use a Clojure (proxy) - would it serialise ?
    (invoke proxy method args)))

(defn -readObject [^org.dada.core.jms.Client this ^ObjectInputStream ois]
  (let [[_ readObject] (.state this)]
    (readObject ois)))

(defn ^boolean -equals [^org.dada.core.jms.Client this ^Object that]
  (let [[_ _ equals] (.state this)]
    (equals that)))

(defn ^int -hashCode [^org.dada.core.jms.Client this]
  (let [[_ _ _ hashCode] (.state this)]
    (hashCode)))

;; (defn ^String -toString [^org.dada.core.jms.Client this]
;;   (let [[_ _ _ _ toString] (.state this)]
;;     (toString this)))

