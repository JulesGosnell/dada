(ns
 org.dada.core.LocalSessionManager
 (:use [clojure.contrib logging])
 (:import
  [java.util Collection]
  [org.dada.core Data Metadata Model ServiceFactory View]
  )
 (:gen-class
  :implements [org.dada.core.NewSessionManager]
  :constructors {[String org.dada.core.Model org.dada.core.ServiceFactory] []}
  :methods []
  :init init
  :state state
  )
 )

(defn -init [^String name ^Model metamodel ^ServiceFactory service-factory]
  [ ;; super ctor args
   []
   (let [remoter nil]			;set up remoter fn
     ;; instance state
     [metamodel remoter]]))
  
;; getName - can be answered immediately by RemoteModel - so no need for it on this interface
;; getMetadata  - can be answered immediately by RemoteModel - so no need for it on this interface
;; getData - should we support this ? - lets not for the moment
;; getModel - no longer needed
;; -query - we won't support this in first implementation

;; local
(defn ^Model -find [^org.dada.core.SessionManagerImpl this ^String model-name key]
  (let [[metamodel remoter] (.state this)
	found (.find (.find metamodel model-name) key)]
    (if (instance? Model found) (remoter found) found)))

;; don't worry about topics yet

(defn ^Data -attach [^org.dada.core.SessionManagerImpl this ^String model-name ^View view]
  (let [[metamodel] (.state this)]
    (.attach (.find metamodel model-name) view)))

(defn ^Data -detach [^org.dada.core.SessionManagerImpl this ^String model-name ^View view]
  (let [[metamodel] (.state this)]
    (.detach (.find metamodel model-name) view)))

