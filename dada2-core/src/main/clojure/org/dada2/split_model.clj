(ns org.dada2.split-model
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 utils core map-model])
    (:import
     [clojure.lang Atom]
     [org.dada2.core ModelView])
    )

;; a model of key:view where:
;; key is the value returned by key-fn for a given datum
;; view is the value returned by view-fn for a new key and notified of the key's corresponding change
;; a real-time "group-by"


;;; key-fn can become split-key-fn - we don't need a key-fn - are they therefore one and the same ?
;; applicator needs to create new entry one the fly - contains new-model-fn
;; applicator needs to return new-state and new-datum (may not be same as change)
;; on-change and on-changes should also be merged and shared with map-model - hard - should we lose singleton api ?

(defn make-on-change [change-fn]
  (fn [old-state new-datum applicator _ notifier key-fn]
      (let [key (key-fn new-datum)
	    old-datum (key old-state)]
	(if old-datum
	  [old-state
	   (fn [views]
	       ;; no direct change to model, so no viewers to notify
	       ;; notify sub-model
	       (notifier old-datum new-datum)
	       )]
	  (let [sub-model (change-fn key new-datum)]
	    [(applicator old-state key sub-model)
	     (fn [views]
		 ;; notify viewers
		 (doseq [view views] (notifier view sub-model))
		 ;; notify sub-model
		 (notifier sub-model new-datum)
		 )]))
	)))

(defn- group [map key value]
  "add [key value] to map as in group-by"
  (assoc map key (conj (if-let [v (key map)] v []) value)))

(defn make-on-changes [change-fn]
  (fn [prev-state changes applicator _ notifier key-fn]
      (let [[next-state view-changes submodel-changes]
	    (reduce
	     (fn [[old-state old-view-changes old-submodel-changes] change]
		 ;; this fn is very similar to on-change above...
		 (let [key (key-fn change)
		       old-datum (key old-state)
		       new-submodel-changes (group old-submodel-changes key change)]
		   (if old-datum
		     [old-state old-view-changes new-submodel-changes]
		     (let [submodel (change-fn key change)
			   new-state (applicator old-state key submodel)
			   new-view-changes (conj old-view-changes submodel)]
		       [new-state new-view-changes new-submodel-changes]))))
	     [prev-state [] {}]
	     changes)]
	[next-state 
	 (fn [views]
	     (doseq [view views] (on-upserts view view-changes)) ;; notify views
	     (doseq [[k v] submodel-changes] (notifier (k next-state) v)) ;; notify sub-models..
	     )]
	)))

(defn- without [coll item] (remove (fn [i] (identical? item i)) coll))

(defn ^ModelView split-model [name key-fn make-model-fn]
  (map-model name key-fn (make-on-change make-model-fn) (make-on-changes make-model-fn) nil nil assoc nil))

;; need to test addition of submodels
;; need to notify submodel after change...

;;--------------------------------------------------------------------------------

;;; need metamodel
;; we need a select-model so we can view this remotely without pulling submodel content over the wire


;;; add new-datum-fn - call (new-datum-fn new-datum) to get new-datum
