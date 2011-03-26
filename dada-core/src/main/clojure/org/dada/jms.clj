(ns org.dada.jms
  (:import
   [java.util.concurrent ExecutorService]
   [javax.jms BytesMessage Message MessageConsumer MessageProducer MessageListener Session TextMessage]
   )
  )

(defprotocol MessageStrategy
  (createMessage [this ^Session session])
  (readMessage [this ^Message message])
  (writeMessage [this ^Message message body]))

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

(defprotocol MessageServer
  (open [this])
  (close [this])
  (process [this message]))

(deftype JMSMessageServer [target ^Session session message-strategy ^MessageProducer producer translator ^String end-point ^ExecutorService threads]
  
  MessageListener
  
  (^void onMessage [this ^Message message]
	 (.execute threads (fn [] (.process this message))))
  
  MessageServer
  
  (open [this]
	(let [ ;; we should be creatng our own thread pool here
	      ^Destination destination (if end-point (.createQueue session end-point) (.createTemporaryQueue session))
	      ^MessageConsumer consumer (.createConsumer session destination)]
	  ;; we need to stash consumer and destination for later
	  (.setMessageListener consumer this)))
  
  (close [this]
	 ;; shutdown our threads
	 ;; close our consumer
	 ;; if we are using a temporary queue - delete it
	 )

  (process [_ message]
	   (let [^Message request message
		 correlation-id (.getJMSCorrelationID request)
		 reply-to (.getJMSReplyTo request)
		 results
		 (try
		   ;; TODO - (AbstractClient/setCurrentSession session) ;; TODO - hacky - we should own this ThreadLocally
		   (let [func (.foreign-to-native translator (.readMessage message-strategy request))]
		     [true (apply func target)])
		   (catch Throwable t
		     [false t]))]

	     (if (and correlation-id reply-to)
	       (let [^Message response (.createMessage message-strategy session)]
		 (.setJMSCorrelationID response correlation-id)
		 (.writeMessage message-strategy response (.foreign-to-native translator results))
		 (.send producer reply-to response)))))

  )
