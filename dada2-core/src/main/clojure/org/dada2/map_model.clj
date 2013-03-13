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
(defn- optimistic-on-change [old-state change applicator ignore? notifier key-fn]
  ;; apply the change optimistically. A second dehash will only
  ;; be needed on an update or successful deletion, but not on insertion...
  (let [key (key-fn change)
	new-state (applicator old-state key change)]
    (if (ignore? old-state new-state key change)
      [old-state]
      [new-state (fn [views] (doseq [view views] (notifier view change)))])))

;; PESSIMISTIC - 2 dehashes / maybe 1 alloc
(defn- pessimistic-on-change [old-state new-datum applicator ignore? notifier key-fn]
  (let [key (key-fn new-datum)]
    (if (ignore? old-state nil key new-datum)
      [old-state]
      [(applicator old-state key new-datum) (fn [views] (doseq [view views] (notifier view new-datum)))])))

;; optimistic
(defn- optimistic-on-changes [old-state changes applicator ignore? notifier key-fn]
  (let [[new-state changes]
	(reduce
	 (fn [[old-state changes] change]
	     ;; this fn is very similar to on-change above...
	     (let [key (key-fn change)
		   new-state (applicator old-state key change)]
	       (if (ignore? old-state new-state key change)
		 [old-state changes]
		 [new-state (conj changes change)])))
	 [old-state []]
	 changes)]
    [new-state (if changes (fn [views] (doseq [view views] (notifier view changes))))]
    ))

;;; pessimistic
(defn- pessimistic-on-changes [old-state changes applicator ignore? notifier key-fn]
  (let [[new-state changes]
	(reduce
	 (fn [[old-state changes] change]
	     ;; this fn is very similar to on-change above...
	     (let [key (key-fn change)]
	       (if (ignore? old-state nil key change)
		 [old-state changes]
		 [(applicator old-state key change)
		  ;;; TODO
		  ;;; inject new fn here 
		  ;; or extend applicator to return both new state and changes to be applied...
		  (conj changes change)
		  ])))
	 [old-state []]
	 changes)]
    [new-state (if changes (fn [views] (doseq [view views] (notifier view changes))))]
    ))

;;--------------------------------------------------------------------------------
;; ignore function makers

;; optimistic
(defn- make-versioned-optimistic-ignore-upsertion? [version-fn more-recent-than?]
  (fn [old-state new-state key upsertion]
      (or (identical? old-state new-state)
	  (and (= (count old-state) (count new-state))
	       (not (more-recent-than? (version-fn upsertion) (version-fn (old-state key))))))))

(defn- unversioned-optimistic-ignore-upsertion? [old-state new-state key upsertion]
  (identical? old-state new-state))

;; pessimistic
(defn make-versioned-pessimistic-ignore-upsertion? [version-fn more-recent-than?]
  (fn [old-state new-state key new-datum]
      (let [old-datum (key old-state)]
	(and old-datum (not (more-recent-than? (version-fn new-datum) (version-fn old-datum)))))))

;; unversioned-pessimistic
(defn- unversioned-pessimistic-ignore-upsertion? [old-state new-state key new-datum]
  (= (key old-state) new-datum))

;; optimistic
(defn- make-versioned-optimistic-ignore-deletion? [version-fn more-recent-than?]
  (fn [old-state new-state key deletion]
      (or (identical? old-state new-state)
	  (more-recent-than? (version-fn (old-state key)) (version-fn deletion)))))

(defn- unversioned-optimistic-ignore-deletion? [old-state new-state key deletion]
  (identical? old-state new-state))

;; pessimistic
(defn make-versioned-pessimistic-ignore-deletion? [version-fn more-recent-than?]
  (fn [old-state new-state key new-datum]
      (let [old-datum (key old-state)]
	(and old-datum (more-recent-than? (version-fn old-datum) (version-fn new-datum))))))

;; unversioned-pessimistic
(defn unversioned-pessimistic-ignore-deletion? [old-state new-state key new-datum]
  (not (key old-state)))

(defn dissoc-deletion [m k v] (dissoc m k))

(defn ^ModelView map-model
  [name key-fn on-change on-changes ignore-upsertion? ignore-deletion? assoc-fn dissoc-fn]
  (->ModelView
   name
   (atom {})
   (atom [])
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

;; (more-recent-than? newer older)
(defn ^ModelView unversioned-optimistic-map-model [name key-fn]
  (map-model name key-fn optimistic-on-change optimistic-on-changes unversioned-optimistic-ignore-upsertion? unversioned-optimistic-ignore-deletion? assoc dissoc-deletion))

(defn ^ModelView versioned-optimistic-map-model [name key-fn version-fn more-recent-than?]
  (let [ignore-upsertion? (make-versioned-optimistic-ignore-upsertion? version-fn more-recent-than?)
	ignore-deletion? (make-versioned-optimistic-ignore-deletion? version-fn more-recent-than?)]
    (map-model name key-fn optimistic-on-change optimistic-on-changes ignore-upsertion? ignore-deletion? assoc dissoc-deletion)))

(defn ^ModelView unversioned-pessimistic-map-model [name key-fn]
  (map-model name key-fn pessimistic-on-change pessimistic-on-changes unversioned-pessimistic-ignore-upsertion? unversioned-pessimistic-ignore-deletion? assoc dissoc-deletion))

(defn ^ModelView versioned-pessimistic-map-model [name key-fn version-fn more-recent-than?]
  (let [ignore-upsertion? (make-versioned-pessimistic-ignore-upsertion? version-fn more-recent-than?)
	ignore-deletion? (make-versioned-pessimistic-ignore-deletion? version-fn more-recent-than?)]
  (map-model name key-fn pessimistic-on-change pessimistic-on-changes ignore-upsertion? ignore-deletion? assoc dissoc-deletion)))

;; models should have names
