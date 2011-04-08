(ns ^{:author "Jules Gosnell" :doc "Client/Server code for DADA interfaces"} org.dada.core.server
    (:use
     [org.dada core]
     [org.dada.core remote]
     )
    (:require
     [org.dada.core
      RemoteSession
      RemoteView
      SessionImpl
      SessionManagerImpl])
    (:import
     [org.dada.core
      Data
      Model
      RemoteSession
      RemoteView
      Session
      SessionImpl
      SessionManager
      SessionManagerImpl
      View]
     [org.dada.core.remote
      Remoter]
     )
    )

(deftype SessionServer [^Session local ^Remoter remoter queue ^Session remote]

  Session

  (^boolean ping [this]
	    (.ping local))
  
  (^long getLastPingTime [this]
	 (.getLastPingTime local))
  
  (^Data registerView [this ^Model model ^View view]
	 (if (instance? RemoteView view) (.hack ^RemoteView view remoter))
	 (.registerView local model view))

  (^Data deregisterView [this ^Model model ^View view]
	 (if (instance? RemoteView view) (.hack ^RemoteView view remoter))
	 (.deregisterView local model view))

  (^Model find [this ^Model model key]
	 (.find local model key))

  (^Data getData [this ^Model model]
	 (.getData local model))

  (^Model query [this ^String query]
	  (.query local query))

  (^void close [this]
	 (.close local)))

(defn session-server [^SessionImpl local ^Remoter remoter]
  (let [queue (.endPoint remoter)
	remote (RemoteSession. queue)
	manager (SessionServer. local remoter queue remote)
	server (.server remoter manager queue)]
    (.addCloseHook local (fn [_] (.close server) (.delete queue))) ;TODO - should be done by Remoter
    remote))

(deftype SessionManagerServer [^SessionManager local ^Remoter remoter]

  SessionManager

  (getName [this]
	   (.getName local))

  (^Session createSession [this]
	    (session-server (.createSession local) remoter))
  
  (^void close [this]
	 (.close local)))

(defn  session-manager-server [^SessionManagerImpl local ^Remoter remoter]
  (let [manager (SessionManagerServer. local remoter)
	server (.server remoter manager (.endPoint remoter (.getName local)))]
    (.addCloseHook local (fn [_] (.close server)))
    manager))
