(ns org.dada.core.BaseModelView
    (:import
     [java.util Collection]
     [org.dada.core Metadata Registration Update View]
     )
    (:gen-class
     :implements [org.dada.core.ModelView]
     :constructors {[String org.dada.core.Metadata] []} ;name, metadata, ...
     :use BaseModelView
     :methods [[notifyUpdate [org.dada.core.ModelView java.util.Collection java.util.Collection java.util.Collection] Object]]
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
	   {}				;extant
	   {}				;extinct
	   '()				;views
	   ])]
   ])

;; (defn -getName [#^org.dada.core.BaseModelView this]
;;   (let [[[_name]] (.state this)]
;;     _name))

;; (defn -getMetadata [#^org.dada.core.BaseModelView this]
;;   (let [[[_ metadata]] (.state this)]
;;     metadata))

;; ;; (defn -getData [#^org.dada.core.BaseModelView this]
;; ;;   (let [[_ mutable] (.state this)
;; ;; 	[extant] @mutable]
;; ;;     (println "GET DATA ->" @mutable-state)
;; ;;     extant))

;; ;; Registration
;; (defn -registerView [#^org.dada.core.BaseModelView this #^View view]
;;   (let [[[_ metadata] mutable] (.state this)
;; 	[extant] @mutable]
;;     ;; N.B. does not check to see if View is already Registered
;;     (println "VIEW ->" @mutable)
;;     (swap! mutable (fn [state view] (assoc state 2 (cons view (state 2)))) view)
;;     (println "VIEW <-" @mutable)
;;     (Registration. metadata extant)
;;     )
;;   )

;; (defn without-first [collection element]
;;   ;; TODO - NYI
;;   collection)

;; ;; should return a Deregistration - currently Collection
;; (defn -deregisterView [#^org.dada.core.BaseModelView this #^View view]
;;   (let [[[_ _metadata] mutable] (.state this)
;; 	[extant extinct] @mutable]
;;     (println "UNVIEW ->" @mutable)
;;     (swap! mutable (fn [state view] (assoc state 2 (without-first (state 2) view))) view)
;;     (println "UNVIEW <-" @mutable)
;;     extant
;;     ))

;; (defn -notifyUpdate [#^org.dada.core.BaseModelView this insertions alterations deletions]
;;   (let [[_ mutable] (.state this)
;; 	[_ _ views] @mutable]
;;     (println "NOTIFY ->" @mutable)
;;     (if (and (empty? insertions) (empty? alterations) (empty? deletions))
;;       (println "WARN: empty event raised" (.getStackTrace (Exception.)))
;;       (doall (map (fn [#^View view]
;; 		      (try (.update view insertions alterations deletions) (catch Throwable t (println "ERROR: " t))))
;; 		  views)))))
