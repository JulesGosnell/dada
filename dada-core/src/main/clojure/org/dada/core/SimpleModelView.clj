(ns org.dada.core.SimpleModelView
    (:use
     [org.dada.core
      counted-set]
     )
    (:require
     [org.dada.core
      ;; BaseModelView
      ]
     )
    (:import
     [java.util Collection]
     [org.dada.core Metadata Update]
     )
    (:gen-class
  ;;   :extends org.dada.core.BaseModelView
     :implements [org.dada.core.ModelView]
     :constructors {[String org.dada.core.Metadata] ;;[String org.dada.core.Metadata]
     []}
     :methods []
     :init init
     :state state
     )
    )

;;--------------------------------------------------------------------------------
;; copied forward from BaseModelView to see if it fixes problem...

(defn -getName [#^org.dada.core.SimpleModelView this]
  (let [[[_name]] (.state this)]
    _name))

(defn -getMetadata [#^org.dada.core.SimpleModelView this]
  (let [[[_ metadata]] (.state this)]
    metadata))

;; (defn -getData [#^org.dada.core.SimpleModelView this]
;;   (let [[_ mutable] (.state this)
;; 	[extant] @mutable]
;;     (println "GET DATA ->" @mutable-state)
;;     extant))

(import org.dada.core.Deregistration)
(import org.dada.core.Registration)
(import org.dada.core.View)

;; Registration
(defn -registerView [#^org.dada.core.SimpleModelView this #^View view]
  (let [[[_ metadata] mutable] (.state this)
	[extant extinct] @mutable]
    ;; N.B. does not check to see if View is already Registered
    ;;(println "VIEW ->" @mutable)
    (swap! mutable (fn [state view] (assoc state 2 (counted-set-inc (state 2) view))) view)
    ;;(println "VIEW <-" @mutable)
    (Registration. metadata (vals extant) (vals extinct))
    )
  )

;; should return a Deregistration - currently Collection
(defn -deregisterView [#^org.dada.core.SimpleModelView this #^View view]
  (let [[[_ _metadata] mutable] (.state this)
	[extant extinct] @mutable]
    ;;(println "UNVIEW ->" @mutable)
    (swap! mutable (fn [state view] (assoc state 2 (counted-set-dec (state 2) view))) view)
    ;;(println "UNVIEW <-" @mutable)
    (Deregistration. (vals extant) (vals extinct))
    ))

(defn -notifyUpdate [#^org.dada.core.SimpleModelView this insertions alterations deletions]
  (let [[_ mutable] (.state this)
	[_ _ views] @mutable]
    ;;(println "NOTIFY ->" @mutable)
    (if (and (empty? insertions) (empty? alterations) (empty? deletions))
      (println "WARN: empty event raised" (.getStackTrace (Exception.)))
      (doall (map (fn [#^View view]
		      (try (.update view insertions alterations deletions) (catch Throwable t (println "ERROR: " t))))
		  (counted-set-vals views))))))

;;--------------------------------------------------------------------------------

;; TODO: consider supporting indexing on mutable keys - probably not a good idea ?

(defn -init [#^String name #^Metadata metadata]

  [ ;; super ctor args
   ;;[name metadata]
   []
   ;; instance state
   (let [key-getter (.getPrimaryGetter metadata)
	 version-comparator (.getVersionComparator metadata)
	 key-fn (fn [value] (.get key-getter value))

	 process-addition
	 (fn [[extant extinct views i a d] #^Update addition]
	     (let [new (.getNewValue addition)
		   key (key-fn new)
		   current (extant key)]
	       (if (nil? current)
		 ;; insertion...
		 (let [removed (extinct key)]
		   (if (nil? removed)
		     ;; first time seen
		     [(assoc extant key new) extinct views (cons (Update. nil new) i) a d] ;insertion
		     ;; already deleted
		     (if (.higher version-comparator removed new)
		       ;; later version - reinstated
		       [(assoc extant key new) (dissoc extinct key) views (cons (Update. nil new) i) a d]
		       (do
			 ;; out of order or duplicate version - ignored
			 ;;(println "WARN: OUT OF ORDER INSERT" current new)
			 [extant extinct views i a d]))
		     )
		   )
		 ;; alteration...
		 (if (.higher version-comparator current new)
		   ;; later version - accepted
		   [(assoc extant key new) extinct views i (cons (Update. current new) a) d] ;alteration
		   (do
		     ;; out of order or duplicate version - ignored
		     ;;(println "WARN: OUT OF ORDER UPDATE" current new)
		     [extant extinct views i a d]))
		 )))

	 process-deletion
	 (fn [[extant extinct views i a d] #^Update deletion]
	     (let [new (.getNewValue deletion)
		   key (key-fn new)
		   current (extant key)]
	       (if (nil? current)
		 (let [removed (extinct key)]
		   (if (nil? removed)
		     ;; neither extant or extinct - mark extinct
		     [extant (dissoc extinct key) views i a d]
		     (if (.higher version-comparator removed new)
		       ;; later version - accepted
		       [extant (assoc extinct key new) views i a (cons (Update. removed new) d)]
		       (do
			 ;; earlier version - ignored
			 ;;(println "WARN: OUT OF ORDER DELETION" current new)
			 [extant extinct views i a d]))))
		 (if (.higher version-comparator current new)
		   ;; later version - accepted
		   [(dissoc extant key) (assoc extinct key new) views i a (cons (Update. current new) d)]
		   (do
		     ;; earlier version - ignored
		     ;;(println "WARN: OUT OF ORDER DELETION" current new)
		     [extant extinct views i a d])))))

	 ;; TODO: perhaps we should raise the granularity at which we
	 ;; compare-and-swap, in order to avoid starvation of larger
	 ;; batches...
	 swap-state-fn (fn [[extant extinct views] insertions alterations deletions]
			   (reduce process-deletion
				   (reduce process-addition
					   (reduce process-addition
						   [extant extinct views '() '() '()]
						   insertions)
					   alterations)
				   deletions))

	 mutable-state (atom [{} {} {}]) ;extant, extinct, views

	 update-fn
	 (fn [inputs]
	     ;;(println "UPDATE ->" @mutable-state)
	     (let [[_ _ _ i a d] (apply swap! mutable-state swap-state-fn inputs)]
	       ;;(println "UPDATE <-" @mutable-state)
	       [i a d]))
	 
	 getData-fn
	 (fn []
	     ;;(println "GET DATA ->" @mutable-state)
	     (let [[extant] @mutable-state]
	       (or (vals extant) '())))
	 ]
     
     [[
       name
       metadata
       update-fn
       getData-fn
       ]
      mutable-state])
   ])

(defn -getData [#^org.dada.core.SimpleModelView this]
  (let [[[_ _ _ getData-fn]] (.state this)]
    (getData-fn)))

(defn -update [#^org.dada.core.SimpleModelView this & inputs]
  (let [[[_ _ update-fn] mutable] (.state this)
	[_ _ views] @mutable
	[#^Collection i #^Collection a #^Collection d] (update-fn inputs)]
    (if (not (and (empty? i) (empty? a) (empty? d)))
      (-notifyUpdate this i a d))
    ))

