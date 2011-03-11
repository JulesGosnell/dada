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
      SessionManager
      ]
     )
    )

(defn -main [& args]

  (def ^SessionManager session-manager (.getBean #^BeanFactory (ClassPathXmlApplicationContext. "client.xml") "sessionManager"))
  
  (def ^Model *remote-metamodel*  (.find session-manager "MetaModel" "MetaModel"))

  (inspect-model *remote-metamodel* inspect-model)
    
  )
