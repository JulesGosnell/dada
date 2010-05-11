(ns org.dada.core.PivotModel
    (:import
     [java.util Collection LinkedHashMap Map]
     [org.dada.core AbstractModel Getter Metadata Update]
     )
    (:gen-class
     :extends org.dada.core.AbstractModelView
     :constructors {[String org.dada.core.Metadata java.lang.Comparable clojure.lang.IFn Object java.util.Collection org.dada.core.Metadata] [String org.dada.core.Metadata]}
     :methods []
     :init init
     :state state
     )
    )

;; TODO: consider supporting indexing on mutable keys - probably not a good idea ?

;; create a map of pivot-key : list of (fn [new-value old-values] ...) where e.g.
;; (apply tgt-creator (map #(% new-value old-values) (pivot-map key)))
;; will copy the value into a new version of the correctly keyed pivotted row...

(defn make-pivot-map [pivot-keys #^Metadata tgt-metadata]
  (let [version-getter (.getAttributeGetter tgt-metadata :version) ;HACK
	time-getter (.getAttributeGetter tgt-metadata :time)	   ;HACK
	#^Map getter-map ;; a map of keys and fns taking new and old values and returning the appliction of the getter on the old value
	(apply
	 array-map
	 (interleave (.getAttributeKeys tgt-metadata)
		     (map
		      #(fn [new-value old-values] (.get #^Getter % old-values))
		      (.getAttributeGetters tgt-metadata))))]
    (apply
     hash-map
     (apply
      concat     
      (map
       (fn [pivot-key]
	   (let [#^Map pivot-map (LinkedHashMap. getter-map)]
	     (.put pivot-map pivot-key (fn [new-value old-values] new-value))
	     ;; TODO: increment version...properly...
	     (.put pivot-map :version (fn [new-value old-values] (let [version (.get version-getter old-values)] (if version (+ version 1) 0))))
	     ;; TODO - hack
	     (.put pivot-map :time (fn [new-value old-values] (let [time (.get time-getter old-values)] (if time time (java.util.Date.)))))
	     [pivot-key (vals pivot-map)]))
       pivot-keys)))
    ))

(defn -init [#^String model-name
	     #^Metadata src-metadata  ;e.g. [day count]
	     #^Comparable the-key     ; e.g. "orca"
	     #^IFn version-fn	      ;     src version fn
	     #^Comparable src-value-key  ;e.g. :count
	     #^Collection tgt-keys    ;e.g. [:mon :tue :wed :thu :fri]
  	     #^Metadata tgt-metadata] ;e.g. [type mon tue wed thu fri]
  [ ;; super ctor args
   [model-name tgt-metadata]
   ;; instance state
   (let [key-getter (.getKeyGetter src-metadata)
	 key-fn (fn [value] (.get key-getter value))

	 ;;----------------------------------------
	 ;; pivot stuff

	 pivot-creator (.getCreator tgt-metadata)
	 pivot-initial-value (.create 
			      pivot-creator
			      (into-array
			       Object
			       (cons the-key
				     (take (- (.size (.getAttributeGetters tgt-metadata)) 1) (repeat nil)))))
	 src-value-getter (.getAttributeGetter src-metadata src-value-key)
	 pivot-map (make-pivot-map tgt-keys tgt-metadata)

	 pivot-fn (fn [old-pivotted key src-value] ;e.g. [["Sei Whale" Mon ... 20 ...] Mon [Mon 21]]
		      (let [src-value-value (.get src-value-getter src-value) ;; e.g. 20
			    pivot-fns (pivot-map key)
			    pivot-values (map #(% src-value-value old-pivotted) pivot-fns)
			    new-pivotted (.create pivot-creator (into-array Object pivot-values))]
			new-pivotted))

	 unpivot-fn (fn [old-pivotted key]
			;; TODO
			old-pivotted)			

	 ;;----------------------------------------

	 process-addition
	 (fn [[extant extinct pivotted i a d] #^Update addition]
	     (let [new (.getNewValue addition)
		   key (last (key-fn new)) ;TODO - last is a hack - pivot-map keyed differently from data
		   current (extant key)]
	       (if (nil? current)
		 ;; insertion...
		 (let [removed (extinct key)]
		   (if (nil? removed)
		     ;; first time seen
		     (let [new-pivotted (pivot-fn pivotted key new)]
		       [(assoc extant key new) extinct new-pivotted (cons (Update. nil new-pivotted) i) a d]) ;insertion
		     ;; already deleted
		     (if (version-fn removed new)
		       ;; later version - reinstated
		       (let [new-pivotted (pivot-fn pivotted key new)]
			 [(assoc extant key new) (dissoc extinct key) new-pivotted (cons (Update. nil new-pivotted) i) a d])
		       (do
			 ;; out of order or duplicate version - ignored
			 ;;(println "WARN: OUT OF ORDER INSERT" current new)
			 [extant extinct pivotted i a d]))
		     )
		   )
		 ;; alteration...
		 (if (version-fn current new)
		   ;; later version - accepted
		   (let [new-pivotted (pivot-fn pivotted key new)]
		     [(assoc extant key new) extinct new-pivotted i (cons (Update. pivotted new-pivotted) a) d]) ;alteration
		   (do
		     ;; out of order or duplicate version - ignored
		     ;;(println "WARN: OUT OF ORDER UPDATE" current new)
		     [extant extinct pivotted i a d]))
		 )))

	 process-deletion
	 (fn [[extant extinct pivotted i a d] #^Update deletion]
	     (let [new (.getNewValue deletion)
		   key (last (key-fn new))
		   current (extant key)]
	       (if (nil? current)
		 (let [removed (extinct key)]
		   (if (nil? removed)
		     ;; neither extant or extinct - mark extinct
		     [extant (dissoc extinct key) pivotted i a d]
		     (if (version-fn removed new)
		       ;; later version - accepted
		       (let [new-pivotted  (pivot-fn pivotted key new)]
			 [extant (assoc extinct key new) new-pivotted i a (cons (Update. pivotted new-pivotted) d)]) ;TODO - is this right ?
		       (do
			 ;; earlier version - ignored
			 ;;(println "WARN: OUT OF ORDER DELETION" current new)
			 [extant extinct pivotted i a d]))))
		 (if (version-fn current new)
		   ;; later version - accepted
		   (let [new-pivotted (unpivot-fn pivotted key)]
		     [(dissoc extant key) (assoc extinct key new) new-pivotted i a (cons (Update. pivotted new-pivotted) d)])
		   (do
		     ;; earlier version - ignored
		     ;;(println "WARN: OUT OF ORDER DELETION" current new)
		     [extant extinct pivotted i a d])))))

	 ;; TODO: perhaps we should raise the granularity at which we
	 ;; compare-and-swap, in order to avoid starvation of larger
	 ;; batches...
	 swap-state-fn (fn [[extant extinct pivotted] insertions alterations deletions]
			   (reduce process-deletion
				   (reduce process-addition
					   (reduce process-addition
						   [extant extinct pivotted '() '() '()]
						   insertions)
					   alterations)
				   deletions))

	 mutable-state (atom [{}{} pivot-initial-value])

	 update-fn
	 (fn [inputs]
	     (let [[_ _ _ i a d] (apply swap! mutable-state swap-state-fn inputs)]
	       [i a d]))
	 
	 getData-fn
	 (fn []
	     (let [[_ _ pivotted] @mutable-state]
	       [pivotted]))
	 ]
     
     [update-fn getData-fn])
   ])

(defn -getData [#^org.dada.core.PivotModel this]
  (let [[_ getData-fn] (.state this)]
    (getData-fn)))

;; TODO: lose this when Clojure collections are Serializable
(defn #^java.util.Collection copy [& args]
  (let [size (count args)
	array-list (java.util.ArrayList. #^Integer size)]
    (if (> size 0) (.addAll array-list args))
    array-list))

(defn -update [#^org.dada.core.PivotModel this & inputs]
  (let [[update-fn] (.state this)
	[#^Collection i #^Collection a #^Collection d] (update-fn inputs)]
    (if (not (and (empty? i) (empty? a) (empty? d)))
      (.notifyUpdate this (apply copy i) (apply copy a) (apply copy d)))))
