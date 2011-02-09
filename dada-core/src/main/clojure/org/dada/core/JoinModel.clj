(ns org.dada.core.JoinModel
  (:use
   [clojure.contrib logging]
   [org.dada core]
   )
  (:import
   [java.util Collection LinkedHashMap Map]
   [org.dada.core AbstractModel Attribute Data Getter Metadata Metadata$VersionComparator Model Tuple Update View]
   )
  (:gen-class
   :extends org.dada.core.AbstractModel
   :constructors {[String org.dada.core.Metadata org.dada.core.Model java.util.Map clojure.lang.IFn] [String org.dada.core.Metadata]}
   :init init
   :state state
   :post-init post-init)
  )


;; these types should really be created on-the-fly to match lhs/rhs relationships
(defrecord LHSEntry [extant version lhs rhs-refs datum]) ;; rhs-refs - resolution of lhs fks to rhs references
(defrecord RHSEntry [rhs lhs-pks]) ;; lhs-pks - {lhs-pk : [lhs-fk-i, ...], ...}

(defn get-key-fn [^Metadata metadata keys]
  (let [getters (map (fn [key] (.getGetter (.getAttribute metadata key))) keys)
	[^Getter h & t] getters]
    (if (empty? t)
      (fn [datum] (.get h datum))
      (fn [datum] (map (fn [^Getter getter] (.get getter datum)) getters)))))
   
(defn invert-map [m]
  (reduce (fn [r [k v]] (assoc r v (conj (r v) k))) {} m))

(defn make-notification [old-datum new-datum old-insertions old-alterations old-deletions delete]
  (debug ["make-notification" old-datum new-datum old-insertions old-alterations old-deletions delete])
  (if new-datum
    (if old-datum
      (if delete
	[old-insertions old-alterations (conj old-deletions (Update. old-datum nil))] ;deletion
	[old-insertions (conj old-alterations (Update. old-datum new-datum)) old-deletions]) ;alteration
      [(conj old-insertions (Update. old-datum new-datum)) old-alterations old-deletions]) ;insertion
    (if old-datum
      [old-insertions old-alterations (conj old-deletions (Update. old-datum new-datum))] ;deletion
      [old-insertions old-alterations old-deletions]))) ;no change

(defn -init [^String model-name
	     model-metadata
	     ^Model lhs-model
	     rhses
	     join-fn]
  (debug "Join: init")
  [ ;; super ctor args
   [model-name model-metadata]
   ;; instance state [atom(mutable) immutable]
   (let [lhs-metadata (.getMetadata lhs-model)
	 lhs-mutable {}
	 rhs-mutables (apply vector (repeatedly (count (invert-map rhses)) hash-map))]
     [(atom [lhs-mutable rhs-mutables])])])

;;--------------------------------------------------------------------------------

(defn update-lhs-rhs-indeces [lhs-pk old-lhs new-lhs rhs-i-to-lhs-getters old-rhs-refs old-rhs-indeces delete]
  (debug ["update-lhs-rhs-indeces" rhs-i-to-lhs-getters old-rhs-indeces])
  (reduce
   (fn [[old-rhs-refs-1 old-rhs-indeces-1] [i lhs-getters rhs-index]]
     (reduce
      (fn [[old-rhs-refs-2 old-rhs-indeces-2] [j ^Getter lhs-getter]]
	(debug [" update-lhs-rhs-indeces: inputs" old-rhs-refs-2 old-rhs-indeces-2])
	(let [old-rhs-pk (if old-lhs (.get lhs-getter old-lhs))
	      new-rhs-pk (.get lhs-getter new-lhs)
	      dummy (debug ["old-rhs-pk new-rhs-pk" old-rhs-pk new-rhs-pk])]
	  (if (and (not delete) (= old-rhs-pk new-rhs-pk))
	    (do
	      (debug [" update-lhs-rhs-indeces: unchanged" i old-rhs-pk])
	      [ old-rhs-refs-2 old-rhs-indeces-2])
	    (let [old-rhs-index (old-rhs-indeces-2 i)
		  ;; remove lhs from old-rhs-pks (if necessary) - TODO - tidy up
		  dummy (debug ["old-rhs-pk" old-rhs-pk old-rhs-index])
		  tmp-rhs-index (if-let [^RHSEntry old-old-rhs-entry (old-rhs-index old-rhs-pk)]
				  (let [old-lhs-pks (.lhs-pks old-old-rhs-entry)
					new-lhs-pks (assoc old-lhs-pks j (disj (old-lhs-pks j) lhs-pk))
					new-old-rhs-entry (RHSEntry. (.rhs old-old-rhs-entry) new-lhs-pks)
					dummy (debug ["new-old-rhs-entry" new-old-rhs-entry])
					tmp-rhs-index (assoc old-rhs-index old-rhs-pk new-old-rhs-entry)]
				    tmp-rhs-index)
				  old-rhs-index)
		  ;; add lhs to new-rhs-pks
		  [rhs new-rhs-index] (if delete ;we have already deleted rhs backptrs - do no more
					[nil tmp-rhs-index]
					(let [^RHSEntry old-new-rhs-entry (tmp-rhs-index new-rhs-pk)
					    [rhs old-lhs-pks] (if old-new-rhs-entry
								[(.rhs old-new-rhs-entry) (.lhs-pks old-new-rhs-entry)]
								[nil
								 ;; TODO - should use templated value
								 (apply vector (repeatedly (count lhs-getters) hash-set))])
					    new-lhs-pks (assoc old-lhs-pks j (conj (old-lhs-pks j) lhs-pk))
					    new-new-rhs-entry (RHSEntry. rhs new-lhs-pks)
					    dummy (debug ["new-new-rhs-entry" new-new-rhs-entry])
					    new-rhs-index (assoc tmp-rhs-index new-rhs-pk new-new-rhs-entry)]
					[rhs new-rhs-index]))
		  ;; update rhs-indeces
		  new-rhs-indeces (assoc old-rhs-indeces-2 i new-rhs-index)
		  ;; update rhs-refs
		  new-rhs-refs (assoc old-rhs-refs-2 i (assoc (old-rhs-refs-2 i) j rhs))]
	      (debug [" update-lhs-rhs-indeces: changed" i old-rhs-pk "->" new-rhs-pk])
	      [new-rhs-refs new-rhs-indeces]))))
      [old-rhs-refs-1 old-rhs-indeces-1]
      (map (fn [j lhs-getter] [j lhs-getter]) (range) lhs-getters))
     )
   [old-rhs-refs old-rhs-indeces]
   (map (fn [[i lhs-getter] rhs-index] [i lhs-getter rhs-index]) rhs-i-to-lhs-getters old-rhs-indeces))
  )

(defn reduce-lhs [reduction updates ^Getter lhs-pk-getter initial-rhs-refs ^Metadata$VersionComparator lhs-version-comparator rhs-i-to-lhs-getters join-fn delete]
  (reduce
   (fn [[old-lhs-index old-rhs-indeces old-insertions old-alterations old-deletions] ^Update update]
     (let [new-lhs (.getNewValue update)
	   latest-lhs (or new-lhs (.getOldValue update))
	   lhs-pk (.get lhs-pk-getter latest-lhs)
	   ^LHSEntry crt-lhs-entry (old-lhs-index lhs-pk)
	   [crt-lhs-entry-version crt-lhs-entry-extant crt-lhs-entry-rhs-refs crt-lhs]
	   (if crt-lhs-entry
	     [(.version crt-lhs-entry)(.extant crt-lhs-entry)(.rhs-refs crt-lhs-entry)(.lhs crt-lhs-entry)]
	     [-1 false initial-rhs-refs nil])]
       (if (and crt-lhs-entry (not ((if delete <= <) (.compareTo lhs-version-comparator crt-lhs (if delete latest-lhs new-lhs)) 0)))
	 (do
	   (debug ["update-lhs - alteration - rejected" crt-lhs new-lhs])
	   [old-lhs-index old-rhs-indeces old-insertions old-alterations old-deletions])
	 (let [[new-rhs-refs new-rhs-indeces]
	       (update-lhs-rhs-indeces lhs-pk (if crt-lhs-entry-extant crt-lhs nil) new-lhs rhs-i-to-lhs-getters crt-lhs-entry-rhs-refs old-rhs-indeces delete)
	       new-lhs-version (if delete crt-lhs-entry-version (inc crt-lhs-entry-version))
	       crt-entry-datum (if crt-lhs-entry (.datum crt-lhs-entry))
	       new-datum (if delete crt-entry-datum (join-fn lhs-pk new-lhs-version new-lhs new-rhs-refs))
	       dummy (debug [new-datum crt-lhs-entry])
	       new-lhs-entry (LHSEntry. (not delete) new-lhs-version (if delete (or new-lhs crt-lhs) new-lhs) new-rhs-refs new-datum)
	       new-lhs-index (assoc old-lhs-index lhs-pk new-lhs-entry)
	       [new-insertions new-alterations new-deletions] (make-notification (if crt-lhs-entry-extant crt-entry-datum nil) new-datum old-insertions old-alterations old-deletions delete)]
	   (debug ["update-lhs - insertion/alteration - accepted" crt-lhs new-lhs])
	   [new-lhs-index new-rhs-indeces new-insertions new-alterations new-deletions]
	   ))))
   reduction
   updates))

(defn update-lhs
  "return new mutable state and events in response to notifications from the left hand side model"
  [[old-lhs-index old-rhs-indeces] insertions alterations deletions ^Getter lhs-pk-getter ^Metadata$VersionComparator lhs-version-comparator rhs-i-to-lhs-getters join-fn]
  (debug ["update-lhs" rhs-i-to-lhs-getters])
  (let [initial-rhs-refs [[nil][nil nil]]] ;TODO - parameterise
    (reduce-lhs     
     (reduce-lhs     
      [old-lhs-index old-rhs-indeces nil nil nil]
      (concat insertions alterations)
      lhs-pk-getter
      initial-rhs-refs
      lhs-version-comparator
      rhs-i-to-lhs-getters
      join-fn
      false)
     deletions
     lhs-pk-getter
     initial-rhs-refs
     lhs-version-comparator
     rhs-i-to-lhs-getters
     join-fn
     true)))

;;--------------------------------------------------------------------------------

(defn update-rhs-lhs-index [old-lhs-index lhs-pks-list i new-rhs join-fn old-insertions old-alterations old-deletions]
  (debug ["update-rhs-lhs-index" old-lhs-index lhs-pks-list new-rhs])
  (reduce
   (fn [[old-lhs-index old-insertions old-alterations old-deletions] [j lhs-pks]]
     (reduce
      (fn [[old-lhs-index old-insertions old-alterations old-deletions] lhs-pk]
	(let [^LHSEntry old-lhs-entry (old-lhs-index lhs-pk)
	      old-lhs-rhs-refs (.rhs-refs old-lhs-entry)
	      new-lhs-rhs-refs (assoc old-lhs-rhs-refs i (assoc (old-lhs-rhs-refs i) j new-rhs))
	      new-lhs-version (inc (.version old-lhs-entry))
	      lhs (.lhs old-lhs-entry)
	      old-datum (if old-lhs-entry (.datum old-lhs-entry))
	      new-datum (join-fn lhs-pk new-lhs-version lhs new-lhs-rhs-refs)
	      [new-insertions new-alterations new-deletions] (make-notification old-datum new-datum old-insertions old-alterations old-deletions false) ;TODO
	      new-lhs-index (assoc old-lhs-index lhs-pk (LHSEntry. (.extant old-lhs-entry) new-lhs-version lhs new-lhs-rhs-refs new-datum))]
	  [new-lhs-index new-insertions new-alterations new-deletions]))
      [old-lhs-index old-insertions old-alterations old-deletions]
      lhs-pks))
   [old-lhs-index old-insertions old-alterations old-deletions]
   (map (fn [j lhs-pks][j lhs-pks])(range)lhs-pks-list)))

(defn update-rhs
  "return new mutable state and events in response to notifications from a right hand side model"
  [[old-lhs-index old-rhs-indeces] insertions alterations deletions i ^Getter rhs-pk-getter ^Metadata$VersionComparator rhs-version-comparator lhs-getters join-fn]
  (let [initial-lhs-pks (apply vector (repeatedly (count lhs-getters) hash-set)) ;TODO - do not rebuild each time...
	old-rhs-index (nth old-rhs-indeces i)]
    (debug ["update-rhs" i lhs-getters old-rhs-index])

    ;; insertions/alterations
    (let [[new-lhs-index new-rhs-index new-insertions new-alterations new-deletions]
	  (reduce
	   (fn [[old-lhs-index old-rhs-index old-insertions old-alterations old-deletions] ^Update update]
	     (let [new-rhs (.getNewValue update)
		   rhs-pk (.get rhs-pk-getter new-rhs)]
	       (if-let [^RHSEntry old-rhs-entry (old-rhs-index rhs-pk)]
		 (let [old-rhs (.rhs old-rhs-entry)]
		   (if (or (nil? old-rhs) (< (.compareTo rhs-version-comparator old-rhs new-rhs) 0))
		     (let [new-rhs-entry (RHSEntry. new-rhs (if old-rhs-entry (.lhs-pks old-rhs-entry) initial-lhs-pks))
			   new-rhs-index (assoc old-rhs-index rhs-pk new-rhs-entry)
			   [new-lhs-index new-insertions new-alterations-new-deletions]
			   (update-rhs-lhs-index old-lhs-index (.lhs-pks old-rhs-entry) i new-rhs join-fn old-insertions old-alterations old-deletions)]
		       (debug ["update-rhs - alteration - accepted" old-rhs new-rhs])
		       [new-lhs-index new-rhs-index new-insertions new-alterations-new-deletions])
		     (do
		       (debug ["update-rhs - alteration - rejected" old-rhs new-rhs])
		       [old-lhs-index old-rhs-index old-insertions old-alterations old-deletions])))
		 (let [new-rhs-entry (RHSEntry. new-rhs initial-lhs-pks)
		       new-rhs-index (assoc old-rhs-index rhs-pk new-rhs-entry)]
		   (debug ["update-rhs - insertion" new-rhs])
		   [old-lhs-index new-rhs-index old-insertions old-alterations old-deletions]))))
	   [old-lhs-index old-rhs-index nil nil nil]
	   (concat insertions alterations))
	  new-rhs-indeces (assoc old-rhs-indeces i new-rhs-index)]
      [new-lhs-index new-rhs-indeces new-insertions new-alterations new-deletions])
    
    ;; deletions - TO DO
    
    ))

;;--------------------------------------------------------------------------------

(defn -post-init [^org.dada.core.JoinModel self _ _ ^Model lhs-model rhses join-fn]
  (let [[mutable immutable] (.state self)
	rhs-model-to-lhs-fks (invert-map rhses)
	lhs-metadata (.getMetadata lhs-model)
	get-lhs-getter (fn [key] (.getGetter (.getAttribute lhs-metadata key)))
	i-to-rhs-model-and-lhs-getters (into
					(sorted-map)
					(map
					 (fn [i [^Model model keys]]
					   [i [model (map get-lhs-getter keys)]])
					 (range)
					 rhs-model-to-lhs-fks))
	i-to-lhs-getters (into (sorted-map) (map (fn [[i [model getters]]] [i getters]) i-to-rhs-model-and-lhs-getters))]
    
    (dorun
     (map
      (fn [[i [^Model rhs-model lhs-getters]]]
	(debug ["post-init: watching rhs:" i rhs-model])
	(let [^Metadata rhs-metadata (.getMetadata rhs-model)
	      rhs-pk-getter (.getPrimaryGetter rhs-metadata)
	      rhs-version-comparator (.getVersionComparator rhs-metadata)
	      ^Data data
	      (.registerView
	       rhs-model
	       (proxy [View] []
		 (update [insertions alterations deletions]
			 (let [[_ _ insertions alterations deletions]
			       (swap! mutable update-rhs insertions alterations deletions i rhs-pk-getter rhs-version-comparator lhs-getters join-fn)]
			   (if (or insertions alterations deletions)
			     (.notifyUpdate self insertions alterations deletions))))))]
	  (swap! mutable update-rhs
		 (map (fn [extant] (Update. nil extant))(.getExtant data))
		 nil nil i rhs-pk-getter rhs-version-comparator lhs-getters join-fn)))
      i-to-rhs-model-and-lhs-getters))

    (debug ["post-init: watching lhs:" lhs-model])
    (let [^Metadata lhs-metadata (.getMetadata lhs-model)
	  lhs-pk-getter (.getPrimaryGetter lhs-metadata)
	  lhs-version-comparator (.getVersionComparator lhs-metadata)
	  ^Data data
	  (.registerView
	   lhs-model
	   (proxy [View] []
	     (update [insertions alterations deletions]
		     (let [[_ _ insertions alterations deletions]
			   (swap! mutable update-lhs insertions alterations deletions lhs-pk-getter lhs-version-comparator i-to-lhs-getters join-fn)]
		       (if (or insertions alterations deletions)
			 (.notifyUpdate self insertions alterations deletions))))))]
      (swap! mutable update-lhs
	     (map (fn [extant] (Update. nil extant))(.getExtant data))
	     nil nil lhs-pk-getter lhs-version-comparator i-to-lhs-getters join-fn) ;; TODO - deletions/extinct
      )))

(defn -getData [^org.dada.core.JoinModel this]
  (let [[extant extinct]
	(reduce
	 (fn [[extant extinct] ^LHSEntry entry] (if (.extant entry) [(conj extant (.datum entry)) extinct] [extant (conj extinct (.datum entry))]))
	 [nil nil]
	 (.values ^Map (first @(first (.state this)))))]
    (Data. extant extinct)))

(defn -find [^org.dada.core.JoinModel this key]
  (if-let [^LHSEntry entry ((first @(first (.state this))) key)]
    (if (.extant entry)
      (.datum entry))))  

;; TODO
;; deletion
;; abstract out Index interface and provide eager and lazy implementations
;; optimise
;; tidy up and polish
;; clarify role of join-fn
;; remaining TODOs
;; compound keys

;; THOUGHTS
;; indeces should live on data model not join model - then they can be shared between many joins/splits etc...
;; outer joins ?
;; why are we so LHS oriented ? should we be ?
;; compound keys ?
;; use lists instead of entries - they may be cheaper to update
;; don't store rhs-refs on lhs, but rhs-entries (or rhs-pks?) - consider
;; make whole thing a macro which generates inline code specific to each individual join
;; compound key join ?
