(ns org.dada.core.AbstractModelView2
    (:import
     [java.util Collection]
     [org.dada.core Metadata Registration Update View]
     )
    (:gen-class
     :implements [org.dada.core.ModelView]
     :constructors {[String org.dada.core.Metadata] []} ;name, metadata, ...
     :use AbstractModelView2
     :methods []
     :init init
     :state state
     )
    )

;; TODO: consider supporting indexing on mutable keys - probably not a good idea ?

(defn -init [#^String _name #^Metadata _metadata]
  [ ;; super ctor args
   []
   ;; instance state
   ;; immutable...
   [[_name
     _metadata]
    ;; mutable...
    (atom [
	   nil				;extant
	   nil				;extinct
	   nil				;views
	   ])]
   ])

(defn -getName [#^org.dada.core.AbstractModelView2 this]
  (let [[[_name]] (.state this)]
    _name))

(defn -getData [#^org.dada.core.AbstractModelView2 this]
  (let [[_ mutable] (.state this)
	[extant] @mutable]
    extant))

;; Registration
(defn -registerView [#^org.dada.core.AbstractModelView2 this #^View view]
  (let [[[_ metadata] mutable] (.state this)
	[extant extinct] @mutable]
    ;; N.B. does not check to see if View is already Registered
    (swap! mutable (fn [state view] (assoc state 2 (cons view (state 2)))) view)
    (Registration. metadata extant)
    )
  )

(defn without-first [collection element]
  ;; TODO - NYI
  collection)

;; should return a Deregistration - currently Collection
(defn -deregisterView [#^org.dada.core.AbstractModelView2 this #^View view]
  (let [[[_ _metadata] mutable] (.state this)
	[extant extinct] @mutable]
    (swap! mutable (fn [state view] (assoc state 2 (without-first (state 2) view))) view)
    extant
    ))

(defn -notifyUpdate [#^org.dada.core.AbstractModelView2 this insertions alterations deletions]
  (let [[_ mutable] (.state this)
	[_ _ views] @mutable]
    (if (and (empty? insertions) (empty? alterations) (empty? deletions))
      (println "WARN: empty event raised" (.getStackTrace (Exception.)))
      (doall (map (fn [#^View view]
		      (try (.update view insertions alterations deletions) (catch Throwable t (println "ERROR: " t))))
		  views)))))
