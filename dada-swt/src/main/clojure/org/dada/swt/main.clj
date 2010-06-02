(ns org.dada.swt.main
    (:use
     org.dada.swt.GridView)
    (:import
     (java.io
      Serializable)
     ))

;;; should load from a spring-config

(import org.apache.activemq.ActiveMQConnectionFactory)
(import javax.jms.ConnectionFactory)
(import javax.jms.Connection)
(import javax.jms.Session)
(import org.dada.jms.RemotingFactory)
(import org.dada.core.MetaModel)
(import org.dada.core.View)
(import java.rmi.server.UID)
(import java.util.concurrent.ExecutorService)
(import java.util.concurrent.Executors)


(def server-name "Cetacea.MetaModel")
(def url "tcp://localhost:61616")
(def connection-factory (ActiveMQConnectionFactory. url))
(def connection (.createConnection connection-factory))
(.start connection)
(def session (.createSession connection false (Session/AUTO_ACKNOWLEDGE)))

;; create proxy to server-side metamodel

(def clientside-metamodel-proxy
     (.createSynchronousClient 
      (RemotingFactory. session MetaModel 10000) 
      (.createQueue session server-name)
      true))

;; create proxy for ourselves to send to server

(def client-destination (.createQueue session (str "Client" (UID.))))
(def client-view (GridView. "SWTGUI"))

(def view-remoting-factory (RemotingFactory. session View 10000))

(def clientside-view-server
     (.createServer
      view-remoting-factory 
      client-view
      client-destination
      (Executors/newFixedThreadPool 20)))

(def serverside-view-proxy
     (.createSynchronousClient
      view-remoting-factory
      client-destination
      true))

;; we need to View to create endpoint
;; we need endpoint to register interest in Model
;; we don't get Metadata until we have registered that interest
;; we need Metadata to build View
;; argh !

;; we will have to call some sort of late-initialise method on the View with the Metadata...

