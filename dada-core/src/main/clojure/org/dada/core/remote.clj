(ns org.dada.core.remote
  (:import
   [java.io ByteArrayInputStream ByteArrayOutputStream ObjectOutputStream]
   [org.dada.core ClassLoadingAwareObjectInputStream]
   ))

;;--------------------------------------------------------------------------------

(definterface Translator
  (^Object nativeToForeign [^Object object])
  (^Object foreignToNative [^Object object]))

(deftype SerialiseTranslator []
  Translator
  (nativeToForeign [_ object]
		   (let [baos (ByteArrayOutputStream.)
			 oos (ObjectOutputStream. baos)]
		     (try
		      (.writeObject oos object)
		      (finally
		       (.close oos)
		       (.close baos)))
		     (.toByteArray baos)))
  (foreignToNative [_ buffer]
		   (let [bais (ByteArrayInputStream. buffer)
			 ois (ClassLoadingAwareObjectInputStream. bais)]
		     (try
		      (.readObject ois)
		      (finally
		       (.close ois)
		       (.close bais))))))

;;------------------------------------------------------------------------------

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
  (endPoint [name one-to-many])	;named topic - varargs does not seem to work in definterface
  (close []))
					
