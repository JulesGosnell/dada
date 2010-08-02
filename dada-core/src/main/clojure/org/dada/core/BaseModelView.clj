(ns org.dada.core.BaseModelView
    (:use
     [org.dada.core counted-set]
     )
    (:import
     [java.util Collection]
     [org.dada.core Data Metadata Update View]
     )
    (:gen-class
     :implements [org.dada.core.ModelView]
     :constructors {[String org.dada.core.Metadata] []} ;name, metadata, ...
     :methods [[notifyUpdate [java.util.Collection java.util.Collection java.util.Collection] void]]
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
	   {}				;views
	   ])]
   ])

