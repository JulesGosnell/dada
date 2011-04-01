(ns org.dada.core.remote)

(definterface MessageStrategy
  (createMessage [session])
  (readMessage [message])
  (^void writeMessage [message body]))

(definterface MessageServer
  (^void open [])
  (^void close [])
  (^void receive [message]))

(definterface AsyncMessageClient
  (^void open [])
  (^void close [])
  (^void sendAsync [invocation]))

(definterface SyncMessageClient
  (^void open [])
  (^void close [])
  (^void receive [message])
  (^Object sendSync [invocation])
  (^void sendAsync [invocation]))

(definterface Remoter
  (^org.dada.core.remote.MessageServer server [target reply-to])
  (^org.dada.core.remote.AsyncMessageClient asyncClient [endpoint])
  (^org.dada.core.remote.SyncMessageClient syncClient [endpoint])
  (endPoint [])				;temporary queue
  (endPoint [name])			;named queue
  (endPoint [name one-to-many]))
					;named topic - varargs does not seem to work in definterface
