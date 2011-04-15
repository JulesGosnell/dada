(ns ^{:author "Jules Gosnell" :doc "Simple Client for DADA"} org.dada.swt.client
    (:gen-class)
    (:use
     [org.dada jms]
     [org.dada.swt inspect])
    (:import
     [java.util.concurrent
      Executors ExecutorService]
     [org.springframework.context.support
      ClassPathXmlApplicationContext]
     [org.springframework.beans.factory
      BeanFactory]
     [org.dada.core
      Model
      RemoteModel
      Session
      SessionManager
      ]
     [org.dada.core.remote
      MessageStrategy
      Remoter
      Translator
      ]
     )
    )

(defn -main [& args]

  (.setContextClassLoader (Thread/currentThread) (clojure.lang.RT/makeClassLoader))

  (let [^SessionManager session-manager (.getBean ^BeanFactory (ClassPathXmlApplicationContext. "client.xml") "sessionManager")
	^Session session (.createSession session-manager)
	^Model remote-metamodel  (.find session (RemoteModel. "MetaModel" nil) "MetaModel")]
    
    (defn inspect-model-with-drilldown-and-shutdown [^Model model]
      (inspect-model model inspect-model-with-drilldown-and-shutdown (fn [] (.close session)  (.close session-manager))))
    
    (inspect-model-with-drilldown-and-shutdown remote-metamodel)
    
    ))
