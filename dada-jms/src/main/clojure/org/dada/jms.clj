(ns org.dada.jms
    (:use
     [clojure.tools logging]
     [org.dada core]
     [org.dada.core utils]
     [org.dada.core remote]
     )
    (:import
     [java.util.concurrent Exchanger ExecutorService TimeUnit]
     [javax.jms BytesMessage Destination Message MessageConsumer MessageProducer MessageListener Session TemporaryQueue TextMessage Topic Queue]
     [clojure.lang Atom]
     [org.dada.core.remote MessageStrategy MessageServer AsyncMessageClient SyncMessageClient Remoter Translator]
     )
    )

;;------------------------------------------------------------------------------

(deftype BytesMessageStrategy []
  MessageStrategy
  (createMessage [_ session]
		 (.createBytesMessage ^Session session))
  (readMessage [_ message]
	       (let [buffer (byte-array (.getBodyLength ^BytesMessage message))]
		 (.readBytes ^BytesMessage message buffer)
		 buffer))
  (writeMessage [_ message buffer]
		(.writeBytes ^BytesMessage message buffer) nil))

(deftype TextMessageStrategy []
  MessageStrategy
  (createMessage [_ session]
		 (.createTextMessage ^Session session))
  (readMessage [_ message]
	       (.getText ^TextMessage message))
  (writeMessage [_ message text]
		(.setText ^TextMessage message text) nil))

;;------------------------------------------------------------------------------

(deftype JMSMessageServer [target
			   ^Session session
			   ^MessageStrategy strategy
			   ^MessageProducer producer
			   ^Translator translator
			   ^MessageConsumer consumer
			   ^ExecutorService threads]
  
  MessageListener
  
  (onMessage [this message]
	     (.execute threads (fn [] (.receive this message))))
  
  MessageServer
  
  (open [this]
	(.setMessageListener consumer this))
  
  (close [this]
	 (.setMessageListener consumer nil)
	 (.close consumer)
	 ;;(.close producer);; TODO - closing a session is closing the producer before the results are sent back
	 )

  (receive [_ message]
	   (let [^Message request message
		 correlation-id (.getJMSCorrelationID request)
		 reply-to (.getJMSReplyTo request)
		 results
		 (try
		  (let [func (.foreignToNative translator (.readMessage strategy request))]
		    [true (func target)])
		  (catch Throwable t
			 (warn "unexpected problem handling incoming message" t)
			 (.printStackTrace t)
			 [false t]))]
	     (if (and correlation-id reply-to)
	       (let [^Message response (.createMessage strategy session)]
		 (.setJMSCorrelationID response correlation-id)
		 (.writeMessage strategy response (.nativeToForeign translator results))
		 (.send producer reply-to response)))))

  )

(defn ^MessageServer init-jms-message-server [target ^Session session ^MessageStrategy strategy ^Translator translator ^Destination reply-to ^ExecutorService threads]
  (let [consumer (.createConsumer session reply-to)
	producer (.createProducer session nil)]
    (doto (JMSMessageServer. target session strategy producer translator consumer threads) (.open))))

;;------------------------------------------------------------------------------

(deftype JMSAsyncMessageClient [^MessageStrategy strategy
				^Translator translator
				^Session session
				^MessageProducer producer]
  AsyncMessageClient

  (open [this]
	)
	
  (close [this]
	 (.close producer))
  
  (sendAsync [_ invocation]
	     (let [^Message message (.createMessage strategy session)]
	       (.writeMessage strategy message (.nativeToForeign translator invocation))
	       (.send producer message)))
  )

(defn ^AsyncMessageClient init-jms-async-message-client-2
  [^MessageStrategy strategy ^Translator translator ^Session session ^MessageProducer producer]
  (doto (JMSAsyncMessageClient. strategy translator session producer) (.open)))

(defn ^AsyncMessageClient init-jms-async-message-client
  [^MessageStrategy strategy ^Translator translator ^Session session ^String send-to-name]
  (let [send-to (.createTopic session send-to-name)
	producer (.createProducer session send-to)]
    (init-jms-async-message-client-2 strategy translator session producer)))

;;------------------------------------------------------------------------------

(deftype JMSSyncMessageClient [^MessageStrategy strategy
			       ^Translator translator
			       ^Atom exchangers
			       ^Atom ids
			       ^Long timeout
			       ^Session session
			       ^ExecutorService threads
			       ^MessageConsumer consumer
			       ^TemporaryQueue reply-to
			       ^MessageProducer producer
			       ^Queue send-to]
  MessageListener
  
  (^void onMessage [this ^Message message]
	 (.execute threads (fn [] (.receive this message))))

  SyncMessageClient

  (open [this]
	(.setMessageListener consumer this))
	
  (close [this]
	 (.setMessageListener consumer nil)
	 (.delete reply-to)
	 (.close consumer)
	 (.close producer))
  
  (receive [this message]
	   (let [id (.getJMSCorrelationID ^Message message)]
	     (if-let [[_ ^Exchanger exchanger] (swap2! exchangers (fn [old id] [(dissoc old id)(old id)]) id)]
	       (try
		 (.exchange exchanger message 0 (TimeUnit/MILLISECONDS))
		 (catch Throwable t (warn "message arrived - but just missed rendez-vous")))
	       (warn "message arrived - but no one waiting for it: " id))))

  (sendAsync [_ invocation]
	     (let [^Message message (.createMessage strategy session)]
	       (.writeMessage strategy message (.nativeToForeign translator invocation))
	       (.send producer message)))

  (sendSync [_ invocation]
	    (let [^Message message (.createMessage strategy session)
		  id (str (swap! ids inc))
		  exchanger (Exchanger.)]
	      (.setJMSCorrelationID message id)
	      (.setJMSReplyTo message reply-to)
	      (.writeMessage strategy message (.nativeToForeign translator invocation))
	      (swap! exchangers assoc id exchanger)
	      (.send producer message)
	      (let [[succeeded results]
		    ;; decode message this side of the exchanger so that large messages do not time out...
		    (.foreignToNative
		     translator
		     (.readMessage
		      strategy
		      (try
		       (.exchange exchanger nil timeout (TimeUnit/MILLISECONDS))
		       (catch Throwable t
			      (swap! exchangers dissoc id)
			      (throw t)))))]
		(if succeeded
		  results
		  (throw results))))))

(defn ^SyncMessageClient init-jms-sync-message-client
  ([^MessageStrategy strategy ^Translator translator ^Atom exchangers ^Atom ids ^Long timeout ^Session session ^ExecutorService threads ^MessageConsumer consumer ^TemporaryQueue reply-to ^MessageProducer producer ^Queue send-to]
   (doto (JMSSyncMessageClient. strategy translator exchangers ids timeout session threads consumer reply-to producer send-to) (.open)))
  ([^MessageStrategy strategy ^Translator translator ^Long timeout ^ExecutorService threads ^Session session ^Destination send-to]
   (let [exchangers (atom {})
	 ids (atom 0)
	 ^TemporaryQueue reply-to (.createTemporaryQueue session)
	 ^MessageConsumer consumer (.createConsumer session reply-to)
	 ^MessageProducer producer (.createProducer session send-to)]
     (init-jms-sync-message-client strategy translator exchangers ids timeout session threads consumer reply-to producer send-to))))

;;------------------------------------------------------------------------------

(deftype JMSRemoter [^Session session ^ExecutorService threads ^MessageStrategy strategy ^Translator translator ^long timeout ^String prefix]
  Remoter
  
  (server [this target reply-to]
	  (init-jms-message-server target session strategy translator reply-to threads))

  (syncClient [this send-to]
	      (init-jms-sync-message-client strategy translator timeout threads session send-to))

  (asyncClient [this send-to]
	       (init-jms-async-message-client strategy translator session send-to))

  (endPoint [this name]
	    (.createQueue session (str prefix "." name)))

  (endPoint [this name one-to-many]
	    (.createTopic session(str prefix "." name)))

  (endPoint [this]
	    (.createTemporaryQueue session))

  (close [this]
	 (.close session)
	 (.shutdown threads))

  )
