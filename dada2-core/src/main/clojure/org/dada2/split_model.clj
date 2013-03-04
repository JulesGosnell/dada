(ns org.dada2.split-model
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 core map-model])
    (:import
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

;; if the key is already present, we have already inserted the model for this split
(defn ignore-upsertion? [old-state _ key _]
  (key old-state))

;; once a split is created we never remove it - TODO: reconsider
(defn- ignore-deletion? [_ _ _ _]
  true)

(defn- make-upserter [make-model]
  (fn [state key upsertion] (assoc state key (make-model key upsertion))))

(defn ^ModelView split-model [key-fn make-model-fn]
  (map-model key-fn pessimistic-on-change pessimistic-on-changes unversioned-pessimistic-ignore-upsertion? ignore-deletion? (make-upserter make-model-fn) nil))

;; need to test addition of submodels
;; need to notify submodel after change...

;;--------------------------------------------------------------------------------

;;; need metamodel
;; we need a select-model so we can view this remotely without pulling submodel content over the wire


;;; add new-datum-fn - call (new-datum-fn new-datum) to get new-datum
