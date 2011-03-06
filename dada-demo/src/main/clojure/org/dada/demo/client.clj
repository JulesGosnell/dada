(ns #^{:author "Jules Gosnell" :doc "Demo Client for DADA"} org.dada.demo.client
    (:gen-class)
    (:use
     [org.dada core]
     [org.dada.core dql]
     [org.dada.swt new])
    (:import
     [org.dada.core
      Model
      RemoteSessionManager
      SessionManager
      ]
     )
    )

(defn -main [& args]

  (def ^SessionManager session-manager (RemoteSessionManager. "POJO" nil nil))
  
  (def ^Model *remote-metamodel*  (.find session-manager "MetaModel" "MetaModel"))

  (inspect-model *remote-metamodel* inspect-model)
    
  )
