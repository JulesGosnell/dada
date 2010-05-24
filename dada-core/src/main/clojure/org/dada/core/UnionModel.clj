(ns org.dada.core.UnionModel
    (:import
     [java.util Collection]
     [org.dada.core AbstractModel Metadata Update]
     )
    (:gen-class
     :extends org.dada.core.AbstractModelView
     :constructors {[String org.dada.core.Metadata clojure.lang.IFn] [String org.dada.core.Metadata]}
     :methods []
     :init init
     :state state
     )
    )

;; TODO: consider supporting indexing on mutable keys - probably not a good idea ?

(defn -init [#^String model-name #^Metadata tgt-metadata #^IFn version-fn]

  [ ;; super ctor args
   [model-name tgt-metadata]
   ;; instance state
   (let [key-getter (.getKeyGetter tgt-metadata)
	 key-fn (fn [value] (.get key-getter value))

	 process-addition
	 (fn [[extant extinct i a d] #^Update addition]
	     (let [new (.getNewValue addition)
		   key (key-fn new)
		   current (extant key)]
	       (if (nil? current)
		 ;; insertion...
		 (let [removed (extinct key)]
		   (if (nil? removed)
		     ;; first time seen
		     [(assoc extant key new) extinct (cons (Update. nil new) i) a d] ;insertion
		     ;; already deleted
		     (if (version-fn removed new)
		       ;; later version - reinstated
		       [(assoc extant key new) (dissoc extinct key) (cons (Update. nil new) i) a d]
		       (do
			 ;; out of order or duplicate version - ignored
			 ;;(println "WARN: OUT OF ORDER INSERT" current new)
			 [extant extinct i a d]))
		     )
		   )
		 ;; alteration...
		 (if (version-fn current new)
		   ;; later version - accepted
		   [(assoc extant key new) extinct i (cons (Update. current new) a) d] ;alteration
		   (do
		     ;; out of order or duplicate version - ignored
		     ;;(println "WARN: OUT OF ORDER UPDATE" current new)
		     [extant extinct i a d]))
		 )))

	 process-deletion
	 (fn [[extant extinct i a d] #^Update deletion]
	     (let [new (.getNewValue deletion)
		   key (key-fn new)
		   current (extant key)]
	       (if (nil? current)
		 (let [removed (extinct key)]
		   (if (nil? removed)
		     ;; neither extant or extinct - mark extinct
		     [extant (dissoc extinct key) i a d]
		     (if (version-fn removed new)
		       ;; later version - accepted
		       [extant (assoc extinct key new) i a (cons (Update. removed new) d)]
		       (do
			 ;; earlier version - ignored
			 ;;(println "WARN: OUT OF ORDER DELETION" current new)
			 [extant extinct i a d]))))
		 (if (version-fn current new)
		   ;; later version - accepted
		   [(dissoc extant key) (assoc extinct key new) i a (cons (Update. current new) d)]
		   (do
		     ;; earlier version - ignored
		     ;;(println "WARN: OUT OF ORDER DELETION" current new)
		     [extant extinct i a d])))))

	 ;; TODO: perhaps we should raise the granularity at which we
	 ;; compare-and-swap, in order to avoid starvation of larger
	 ;; batches...
	 swap-state-fn (fn [[extant extinct] insertions alterations deletions]
			   (reduce process-deletion
				   (reduce process-addition
					   (reduce process-addition
						   [extant extinct '() '() '()]
						   insertions)
					   alterations)
				   deletions))

	 mutable-state (atom [{}{}])

	 update-fn
	 (fn [inputs]
	     (let [[_ _ i a d] (apply swap! mutable-state swap-state-fn inputs)]
	       [i a d]))
	 
	 getData-fn
	 (fn []
	     (let [[extant] @mutable-state]
	       (or (vals extant) '())))
	 ]
     
     [update-fn getData-fn])
   ])

(defn -getData [#^org.dada.core.UnionModel this]
  (let [[_ getData-fn] (.state this)]
    (getData-fn)))

;; TODO: lose this when Clojure collections are Serializable
(defn #^java.util.Collection copy [& args]
  (let [size (count args)
	array-list (java.util.ArrayList. #^Integer size)]
    (if (> size 0) (.addAll array-list args))
    array-list))

(defn #^java.util.Collection copy [& args]
  args)

(defn -update [#^org.dada.core.UnionModel this & inputs]
  (let [[update-fn] (.state this)
	[#^Collection i #^Collection a #^Collection d] (update-fn inputs)]
    (if (not (and (empty? i) (empty? a) (empty? d)))
      (.notifyUpdate this (apply copy i) (apply copy a) (apply copy d)))))
