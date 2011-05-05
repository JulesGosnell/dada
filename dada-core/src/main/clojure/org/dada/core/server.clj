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

  (^int ping [this]
	(.ping local))
  
  (^long getLastPingTime [this]
	 (.getLastPingTime local))
  
  (^Data attach [this ^Model model ^View view]
	 (if (instance? RemoteView view) (.hack ^RemoteView view remoter))
	 (.attach local model view))

  (^Data detach [this ^Model model ^View view]
	 (if (instance? RemoteView view) (.hack ^RemoteView view remoter))
	 (.detach local model view))

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

  (^String getName [this]
	   (.getName local))

  (createSession [this user-name application-name application-version]
		 (session-server (.createSession local user-name application-name application-version) remoter))
  
  (^void close [this]
	 (.close local)))

(defn  session-manager-server [^SessionManagerImpl local ^Remoter remoter]
  (let [manager (SessionManagerServer. local remoter)
	server (.server remoter manager (.endPoint remoter (.getName local)))]
    (.addCloseHook local (fn [_] (.close server)))
    manager))
