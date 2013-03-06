(ns org.dada2.split-model
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 core map-model])
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
	  [old-state nil old-datum]
	  (let [new-view (change-fn key new-datum)]
	    [(applicator old-state key new-view) (fn [view] (notifier view new-view)) (fn [] (notifier new-view new-datum))]))
	;; 
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
	[new-state (if changes (fn [view] (notifier view changes)))]
	)))

(deftype SplitModelView [^Atom state on-upsert-fn on-delete-fn on-upserts-fn on-deletes-fn]
  Model
  (attach [this view] (add-watch state view (fn [view state old [new delta]] (if delta (delta view)))) this)
  (detach [this view] (remove-watch state view) this)
  (data [_] (first @state))
  View
  (on-upsert [this upsertion]
	     ;; TODO - keep notifications out of atom...
	     (let [[_ _ foo] (swap! state (fn [[current] upsertion] (on-upsert-fn current upsertion)) upsertion)]
	       ;; notify interested views
	       ;; HERE
	       (foo)
	     this))
  (on-delete [this deletion]
	     (swap! state (fn [[current] deletion] (on-delete-fn current deletion)) deletion)
	     this)
  (on-upserts [this upsertions]
	      (swap! state (fn [[current] upsertions] (on-upserts-fn current upsertions)) upsertions)
	      this)
  (on-deletes [this deletions]
	      (swap! state (fn [[current] deletions] (on-deletes-fn current deletions)) deletions)
	      this)
  )

(defn ^SplitModelView split-map-model
  [key-fn on-change on-changes ignore-upsertion? ignore-deletion? assoc-fn dissoc-fn]
  (->SplitModelView
   (atom [{}])
   ;; on-upsert
   (fn [old-state upsertion]
       (on-change old-state upsertion assoc-fn ignore-upsertion? on-upsert key-fn))
   ;; on-delete
   (fn [old-state deletion]
       (on-change old-state deletion dissoc-fn ignore-deletion? on-delete key-fn))
   ;; on-upserts
   (fn [old-state upsertions]
       (on-changes old-state upsertions assoc-fn ignore-upsertion? on-upserts key-fn))
   ;; on-deletes
   (fn [old-state deletions]
       (on-changes old-state deletions dissoc-fn ignore-deletion? on-deletes key-fn))
   ))

(defn ^SplitModelView split-model [key-fn make-model-fn]
  (split-map-model key-fn (make-on-change make-model-fn) (make-on-changes make-model-fn) nil nil assoc nil))

;; need to test addition of submodels
;; need to notify submodel after change...

;;--------------------------------------------------------------------------------

;;; need metamodel
;; we need a select-model so we can view this remotely without pulling submodel content over the wire


;;; add new-datum-fn - call (new-datum-fn new-datum) to get new-datum
