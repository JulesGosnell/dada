(ns org.dada.jms
    (:import
     [javax.jms BytesMessage Message Session TextMessage]
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
