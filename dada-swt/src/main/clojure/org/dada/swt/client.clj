(ns #^{:author "Jules Gosnell" :doc "Simple Client for DADA"} org.dada.swt.client
    (:gen-class)
    (:use
     [org.dada.swt new])
    (:import
     [org.springframework.context.support
      ClassPathXmlApplicationContext]
     [org.springframework.beans.factory
      BeanFactory]
     [org.dada.core
      Model
      RemoteModel
      SessionManager
      ]
     )
    )

(defn -main [& args]

  (.setContextClassLoader (Thread/currentThread) (clojure.lang.RT/makeClassLoader))
  
  (def ^SessionManager session-manager (.getBean #^BeanFactory (ClassPathXmlApplicationContext. "client.xml") "sessionManager"))
  
  (def ^Model *remote-metamodel*  (.find session-manager (RemoteModel. "MetaModel" nil) "MetaModel"))

  (inspect-model *remote-metamodel* inspect-model)
    
  )
