(ns
 org.dada.core.SessionManagerImpl
 (:import
  [org.dada.core Deregistration MetaModel Model Registration ServiceFactory View]
  )
 (:gen-class
  :implements [org.dada.core.SessionManager]
  :constructors {[org.dada.core.MetaModel org.dada.core.ServiceFactory] []}
  :methods []
  :init init
  :state state
  )
 )

(defn -init [#^MetaModel metamodel #^ServiceFactory service-factory]
  [ ;; super ctor args
   []
   ;; instance state
   (let [exports {}]
     [[metamodel service-factory]
      (atom [exports])])])

(defn #^String -getName [#^org.dada.core.SessionManagerImpl this]
  (let [[[#^MetaModel metamodel]] (.state this)]
    (.getName metamodel)))

(defn #^Model -getModel [#^org.dada.core.SessionManagerImpl this #^String name]
  (let [[[#^MetaModel metamodel]] (.state this)]
    (.getModel metamodel name)))

;; TODO: modification of our data model should be encapsulated in a fn
;; and handed of to Model so that it is all done within the scope of
;; the Model's single spin lock. This will resolve possible race and
;; ordering issues. Fn will probably need to implement a Strategy i/f
;; so that it plays ball with Java API

;; TODO: I think we will need multiple SessionManagers so that one can
;; speak Serialised POJOs, one XML etc...

(defn #^Registration -registerView [#^org.dada.core.SessionManagerImpl this #^String model-name #^View view]
  (let [[[#^MetaModel metamodel #^ServiceFactory service-factory] mutable] (.state this)
	[exports] @mutable
	model (.getModel metamodel model-name)]
    (if (nil? model)
      (do (println "WARN: no Model for name:" model-name) nil) ;should throw Exception
      (let [[_ view]
	    (swap!
	     mutable
	     (fn [[exports]]
		 (let [[count view] (or (exports model-name) [0 (.client service-factory model-name)])]
		   [(assoc exports model-name [(inc count) view]) (if (zero? count) view)])))]
	(if view
	  (.registerView model view)
	  (Registration. (.getMetadata model) (.getData model) nil) ;TODO: HACK!
	  )))))

(defn #^Deregistration -deregisterView [#^org.dada.core.SessionManagerImpl this #^String model-name #^View view]
  (let [[[#^MetaModel metamodel #^ServiceFactory service-factory] mutable] (.state this)
	[exports] @mutable
	model (.getModel metamodel model-name)]
    (if (nil? model)
      (do (println "WARN: no Model for name:" model-name) nil) ;should throw Exception
      (let [[_ view]
	    (swap!
	     mutable
	     (fn [[exports]]
		 (let [entry (exports model-name)]
		   (if entry
		     (let [[count view] entry]
		       (if (= count 1)
			 [(dissoc exports model-name) view]
			 [(assoc exports model-name [(dec count) view])]))
		     [exports]))))]
	(if view
	  (.deregisterView model view)
	  (Deregistration. (.getData model) nil) ;TODO: HACK!
	  )))))
