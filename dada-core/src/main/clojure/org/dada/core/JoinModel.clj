(ns org.dada.core.JoinModel
  (:use
   [clojure.contrib logging]
   [org.dada core]
   )
  (:import
   [java.util Collection LinkedHashMap Map]
   [org.dada.core AbstractModel Attribute Data Getter Metadata Metadata$Comparator Model Tuple Update View]
   )
  (:gen-class
   :extends org.dada.core.AbstractModel
   :constructors {[String org.dada.core.Model clojure.lang.IFn java.util.Collection] [String org.dada.core.Metadata]}
   :init init
   :state state
   :post-init post-init)
  )

;; these types should really be created on-the-fly to match lhs/rhs relationships
(defrecord LHSEntry [extant version lhs rhs-tuple element]) ;; rhs-tuple - resolution of lhs fks to rhs references
(defrecord RHSEntry [rhs lhs-refs]) ;; lhs-refs - {lhs-pk : [lhs-fk-i, ...], ...}

(defn get-immutable [
		     ;; lhs-key lhs-metadata
		     ^Model rhs-model]
  (let [rhs-metadata (.getMetadata rhs-model)
	;;lhs-getter (.getGetter (.getAttribute lhs-metadata lhs-key))
	]
    [(.getPrimaryGetter rhs-metadata)(.getVersionGetter rhs-metadata)(.getVersionComparator rhs-metadata) ;; lhs-getter
     ]))  

(defn get-key-fn [^Metadata metadata keys]
  (let [getters (map (fn [key] (.getGetter (.getAttribute metadata key))) keys)
	[^Getter h & t] getters]
    (if (empty? t)
      (fn [datum] (.get h datum))
      (fn [datum] (map (fn [^Getter getter] (.get getter datum)) getters)))))
   
(defn -init [^String model-name
	     ^Model lhs-model
	     join-fn
	     rhses]
  (let [model-metadata nil]
    (info "Join: init")
    [ ;; super ctor args
     [model-name model-metadata]
     ;; instance state [atom(mutable) immutable]
     (let [lhs-metadata (.getMetadata lhs-model)
	   lhs-mutable {}
	   lhs-immutable (get-immutable lhs-model)
	   rhs-mutables (apply vector (repeatedly (count rhses) hash-map))
	   rhs-immutables (map
			   (fn [[key-lists rhs-model]]
			     (concat
			      (get-immutable rhs-model)
			      [(map
				(fn [keys]
				  (map
				   (fn [key] (.getGetter (.getAttribute lhs-metadata key)))
				   keys))
				key-lists)]))
			   rhses)

	   ;; [[index key-fn] & ...]
	   ;;fks (mapcat (fn [i [fk-lists]] (map (fn [fk-list] [i (get-key-fn lhs-metadata fk-list)]) fk-lists)) rhses)
	   
	   ]

       ;; need to expand rhses into :
       
       [(atom [lhs-mutable rhs-mutables]) [lhs-immutable rhs-immutables]])]))

(defn MAKE-ENTRY [& args] ;; TODO
  (info ["MAKE-ENTRY" args]))

;; returns [new-rhs-indeces new-rhs-tuple new-element]
(defn process-rhs [old-lhs new-lhs old-rhs-indeces old-rhs-tuple old-element]
  (info ["process-rhs" old-lhs new-lhs old-rhs-indeces old-rhs-tuple old-element])

  
  ;; walk across just the fks that we are interested in, comparing old-lhs with new-lhs
  ;; if we find any changes create a new-rhs-tuple, element and updated rhs-indeces

  ;; what do we need ?
  ;; rhses, each one of which has a set of lhs-fk-getters
  ;; we run each lhs-fk-getter against old and new lhs and compare outputs

  ;; find rhs-indeces that need updating

  ;; THIS NEEDS SORTING OUT WITH A FRESH BRAIN TOMORROW



  ;; NEW-ELEMENT must be identical to OLD_ELEMENT if we don't want any events sent...

  ;; figure out which columns need updating.
  (let [FKS [] ;; TODO
	changes (reduce
		 (fn [reduction [i key-fn]]
		   (let [old-value (key-fn old-lhs)
			 new-value (key-fn new-lhs)]
		     (if (= old-value new-value)
		       reduction
		       (conj reduction [i old-value new-value]))))
		 '()
		 FKS)]
    
    (if (empty? changes)
      ;; none of the columns relevant to the join have changed
      [old-rhs-indeces old-rhs-tuple old-element]
      ;; rebuild rhs-tuple, rhs-indeces and element accordingly
      (let [[ori ort oev] (reduce
			   (fn [[old-rhs-indeces old-rhs-tuple old-entry-values] [i old-value new-value]]
			     ;; update rhs-index
			     (let [old-rhs-index (nth old-rhs-indeces i)
				   old-rhs-entry (old-rhs-index old-value)]
			       ;;(assoc old-rhs-index

			       ;; TODO: RAN OUT OF STEAM HERE...
			       
			       ;;(assoc old-rhs-indeces i (RHSEntry. (.rhs)))
			       ;; update rhs-tuple
			       ;; update entry-value
			       ))
			   [old-rhs-indeces old-rhs-tuple nil ;;(map (fn [getter] (.get getter old-entry)) getters)
			    ]
			   )]
	[ori ort (apply MAKE-ENTRY oev)]
	))))

;; h/w immutables into fn - slower to carry them as args with every call

;; outputs [rhs-tuple rhs-indeces]
(defn update-rhs-tuple [old-lhs old-lhs-rhs-tuple new-lhs old-rhs-indeces rhs-immutables]
  [(if old-lhs
     []
     (map
      (fn [rhs-index [_ _ _ lhs-fk-getters-list]]
	  (map
	   (fn [lhs-fk-getters]
	     (map
	      (fn [^Getter lhs-fk-getter] 
		(let [rhs-pk (.get lhs-fk-getter new-lhs)
		      ^RHSEntry old-rhs-entry (rhs-index rhs-pk)
		      dummy (info ["  found entry" rhs-pk "->" old-rhs-entry ":" rhs-index])
		      rhs (and old-rhs-entry (.rhs old-rhs-entry))
		      dummy (info ["  found datum" rhs-pk "->" rhs])
		      new-rhs-entry (RHSEntry. rhs (conj (.lhs-refs old-rhs-entry) rhs-pk))]
		  rhs))
	      lhs-fk-getters))
	   lhs-fk-getters-list))
      old-rhs-indeces
      rhs-immutables))
   old-rhs-indeces])

;; returns -> [mutable immutable insertions alterations deletions]
(defn update-lhs [[lhs-index rhs-indeces] [[^Getter lhs-pk-getter ^Getter lhs-version-getter ^Metadata$Comparator lhs-version-comparator] rhs-immutables] insertions alterations deletions]
  (info ["update-lhs" [lhs-index rhs-indeces] insertions alterations deletions])
  
  (reduce
   (fn [[old-lhs-index old-rhs-indeces old-insertions old-alterations old-deletions] ^Update update]
     (let [new-lhs (.getNewValue update)
	   lhs-pk (.get lhs-pk-getter new-lhs)]
       (info [" processing" lhs-pk])
       (if-let [^LHSEntry old-lhs-entry (old-lhs-index lhs-pk)]
	 (let [old-lhs (.lhs old-lhs-entry)]
	   (info [" update-lhs: index alteration" lhs-pk])
	   (if (.higher lhs-version-comparator old-lhs new-lhs)
	     (let [old-version (.version old-lhs-entry)
		   old-rhs-tuple (.rhs-tuple old-lhs-entry)
		   old-extant (.extant old-lhs-entry)
		   old-datum (.element old-lhs-entry)
		   [new-rhs-tuple new-rhs-indeces] (update-rhs-tuple old-lhs old-rhs-tuple new-lhs old-rhs-indeces rhs-immutables)
		   dummy (info " update-rhs: new-lhs-tuple:" new-rhs-tuple)
		   new-datum nil ;; TODO
		   new-lhs-entry (LHSEntry. true (inc old-version) new-lhs new-rhs-tuple new-datum)
		   new-lhs-index (assoc old-lhs-index lhs-pk new-lhs-entry)]
	       (info ["  update-lhs: version accepted" old-lhs-entry "->" new-lhs-entry])
	       [new-lhs-index
		new-rhs-indeces
		old-insertions
		;; TODO: may be an insertion, not an alteration ?
		(if (identical? old-datum new-datum) old-alterations (conj old-alterations (Update. old-datum new-datum)))
		old-deletions]
		)
	     (do
	       (info ["  update-lhs: version rejected" lhs-pk])
	       ;; ignore this update - version is superceded
	       [old-lhs-index old-rhs-indeces old-insertions old-alterations old-deletions])))
	 (let [[new-rhs-tuple new-rhs-indeces] (update-rhs-tuple nil nil new-lhs old-rhs-indeces rhs-immutables)
	       new-datum nil
	       new-lhs-index (assoc old-lhs-index lhs-pk (LHSEntry. true 0 new-lhs new-rhs-tuple new-datum))]
	   (info ["  inserting" lhs-pk])
	   ;; add a new entry
	   ;; update corresponding rhses - TODO
	   ;; needs to share code with above...
	   [new-lhs-index
	    new-rhs-indeces
	    ;; TODO - insertion needed here (maybe?)
	    old-insertions old-alterations old-deletions]))))
   [lhs-index rhs-indeces nil nil nil]
   (concat insertions alterations))
    
  ;; maintains a map of lhs-pk: [join-version join-value lhs & rhses] (nil if unmatched)

  ;; [maybe] update lhs-index
  ;; [maybe] update rhs-indeces
  


  ;; THOUGHTS;
  ;; handle out-of-order versioned updates
  ;; handle OUTER joins
  ;; handle more than one join at a time
  ;; should we copy required cols into output row, or ref them from compound object ?
  ;; can we handle updates on rhs with same code as lhs but just reverse sides for inputs ?
  ;; what if output-pk is same as lhs-pk - can we save cycles - similarly on other side ?
  ;; raise events
  ;; implement output model
  ;; if two rows are joined then unjoined will we forget their joined version ? - problem - need to implement 'extinct' as well
  ;; allow disabling of indeces for faster runtime

  
  )

(defn update-lhs-index [lhs-index lhs-refs rhs]
  (info ["update-lhs-index"])
  (reduce
   (fn [lhs-index [lhs-key lhs-fk-is]]
     (let [^LHSEntry old-lhs-entry (lhs-index lhs-key)]
       (assoc lhs-index
	 lhs-key
	 (LHSEntry.
	  true
	  (inc (.version old-lhs-entry))
	  (.lhs old-lhs-entry)
	  (reduce (fn [lhs-rhs-tuple i] (assoc lhs-rhs-tuple i rhs)) (.rhs-tuple old-lhs-entry) lhs-fk-is) ;; new-rhs-tuple
	  nil ;; datum - TODO
	  ))))
   lhs-index
   lhs-refs))

;; returns -> [mutable immutable insertions alterations deletions]
(defn update-rhs [[lhs-index rhs-indeces] [lhs-immutable rhs-immutables] i insertions alterations deletions]
  ;; TODO: extant/extinct / deletion
  (info ["update-rhs" i rhs-indeces])
  (let [rhs-index (nth rhs-indeces i)
	[^Getter rhs-pk-getter ^Getter rhs-version-getter ^Metadata$Comparator rhs-version-comparator lhs-fk-getters] (nth rhs-immutables i)]
    (info ["update-rhs:" rhs-index insertions alterations deletions])

    (let [[lhs-index rhs-index insertions alterations deletions]
	  (reduce
	   (fn [[lhs-index rhs-index i a d] ^Update update]
	     (let [new-rhs (.getNewValue update)
		   rhs-pk (.get rhs-pk-getter new-rhs)]
	       (if-let [^RHSEntry old-rhs-entry (rhs-index rhs-pk)]
		 (let [old-rhs (.rhs old-rhs-entry)]
		   (info [" update-rhs: index alteration" rhs-pk old-rhs new-rhs])
		   (if (.higher rhs-version-comparator old-rhs new-rhs)
		     (let [lhs-refs (.lhs-refs old-rhs-entry)
			   new-lhs-index (update-lhs-index lhs-index lhs-refs new-rhs)
			   new-rhs-entry (RHSEntry. new-rhs lhs-refs)
			   new-rhs-index (assoc rhs-index rhs-pk new-rhs-entry)
			   ;; TODO: if the old-rhs-entry had NO rhs, then this is an INSERTION, if not, an ALTERATION
			   new-alterations (conj a nil)] ;NYI - notification
		       (info ["  update-rhs: version accepted" old-rhs-entry "->" new-rhs-entry])
		       [new-lhs-index new-rhs-index i new-alterations d]
		       )
		     (do
		       (info ["  update-rhs: version rejected" rhs-pk])
		       [lhs-index rhs-index i a d]))
		   )
		 (let [new-rhs-entry (RHSEntry. new-rhs #{})
		       new-rhs-index (assoc rhs-index rhs-pk new-rhs-entry)]
		   (info [" update-rhs: index insertion" rhs-pk lhs-index rhs-index])
		   ;; no notification needed, since no new join occurred
		   ;; when data is added on lhs, new items may be added to rhs indeces, although rhs datum has not yet been seen
		   [lhs-index new-rhs-index i a d]
		   )
		 )
	       )
	     )
	   [lhs-index rhs-index nil nil nil]
	   (concat insertions alterations))]
      [lhs-index (assoc rhs-indeces i rhs-index) insertions alterations deletions]
      )))

(defn -post-init [^org.dada.core.JoinModel self _ ^Model lhs-model _ rhses]
  (let [[mutable immutable] (.state self)]
    
    (info ["post-init: watching lhs:" lhs-model])
    (.registerView
     lhs-model
     (proxy [View] []
       (update [insertions alterations deletions]
	       (let [[_mutable insertions alterations deletions] (swap! mutable update-lhs immutable insertions alterations deletions)]
		 (.notifyUpdate self insertions alterations deletions)))))
    (dorun
     (map
      (fn [i [_ ^Model rhs-model]]
	(info ["post-init: watching rhs:" i rhs-model])
	(.registerView
	 rhs-model
	 (proxy [View] []
	   (update [insertions alterations deletions]
		   (let [[_mutable insertions alterations deletions] (swap! mutable update-rhs immutable i insertions alterations deletions)]
		     (.notifyUpdate self insertions alterations deletions))))))
      (range)
      rhses))))

(defn -getData [^org.dada.core.JoinModel this]
  (info "Join: getData")
  (Data. (vals (nth @(first (.state this)) 4)) nil))

(defn -find [^org.dada.core.JoinModel this key]
  (info ["Join: find" key])
  ((nth @(first (.state this)) 4) key))

;;--------------------------------------------------------------------------------

;; an index is a pair of PersistentMaps

;; (defprotocol Indexer
;;   (insert [index insertion])
;;   (delete [index deletion])
;;   (get [keys index values]))

;; (deftype LazyIndexer [key-fn]
;;   Indexer
;;   ;; do nothing up front
;;   (insert [index insertion])
;;   (delete [index deletion])
;;   (get [key index values]
;; 	 ;; scan collection for matching elements
;; 	 (filter (fn [v] (= key (key-fn v))) values)))

;; (deftype AgressiveIndexer [key-fn]
;;   Indexer
;;   ;; do work up front - update index
;;   (insert [[extant old-extinct] old-value new-value]
;; 	  (let [key (key-fn new-value)
;; 		new-extinct (dissoc old-extinct key)]
;; 	    (if (identical? 
;; 				    )
;;   (delete [index deletion])
  
;;   (get [key index values]
;; 	 (index key)))
