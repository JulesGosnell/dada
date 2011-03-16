(ns org.dada.core.SimpleModelView
  (:use
   [clojure.contrib logging]
   [org.dada.core counted-set]
   )
  (:import
   [java.util Collection]
   [org.dada.core Data Metadata RemoteModel Update View]
   )
  (:gen-class
   :implements [org.dada.core.ModelView java.io.Serializable]
   :constructors {[String org.dada.core.Metadata] []}
   :methods [[writeReplace [] Object]]
   :init init
   :state state
   )
  )

;; TODO: consider supporting indexing on mutable keys - probably not a good idea ?

(defn -init [#^String name #^Metadata metadata]

  [ ;; super ctor args
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
		     (if (< (.compareTo version-comparator removed new) 0)
		       ;; later version - reinstated
		       [(assoc extant key new) (dissoc extinct key) views (cons (Update. nil new) i) a d]
		       (do
			 ;; out of order or duplicate version - ignored
			 (debug ["out of order insertion" current new])
			 [extant extinct views i a d]))
		     )
		   )
		 ;; alteration...
		 (if (or (not version-comparator)(< (.compareTo version-comparator current new) 0))
		   ;; later version - accepted
		   [(assoc extant key new) extinct views i (cons (Update. current new) a) d] ;alteration
		   (do
		     ;; out of order or duplicate version - ignored
		     (debug ["out of order update" current new])
		     [extant extinct views i a d]))
		 )))

	 process-deletion
	 (fn [[extant extinct views i a d] #^Update deletion]
	     (let [old (.getOldValue deletion)
		   new (.getNewValue deletion)
		   key (key-fn old)
		   current (extant key)
		   latest (or new old)]
	       (if (nil? current)
		 (let [removed (extinct key)]
		   (if (nil? removed)
		     ;; neither extant or extinct - mark extinct
		     ;; we will remember the id in case we get an out of order insertion later
		     [extant (assoc extinct key latest) views i a (conj d (Update. nil new))]
		     (if (< (.compareTo version-comparator removed latest) 0)
		       ;; later version - accepted
		       [extant (assoc extinct key new) views i a (cons (Update. removed new) d)]
		       (do
			 ;; earlier version - ignored
			 (debug ["out of order deletion - ignored" removed latest])
			 [extant extinct views i a d]))))
		 (if (<= (.compareTo version-comparator current old) 0)
		   ;; deletion of current or later version - accepted
		   [(dissoc extant key) (assoc extinct key latest) views i a (cons (Update. current new) d)]
		   (do
		     ;; earlier version - ignored
		     (debug ["out of order deletion - ignored" current new])
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
	     (trace ["UPDATE ->" @mutable-state])
	     (let [[_ _ _ i a d] (apply swap! mutable-state swap-state-fn inputs)]
	       (trace ["UPDATE <-" @mutable-state])
	       [i a d]))
	 
	 getData-fn
	 (fn []
	     (let [[extant extinct] @mutable-state]
	       (Data. (vals extant) (vals extinct))))
	 ]
     
     [[
       name
       metadata
       update-fn
       getData-fn
       ]
      mutable-state])
   ])
;;--------------------------------------------------------------------------------

(defn -getName [#^org.dada.core.SimpleModelView this]
  (let [[[_name]] (.state this)]
    _name))

(defn -getMetadata [#^org.dada.core.SimpleModelView this]
  (let [[[_ metadata]] (.state this)]
    metadata))

(defn -find [#^org.dada.core.SimpleModelView this key]
  (let [[_ mutable] (.state this)
	[extant] @mutable]
    (extant key)))

(defn #^Data -registerView [#^org.dada.core.SimpleModelView this #^View view]
  (let [[_ mutable] (.state this)]
    ;; N.B. does not check to see if View is already Registered
    (debug ["REGISTER VIEW   ->" view (@mutable 2)])
    (let [[extant extinct views] (swap! mutable (fn [state view] (assoc state 2 (counted-set-inc (state 2) view))) view)]
      (debug ["REGISTER VIEW   <-" views])
      (Data. (vals extant) (vals extinct)))))

(defn #^Data -deregisterView [#^org.dada.core.SimpleModelView this #^View view]
  (let [[_ mutable] (.state this)]
    (debug ["DEREGISTER VIEW ->" view (@mutable 2)])
    (let [[extant extinct views] (swap! mutable (fn [state view] (assoc state 2 (counted-set-dec (state 2) view))) view)]
      (debug ["DEREGISTER VIEW <-" views])
      (Data. (vals extant) (vals extinct)))))

(defn -notifyUpdate [#^org.dada.core.SimpleModelView this insertions alterations deletions]
  (let [[_ mutable] (.state this)
	[_ _ views] @mutable]
    (trace ["NOTIFY ->" views])
    (if (and (empty? insertions) (empty? alterations) (empty? deletions))
      (warn "empty event raised" (.getStackTrace (Exception.)))
      (dorun (map (fn [#^View view]	;dirty - side-effects
		      (try (.update view insertions alterations deletions)
			   (catch Throwable t
				  (error "View notification failure" t)
				  (.printStackTrace t))))
		  (counted-set-vals views))))))

;;--------------------------------------------------------------------------------

(defn -getData [#^org.dada.core.SimpleModelView this]
  (let [[[_ _ _ getData-fn]] (.state this)]
    (getData-fn)))

(defn -update [#^org.dada.core.SimpleModelView this & inputs]
  (let [[[_ _ update-fn] mutable] (.state this)
	[#^Collection i #^Collection a #^Collection d] (update-fn inputs)]
    (if (not (and (empty? i) (empty? a) (empty? d)))
      (-notifyUpdate this i a d))
    ))

;;--------------------------------------------------------------------------------

(defn #^{:private true} -writeReplace [#^org.dada.core.SimpleModelView this]
  (let [[[name metadata]] (.state this)]
      (RemoteModel. name metadata)))

(defn #^String -toString [#^org.dada.core.SimpleModelView this]
  (let [[[name metadata]] (.state this)]
    name))
