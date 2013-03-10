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
;; on-change and on-changes should also be merged and shhared with map-model - hard - should we lose singleton api ?

(defn make-on-change [change-fn]
  (fn [old-state new-datum applicator _ notifier key-fn]
      (let [key (key-fn new-datum)
	    ;; HERE
	    old-datum (key old-state)]
	(if old-datum
	  ;; HERE
	  [old-state]
	  (let [sub-model (change-fn key new-datum)]
	    [(applicator old-state key sub-model)
	     (fn [views]
		 ;; notify viewers
		 (doseq [view views] (notifier view sub-model))
		 ;; notify sub-model
		 (notifier sub-model new-datum)
		 )]))
	)))

(defn make-on-changes [change-fn]
  (fn [old-state changes applicator _ notifier key-fn]
      (let [[new-state changes]
	    (reduce
	     (fn [[old-state changes] change]
		 ;; this fn is very similar to on-change above...
		 (let [key (key-fn change)
		       ;; HERE
		       old-datum (key old-state)]
		   (if old-datum
		     [old-state changes]
		     (let [change (change-fn key change)]
		       [(applicator old-state key change)
			(conj changes change)
			]))))
	     [old-state []]
	     changes)]
	[new-state (if changes (fn [views] (doseq [view views] (notifier view changes))))]
	)))

(defn- without [coll item] (remove (fn [i] (identical? item i)) coll))

(defn ^ModelView split-model [key-fn make-model-fn]
  (map-model key-fn (make-on-change make-model-fn) (make-on-changes make-model-fn) nil nil assoc nil))

;; need to test addition of submodels
;; need to notify submodel after change...

;;--------------------------------------------------------------------------------

;;; need metamodel
;; we need a select-model so we can view this remotely without pulling submodel content over the wire


;;; add new-datum-fn - call (new-datum-fn new-datum) to get new-datum
