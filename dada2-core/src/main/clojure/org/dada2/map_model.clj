(ns org.dada2.map-model
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 core])
    (:import
     [org.dada2.core ModelView])
    )

;; a versioned hashmap model only accepts changes in which the version
;; is more recent than the one on the entry already held.

;;(on-change old-state upsertion assoc ignore-upsertion? on-upsert key-fn more-recent-than? version-fn)
;;(on-change old-state deletion (fn [m k v] (dissoc m k)) ignore-upsertion? on-upsert key-fn more-recent-than? version-fn)

;; optimistic -  most of your updates will result in a change - saves a dehash
;; pessimistic - most of your updates won't result in a change -saves an alloc (change)

;; OPTIMISTIC -  1 alloc / 1 dehash 
(defn- optimistic-on-change [old-state change applicator ignore? notifier key-fn more-recent-than? version-fn]
  ;; apply the change optimistically. A second dehash will only
  ;; be needed on an update or successful deletion, but not on insertion...
  (let [key (key-fn change)
	new-state (applicator old-state key change)]
    (if (ignore? old-state new-state more-recent-than? version-fn key change)
      [old-state nil]
      [new-state (fn [view] (notifier view change))])))

;; PESSIMISTIC - 2 dehashes / maybe 1 alloc
(defn pessimistic-on-change [old-state new-datum applicator ignore? notifier key-fn more-recent-than? version-fn]
  (let [key (key-fn new-datum)]
    (if (ignore? old-state nil more-recent-than? version-fn key new-datum)
      [old-state nil]
      [(applicator old-state key new-datum) (fn [view] (notifier view new-datum))])))

;; optimistic
(defn- optimistic-on-changes [old-state changes applicator ignore? notifier key-fn more-recent-than? version-fn]
  (let [[new-state changes]
	(reduce
	 (fn [[old-state changes] change]
	     ;; this fn is very similar to on-change above...
	     (let [key (key-fn change)
		   new-state (applicator old-state key change)]
	       (if (ignore? old-state new-state more-recent-than? version-fn key change)
		 [old-state changes]
		 [new-state (conj changes change)])))
	 [old-state []]
	 changes)]
    [new-state (if changes (fn [view] (notifier view changes)))]
    ))

;;; pessimistic
(defn pessimistic-on-changes [old-state changes applicator ignore? notifier key-fn more-recent-than? version-fn]
  (let [[new-state changes]
	(reduce
	 (fn [[old-state changes] change]
	     ;; this fn is very similar to on-change above...
	     (let [key (key-fn change)]
	       (if (ignore? old-state nil more-recent-than? version-fn key change)
		 [old-state changes]
		 [(applicator old-state key change)
		  ;;; TODO
		  ;;; inject new fn here 
		  ;; or extend applicator to return both new state and changes to be applied...
		  (conj changes change)
		  ])))
	 [old-state []]
	 changes)]
    [new-state (if changes (fn [view] (notifier view changes)))]
    ))

;;--------------------------------------------------------------------------------
;; ignore function makers

;; optimistic
(defn- optimistic-ignore-upsertion? [old-state new-state more-recent-than? version-fn key upsertion]
  (or (identical? old-state new-state)
      (and (= (count old-state) (count new-state))
	   (not (more-recent-than? (version-fn upsertion) (version-fn (old-state key)))))))

(defn- unversioned-optimistic-ignore-upsertion? [old-state new-state _ _ key upsertion]
  (identical? old-state new-state))

;; pessimistic
(defn pessimistic-ignore-upsertion? [old-state new-state more-recent-than? version-fn key new-datum]
  (let [old-datum (key old-state)]
    (and old-datum (not (more-recent-than? (version-fn new-datum) (version-fn old-datum))))))

;; unversioned-pessimistic
(defn unversioned-pessimistic-ignore-upsertion? [old-state new-state _ _ key new-datum]
  (= (key old-state) new-datum))

;; optimistic
(defn- optimistic-ignore-deletion? [old-state new-state more-recent-than? version-fn key deletion]
  (or (identical? old-state new-state)
      (more-recent-than? (version-fn (old-state key)) (version-fn deletion))))

(defn- unversioned-optimistic-ignore-deletion? [old-state new-state _ _ key deletion]
  (identical? old-state new-state))

;; pessimistic
(defn pessimistic-ignore-deletion? [old-state new-state more-recent-than? version-fn key new-datum]
  (let [old-datum (key old-state)]
    (and old-datum (more-recent-than? (version-fn old-datum) (version-fn new-datum)))))

;; unversioned-pessimistic
(defn unversioned-pessimistic-ignore-deletion? [old-state new-state _ _ key new-datum]
  (not (key old-state)))

(defn dissoc-deletion [m k v] (dissoc m k))

(defn ^ModelView map-model [key-fn version-fn more-recent-than? on-change on-changes ignore-upsertion? ignore-deletion? assoc-fn dissoc-fn]
  (->ModelView
   (atom [{}])
   ;; on-upsert
   (fn [old-state upsertion]
       (on-change old-state upsertion assoc-fn ignore-upsertion? on-upsert key-fn more-recent-than? version-fn))
   ;; on-delete
   (fn [old-state deletion]
       (on-change old-state deletion dissoc-fn ignore-deletion? on-delete key-fn more-recent-than? version-fn))
   ;; on-upserts
   (fn [old-state upsertions]
       (on-changes old-state upsertions assoc-fn ignore-upsertion? on-upserts key-fn more-recent-than? version-fn))
   ;; on-deletes
   (fn [old-state deletions]
       (on-changes old-state deletions dissoc-fn ignore-deletion? on-deletes key-fn more-recent-than? version-fn))
   ))

;; (more-recent-than? newer older)
(defn ^ModelView unversioned-optimistic-map-model [key-fn]
  (map-model key-fn nil nil optimistic-on-change optimistic-on-changes unversioned-optimistic-ignore-upsertion? unversioned-optimistic-ignore-deletion? assoc dissoc-deletion))

(defn ^ModelView optimistic-map-model [key-fn version-fn more-recent-than?]
  (map-model key-fn version-fn more-recent-than? optimistic-on-change optimistic-on-changes optimistic-ignore-upsertion? optimistic-ignore-deletion? assoc dissoc-deletion))

(defn ^ModelView unversioned-pessimistic-map-model [key-fn]
  (map-model key-fn nil nil pessimistic-on-change pessimistic-on-changes unversioned-pessimistic-ignore-upsertion? unversioned-pessimistic-ignore-deletion? assoc dissoc-deletion))

(defn ^ModelView pessimistic-map-model [key-fn version-fn more-recent-than?]
  (map-model key-fn version-fn more-recent-than? pessimistic-on-change pessimistic-on-changes pessimistic-ignore-upsertion? pessimistic-ignore-deletion? assoc dissoc-deletion))

;; models should have names
