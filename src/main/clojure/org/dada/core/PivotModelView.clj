(ns org.dada.core.PivotModelView
    (:use org.dada.core)
    (:import
     [java.util Collection LinkedHashMap]
     [org.dada.core AbstractModel Metadata]
     )
    (:gen-class
     :extends org.dada.core.AbstractModelView
     :constructors {
     [String clojure.lang.Keyword clojure.lang.Keyword java.util.Collection org.dada.core.Metadata org.dada.core.Metadata]
     [String org.dada.core.Metadata]}
     :init init
     :state state
     )
    )

;; create a map of pivot-key : list of (fn [new-value old-values] ...) where e.g.
;; (apply tgt-creator (map #(% new-value old-values) (pivot-map key)))
;; will copy the value into a new version of the correctly keyed pivotted row...

(defn make-pivot-map [pivot-keys tgt-metadata]
  (let [getter-map
	(apply
	 array-map
	 (interleave (.getAttributeKeys tgt-metadata)
		     (map
		      #(fn [new-value old-values] (.get % old-values))
		      (.getAttributeGetters tgt-metadata))))]
    (apply
     hash-map
     (apply
      concat     
      (map
       (fn [pivot-key]
	   (let [xxx (LinkedHashMap. getter-map)]
	     (.put xxx pivot-key (fn [new-value old-values] new-value))
	     ;; TODO: increment version...
	     [pivot-key (vals xxx)]))
       pivot-keys)))
    ))

;; pivot-metadata should probably be created in this init function ...

(defn -init [#^String model-name      ;e.g. "Count/Day"
	     #^Keyword src-key-key    ;e.g. :day
	     #^Keyword src-value-key  ;e.g. :count
	     #^Collection pivot-keys  ;e.g. [:mon :tue :wed :thu :fri]
	     #^Metadata src-metadata  ;e.g. [day count]
	     #^Metadata tgt-metadata] ;e.g. [type mon tue wed thu fri]
  [;; super ctor args
   [model-name tgt-metadata]
   ;; instance state
   ;; atomically updateable input and output maps pairs [extant extinct]
   (let [tgt-creator (.getCreator tgt-metadata)
	 src-key-getter (.getAttributeGetter src-metadata (name src-key-key))
	 src-value-getter (.getAttributeGetter src-metadata (name src-value-key))
	 pivot-map (make-pivot-map (map name pivot-keys) tgt-metadata)
	 pivot-fn (fn [tgt-key src-value output-extant] ;e.g. ["Sei Whale" [Mon ... 20 ...]]
		      (let [src-value-key (.get src-key-getter src-value) ;; e.g. Mon
			    src-value-value (.get src-value-getter src-value) ;; e.g. 20
			    old-tgt-value (output-extant tgt-key) ;; e.g.["Sei Whale"  20  30 ...]
			    pivot-fns (pivot-map src-value-key)]
			(apply
			 ;; a creator-fn for the pivoted row
			 (fn [& args] (.create tgt-creator args))
			 ;; the pivoted creator-fn args - 
			 (map #(% src-value-value old-tgt-value) pivot-fns))))
		       
	 update-fn (fn [old-state &inputs]
		       ;; (apply
		       ;; 	process-update
		       ;; 	in-extant in-extinct out-extant out-extinct pivot-fn inputs)
		       )]
     [(atom [{}{}{}{}]) update-fn])
   ])

    ;; 	;; this should all be done in init and a fn produced to be passed to swap...
    ;; 	i (.iterator (.getKeyAttributeKeys src-metadata))
    ;; 	src-key-getter (.getAttributeGetter (.next i))
    ;; 	src-version-getter (.getAttributeGetter (.next i))]
    ;; (doall
    ;;  (map
    ;;   (fn [insertion]
    ;; 	  (println ["PIVOT INSERTION:" insertion])
    ;; 	  (swap!
    ;; 	   mutable-state
    ;; 	   (fn [[inExtant inExtinct outExtant outExtinct src-key-getter src-version-getter] insertion]
    ;; 	       (let [src-key (src-key-getter insertion)
    ;; 		     src-version (src-version-getter insertion)
    ;; 		     current (inExtant src-key)
    ;; 		     current-version (src-version-getter current)]
    ;; 		 (debug "PIVOT INSERTION:" src-key src-version current-version)
    ;; 		 ;; (if (> src-version current-version)
    ;; 		 ;; 	 (let [transformed ]
		 
    ;; 		 ;; 	   )
    ;; 		 ;; 	 (debug "out of order version:" src-key "-" src-version "<=" current-version))
    ;; 		 ;;[(conj {src-key transformed} inExtant) inExtinct outExtant outExtinct]
    ;; 		 )))
    ;; 	  ;; don't forget to call notifyUpdate
    ;; 	  )
    ;;   insertions
    ;;   )))

;; e.g.
;; Split by type:
;;  Sei Whale model :
;;    Mon : 20
;;    Tue : 30
;;    Wed : 35
;;    Thu : 40
;;    Fri : 43
;; Blue Whale model:
;;    Mon : 10
;;    Tue : 11
;;    Wed : 12
;;    Thu : 11
;;    Fri : 09
;; etc...
;; -->
;;    Type       Mon Tue Wed Thu Fri
;;    Sei Whale  20  30  35  40  43
;;    Blue Whale 10  11  12  11  09
;;    etc...

(defn process-addition [in-extant in-extinct out-extant out-extinct pivot-fn addition]
  ;; ensure row with key 
  ;; copy row forward into new row, overlaying field with incoming key's value

;;  (pivot-fn tgt-key src-value output-extant)
  
  ;; TODO - start here

  ;; increment version
  ;; keep input and output models
  ;; send insertion/alteration upstream
  [[nil nil nil]])

(defn process-deletion [in-extant in-extinct out-extant out-extinct pivot-fn deletion]
  [[nil nil nil]])

(defn process-update [in-extant in-extinct out-extant out-extinct pivot-fn
		      insertions alterations deletions]
  ;;TODO - sort out state required...
  (map
   identity ;; TODO - sort out reduction of list
   (concat
    (map #(process-addition %) in-extant in-extinct out-extant out-extinct pivot-fn insertions)
    (map #(process-addition %) in-extant in-extinct out-extant out-extinct pivot-fn alterations)
    (map #(process-deletion %) in-extant in-extinct out-extant out-extinct pivot-fn deletions))))

(defn -getData [this]
  (let [[[in-extant in-extinct out-extant out-extinct]] @(.state this)]
    (or (vals out-extant) '())))

(defn -update [#^AbstractModel this & inputs]
  (let [[mutable-state update-fn] @(.state this)
	dummy (debug "Pivot input:" inputs)
	[_ _ outputs] (apply swap! mutable-state update-fn inputs)]
    (debug "Pivot output:" outputs)
    (apply
     (fn [#^Collection insertions #^Collection alterations #^Collection deletions]
	 (if (not (and (nil? insertions) (nil? alterations) (nil? deletions)))
	   (.notifyUpdate this insertions alterations deletions)))
     outputs)))
