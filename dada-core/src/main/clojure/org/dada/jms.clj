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

;; ;;------------------------------------------------------------------------------

;; (defprotocol MessageServer
;;   (open [this])
;;   (close [this])
;;   (receive-send [this message]))

;; (deftype JMSMessageServer [target ^Session session message-strategy ^MessageProducer producer translator ^String end-point ^ExecutorService threads]
  
;;   MessageListener
  
;;   (^void onMessage [this ^Message message]
;; 	 (.execute threads (fn [] (.process this message))))
  
;;   MessageServer
  
;;   (open [this]
;; 	(let [ ;; we should be creatng our own thread pool here
;; 	      ^Destination destination (if end-point (.createQueue session end-point) (.createTemporaryQueue session))
;; 	      ^MessageConsumer consumer (.createConsumer session destination)]
;; 	  ;; we need to stash consumer and destination for later
;; 	  (.setMessageListener consumer this)))
  
;;   (close [this]
;; 	 ;; shutdown our threads
;; 	 ;; close our consumer
;; 	 ;; if we are using a temporary queue - delete it
;; 	 )

;;   (receive [_ message]
;; 	   (let [^Message request message
;; 		 correlation-id (.getJMSCorrelationID request)
;; 		 reply-to (.getJMSReplyTo request)
;; 		 results
;; 		 (try
;; 		   ;; TODO - (AbstractClient/setCurrentSession session) ;; TODO - hacky - we should own this ThreadLocally
;; 		   (let [func (.foreign-to-native translator (.readMessage message-strategy request))]
;; 		     [true (apply func target)])
;; 		   (catch Throwable t
;; 		     [false t]))]

;; 	     (if (and correlation-id reply-to)
;; 	       (let [^Message response (.createMessage message-strategy session)]
;; 		 (.setJMSCorrelationID response correlation-id)
;; 		 (.writeMessage message-strategy response (.native-to-foreign translator results))
;; 		 (.send producer reply-to response)))))

;;   )
;; ;;------------------------------------------------------------------------------

;; (defprotocol MessageClient
;;   (open [this])
;;   (close [this])
;;   (receive [this])
;;   (send [this]))


;; ;; we should be able to abstract just client bits into an async client
;; ;; from which a sync client could inherit and add exchange and
;; ;; server-side resources

;; (deftype JMSMessageClient [ ;;server
;; 			   threads exchangers translator message-strategy
;; 			   reply-to
;; 			   ;; client
;; 			   producer ;with implicit send-to destination
;; 			   send-to
;; 			   ids
;; 			   timeout	;millis
;; 			   ]
  

;;   MessageListener

;;   (^void onMessage [this ^Message message]
;; 	 (.execute thread (fn [] (.receive this message))))
  
;;   MessageClient

;;   (open [this]
;; 	;; create a temporary queue - reply-to
;; 	;; or use a permanent topic
;; 	(.setMessageListener reply-to this))
	
;;   (close [this]
;; 	 (.setMessageListener reply-to nil)
;; 	 ;;if using a temporary queue, delete it
;; 	 )
  
;;   (receive [this ^Message message]
;; 	   (let [id (.getJMSCorrelationID message)]
;; 	     (if-let [^Exchanger exchanger (swap2! exchangers (fn [old id] [(dissoc old id)(old id)]) id)]
;; 	       (do
;; 		 ;; TODO - (AbstractClient/setCurrentSession session) ;; TODO - hacky - we should own this ThreadLocally
;; 		 (.exchange exchanger (.foreign-to-native translator (.readMessage message-strategy message))))
;; 	       (warn "no exchanger for message: " id))))

;;   (send-async [_ invocation]
;; 	      (let [^Message message (.createMessage strategy session)]
;; 		(.writeMessage strategy message (.native-to-foreign translator invocation))
;; 		(.send producer message)))
  
;;   (send-sync [_ invocation]
;; 	     (let [^Message message (.createMessage strategy session)
;; 		   id (swap! ids inc)
;; 		   exchanger (Exchanger.)]
;; 	       (.setJMSCorrelationId message id)
;; 	       (.setJMSReplyTo message reply-to)
;; 	       (.writeMessage strategy message (.native-to-foreign translator invocation))
;; 	       (swap! exchangers assoc id exchanger)
;; 	       (.send producer message)
;; 	       (let [[is-exception results]
;; 		     (try
;; 		       (.exchange exchanger nil timeout (TimeUnit/MILLISECONDS))
;; 		       (catch Throwable t
;; 			 (swap! exchanger dissoc id)
;; 			 (throw t)))]
;; 		 (if is-exception
;; 		   (throw results)
;; 		   results))))
;;   )

;; ;;------------------------------------------------------------------------------
