(ns org.dada.core.ModelImpl
    (:use org.dada.core)
    ;;(:as this)
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

(defn process-addition [[extant extinct key-fn version-fn i a d] addition]
  (let [new (.getNewValue addition)
	key (key-fn new)
	;;dummy (println "PROCESS:" key new)
	current (extant key)]
    (if (nil? current)
      (let [removed (extinct key)]
	(if (nil? removed)
	  [(conj {key new} extant) extinct key-fn version-fn (cons (Update. nil new) i) a d] ;insertion
	  ;; TODO - not extant but extinct
	  )
	)
      ;; extant - alteration - TODO - check version
      [(conj {key new} extant) extinct key-fn version-fn i (cons (Update. current new) a) d]
      )))

(defn process-deletion [[extant extinct key-fn version-fn i a d] addition]
  ;; ensure row with key 
  ;; send insertion/alteration upstreamp
  [extant extinct key-fn version-fn i a d])

(defn process-update [extant extinct key-fn version-fn insertions alterations deletions]
  (reduce process-deletion
	  (reduce process-addition
		  (reduce process-addition
			  [extant extinct key-fn version-fn '() '() '()]
			  insertions)
		  alterations)
	  deletions))

(defn -init [#^String model-name #^Metadata tgt-metadata #^IFn key-fn #^IFn version-fn]
  [;; super ctor args
   [model-name tgt-metadata]
   ;; instance state
   (let [tgt-creator (.getCreator tgt-metadata)
	 update-fn (fn [[extant extinct] & updates]
		       (apply process-update extant extinct key-fn version-fn updates))]
     [(atom [{}{}]) update-fn])
   ])

(defn -getData [this]
  (let [[mutable-state] (.state this)
	[extant extinct] @mutable-state]
    (or (vals extant) '())))

(defn -update [#^AbstractModel this & inputs]
  ;;(debug "MODELIMPL -update:" this inputs)
  (let [[mutable-state update-fn] (.state this)
	;;dummy (debug "MODELIMPL INPUT:" inputs)
	[_ _ _ _ output-insertions output-alterations output-deletions]
	(apply swap! mutable-state update-fn inputs)
	;;dummy (debug "MODELIMPL OUTPUT:" output-insertions output-alterations output-deletions)
	]
    (apply
     (fn [#^Collection insertions #^Collection alterations #^Collection deletions]
	 (if (not (and (nil? insertions) (nil? alterations) (nil? deletions)))
	   (.notifyUpdate this insertions alterations deletions)))
     [output-insertions output-alterations output-deletions])))
