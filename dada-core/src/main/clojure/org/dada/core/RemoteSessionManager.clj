(ns
 org.dada.core.RemoteSessionManager
 (:use [clojure.contrib logging])
 (:import
  [java.util Collection]
  [org.dada.core Data Metadata Model ServiceFactory View]
  )
 (:gen-class
  :implements [org.dada.core.NewSessionManager]
  :constructors {[String org.dada.core.ServiceFactory] []}
  :methods []
  :init init
  :state state
  )
 )

(defn -init [^String name ^ServiceFactory service-factory]
  [ ;; super ctor args
   []
   ;; instance state
   (let [
	 peer nil ;; need to attach to peer here
	 remoter nil ;; need to set up remoter here 
	 ]
     [peer remoter])])

;; getName - can be answered immediately by RemoteModel - so no need for it on this interface
;; getMetadata  - can be answered immediately by RemoteModel - so no need for it on this interface

;; getModel - no longer needed

;; getData - should we support this ? - lets not for the moment
;; -query - we won't support this in first implementation

;; RemoteViews need to be equal so that detach will work
;; since they are going across the wire, do we need to maintain a list of RemoteViews that we have allocated ?

(defn ^Model -find [^org.dada.core.SessionManagerImpl this ^String model-name key]
  (let [[peer] (.state this)]
    ;; TODO - if we are handing out a Model - spike it
    ;; actually - put the SPIKE in a ThreadLocal and read it during deserialisation of View/Model ?
    ;; then we can mess with nested Models/Views ok ...
    ;; use spiking strategy for the moment
    ;; need an let-instance? macro - (let-instance? RemoteModel model (.find...)) (doto model (.setSessionManager this)) object)
    (.find peer model-name key)))

;; don't worry about topics yet

(defn ^Data -attach [^org.dada.core.SessionManagerImpl this ^String model-name ^View view]
  (let [[peer remoter] (.state this)]
    (.attach peer model-name (remoter view))))

(defn ^Data -detach [^org.dada.core.SessionManagerImpl this ^String model-name ^View view]
  (let [[peer remoter] (.state this)]
    (.detach peer model-name (remoter view))))

