(ns
 org.dada.core.SessionManagerImpl
 ;; (:use
 ;;  [clojure.contrib with-ns])
 (:import
  [java.util Collection]
  [org.dada.core Data Metadata MetaModel Model ServiceFactory View]
  )
 (:gen-class
  :implements [org.dada.core.SessionManager]
  :constructors {[String org.dada.core.MetaModel org.dada.core.ServiceFactory] []}
  :methods []
  :init init
  :state state
  )
 )

(defn -init [#^String name #^MetaModel metamodel #^ServiceFactory service-factory]
  [ ;; super ctor args
   []
   ;; instance state
   (let [exports {}]
     [[name metamodel service-factory]
      (atom [exports])])])

(defn #^String -getName [#^org.dada.core.SessionManagerImpl this]
  (let [[[name #^MetaModel metamodel]] (.state this)]
    name))

(defn #^Model -getModel [#^org.dada.core.SessionManagerImpl this #^String name]
  (let [[[_ #^MetaModel metamodel]] (.state this)]
    (.getModel metamodel name)))

(defn #^Metadata -getMetadata [#^org.dada.core.SessionManagerImpl this #^String name]
  (let [[[_ #^MetaModel metamodel]] (.state this)]
    (.getMetadata (.getModel metamodel name))))

;; TODO: modification of our data model should be encapsulated in a fn
;; and handed of to Model so that it is all done within the scope of
;; the Model's single spin lock. This will resolve possible race and
;; ordering issues. Fn will probably need to implement a Strategy i/f
;; so that it plays ball with Java API

;; TODO: I think we will need multiple SessionManagers so that one can
;; speak Serialised POJOs, one XML etc...

(defn #^Data -registerView [#^org.dada.core.SessionManagerImpl this #^String model-name #^View view]
  (let [[[_ #^MetaModel metamodel #^ServiceFactory service-factory] mutable] (.state this)
	model (.getModel metamodel model-name)]
    (if (nil? model)
      (do (println "WARN: no Model for name:" model-name) nil) ;should throw Exception
      (let [[_ view]
	    (swap!
	     mutable
	     (fn [[exports]]
		 (let [[count view] (or (exports model-name) [0 view])]
		   [(assoc exports model-name [(inc count) view]) (if (zero? count) view)])))]
	(if view
	  (.registerView model view)
	  (.getData model)
	  )))))

(defn #^Data -deregisterView [#^org.dada.core.SessionManagerImpl this #^String model-name #^View view]
  (let [[[_ #^MetaModel metamodel #^ServiceFactory service-factory] mutable] (.state this)
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
	  (.getData model)
	  )))))

;; need contrib with-ns - only contrib dependency so far...
;; (defmacro with-temp-ns-inherited [ns & body]
;;   `(with-temp-ns (use (quote ~ns)) ~@body))

;; TODO: consider memoisation of queries, unamiguous DSL, model names, etc...
;; TODO: security - arbitrary code can be evaluated here...
(defn #^Collection -query[#^org.dada.core.SessionManagerImpl this namespace-name query-string]
  (println "QUERY:" query-string)
  (use (symbol namespace-name))		;TODO - query should be evaluated in temporary namespace
  (let [;;target-namespace (symbol namespace-name) ;TODO - should be part of SessionManager's state
	[[_ target-metamodel #^ServiceFactory service-factory] mutable] (.state this)
	;; TODO: involve target-metamodel - how can we thread this through the chain ?
	;;[_ data-fn] (with-temp-ns-inherited target-namespace (eval (read-string query-string)))
	[_ data-fn] (let [query (read-string query-string)] (prn "EVALUATING:" query) (eval query))
	[#^Model results-model] (data-fn)]
    ;;(.getName results-model)
    (map (fn [[#^Model m]] (.getName m)) (.getExtant (.getData results-model))))) ;TODO: security - arbitrary code can be evaluated here...
