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
	  (fn [[extant extinct i a d] #^Update addition]
	      ;; ensure row with key 
	      ;; send insertion/alteration upstreamp
	      [extant extinct i a d])

	  process-update
	  (fn [extant extinct insertions alterations deletions]
	      (reduce process-deletion
		      (reduce process-addition
			      (reduce process-addition
				      [extant extinct '() '() '()]
				      insertions)
			      alterations)
		      deletions))

	 update-fn (fn [[extant extinct] & updates]
		       (apply process-update extant extinct updates))]
     [(atom [{}{}]) update-fn])
   ])

(defn -getData [#^org.dada.core.ModelImpl this]
  (let [[mutable-state] (.state this)
	[extant extinct] @mutable-state]
    (or (vals extant) '())))

(defn -update [#^org.dada.core.ModelImpl this & inputs]
  ;;(debug "MODELIMPL -update:" this inputs)
  (let [[mutable-state update-fn] (.state this)
	;;dummy (debug "MODELIMPL INPUT:" inputs)
	[_ _ output-insertions output-alterations output-deletions]
	(apply swap! mutable-state update-fn inputs)
	;;dummy (debug "MODELIMPL OUTPUT:" output-insertions output-alterations output-deletions)
	]
    (apply
     (fn [#^Collection insertions #^Collection alterations #^Collection deletions]
	 (if (not (and (nil? insertions) (nil? alterations) (nil? deletions)))
	   (.notifyUpdate this insertions alterations deletions)))
     [output-insertions output-alterations output-deletions])))
