(ns org.dada.core.ModelImpl
    (:import
     [java.util Collection LinkedHashMap]
     [org.dada.core AbstractModel Metadata Update]
     )
    (:gen-class
     :extends org.dada.core.AbstractModelView
     :constructors {[String org.dada.core.Metadata clojure.lang.IFn clojure.lang.IFn] [String org.dada.core.Metadata]}
     :methods []
     :init init
     :state state
     )
    )

(defn -init [#^String model-name #^Metadata tgt-metadata #^IFn key-fn #^IFn version-fn]

  [ ;; super ctor args
   [model-name tgt-metadata]
   ;; instance state
   (let [tgt-creator (.getCreator tgt-metadata)

	 process-addition
	 (fn [[extant extinct i a d] #^Update addition]
	     (let [new (.getNewValue addition)
		   key (key-fn new)
		   ;;dummy (println "PROCESS:" key new)
		   current (extant key)]
	       (if (nil? current)
		 (let [removed (extinct key)]
		   (if (nil? removed)
		     [(conj {key new} extant) extinct (cons (Update. nil new) i) a d] ;insertion
		     ;; TODO - not extant but extinct
		     )
		   )
		 ;; extant - alteration - TODO - check version
		 [(conj {key new} extant) extinct i (cons (Update. current new) a) d]
		 )))

	 process-deletion
	 (fn [[extant extinct i a d] #^Update deletion]
	     ;; ensure row with key 
	     ;; send insertion/alteration upstreamp
	     [extant extinct i a d])

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

(defn -getData [#^org.dada.core.ModelImpl this]
  (let [[_ getData-fn] (.state this)]
    (getData-fn)))

;; TODO: lose this when Clojure collections are Serializable
(defn #^java.util.Collection copy [& args]
  (let [size (count args)
	array-list (java.util.ArrayList. #^Integer size)]
    (if (> size 0) (.addAll array-list args))
    array-list))

(defn -update [#^org.dada.core.ModelImpl this & inputs]
  (let [[update-fn] (.state this)
	[#^Collection i #^Collection a #^Collection d] (update-fn inputs)]
    (if (not (and (nil? i) (nil? a) (nil? d)))
      (.notifyUpdate this (apply copy i) (apply copy a) (apply copy d)))))
