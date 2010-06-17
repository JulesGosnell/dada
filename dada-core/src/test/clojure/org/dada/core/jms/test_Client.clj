(ns org.dada.core.jms.test-Client
    (:use 
     [clojure test]
     [org.dada.core.jms Client])
    (:import
     [java.io
      ByteArrayInputStream
      ByteArrayOutputStream
      ObjectInputStream
      ObjectOutputStream
      Serializable]
     [javax.jms
      Destination Session]
     [org.dada.slf4j
      Logger LoggerFactory]
     [org.dada.core
      View]
     [org.dada.core.jms
      Client]
     )
    )

(deftest serialisability
  (let [session (proxy [Session  Serializable] [])
	destination (proxy [Destination Serializable] [])
	client1 (Client. session destination View (long 1000) true)
	baos (ByteArrayOutputStream.)
	oos (ObjectOutputStream. baos)]
    (.writeObject oos client1)
    (.close oos)
    (let [bais (ByteArrayInputStream. (.toByteArray baos))
	  ois (ObjectInputStream. bais)
	  client2 (.readObject ois)]
      (.close ois)
      client2)))

    
  