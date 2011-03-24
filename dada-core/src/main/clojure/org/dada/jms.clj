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
		   (let [[func args] (.foreign-to-native translator (.readMessage message-strategy request))]
		     [true (apply func target args)])
		   (catch Throwable t
		     [false t]))]

	     (if (and correlation-id reply-to)
	       (let [^Message response (.createMessage message-strategy session)]
		 (.setJMSCorrelationID response correlation-id)
		 (.writeMessage message-strategy response (.foreign-to-native translator results))
		 (.send producer reply-to response)))))

  )
	 
;; we also need a MessageClient - which should be wrapped in a proxy ?
;; it will receive a Method and args[]
;; can we expand these using a macro, build a lambda and the send that - or is a proxy the wrong approach ?
;; I guess MessageClient needs to be a macro which expands a protocol/interface with bodies that just post closures to a MessageProducer...
;; should be fun :-)
;; maybe the invocation should be : [(fn [target & args]) args] - then we can reuse the same function class for evey invocation...

;; returns a fn that can be used to create proxies - but no reflection is used - all code is generated and compiled into classes on-the-fly

;; (import java.lang.reflect.Method)

;; (defn make-proxy-method [^Class interface ^Method method]
;;   (let [method-name (.getName method)
;; 	params (map (fn [i] (symbol (str "arg" i))) (range (count (.getParameterTypes method))))]
;;     (list
;;      (symbol method-name) (apply vector 'this params)
;;      (list 'invoker (list 'fn '[target] (apply list (symbol (str "." method-name)) 'target params)))
;;      )))

;; (defmacro defproxy-type [name & interfaces]
;;   `(deftype ~name [invoker]
;;      ~@(mapcat
;; 	  (fn [interface]
;; 	    (concat
;; 	     [interface]
;; 	     (map
;; 	      (fn [^Method method]
;; 		(make-proxy-method interface method))
;; 	      (.getMethods (eval interface)))))
;; 	  interfaces)
;;     ))

;; ;; not quite right - needs a function that is applied to target [args]
;; (deftype Jules [invoker]
;;   org.dada.core.View
;;   (update [this arg0 arg1 arg2] (invoker (fn [target] (.update target arg0 arg1 arg2)))))

;; ;; macro should do this :

;; ;; create a unique namespace
;; ;; within this create a unique function for each method taking an object (target) type hinted with respective interface
;; ;; also create a deftype which implements all interfaces each method calling 'invoker' with the relevant fn defined above, a vector/array of its args
;; ;; creating an instance of this type with the relevant invoker then calling a method on it will do the following :
;; ;; e.g. invoker receives [the same] function [each time] long with seq of params and sends these to the remote peer
;; ;; it receives results indicating that it should either throw the enclosed exception or return the enclosed value back up through the proxy :-)

;; ;; TODO - handle method param overloading - I'll leave that one for later :-)

;; ;; this is better tahn using a java.lang.proxy and there is no
;; ;; introspection needed to invoke said method on other side - we just
;; ;; pull the class that implements the fn over (only the first time)
;; ;; then execute the function's bytecode every time we receive the
;; ;; invocation - no further compilation, introspection or anything :-)

;; we should be able to implement a similar proxy/interceptor that
;; passes the method id through so that ut can be used to compose and
;; XML message

;; at the other end the XML xould be unpacked and the method looked up
;; through synbol evaluation or introspectin - i should do some
;; timings to see which is fastest.
