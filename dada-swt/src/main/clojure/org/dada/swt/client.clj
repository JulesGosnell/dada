(ns #^{:author "Jules Gosnell" :doc "Simple Client for DADA"} org.dada.swt.client
    (:gen-class)
    (:use
     [org.dada.swt inspect])
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

  (defn inspect-model-with-drilldown-and-shutdown [^Model model]
    (inspect-model model inspect-model-with-drilldown-and-shutdown (fn [] (.close session-manager))))

  (inspect-model-with-drilldown-and-shutdown *remote-metamodel*)
    
  )
