(ns org.dada.core.JoinModel
  (:use
   [clojure.tools logging]
   [org.dada core]
   [org.dada.core counted-set utils map]
   )
  (:import
   [java.util Collection Map HashMap]
   [org.dada.core Attribute Data Getter Metadata Metadata$VersionComparator Model RemoteModel Tuple Update View]
   )
  (:gen-class
   :implements [org.dada.core.Model java.io.Serializable]
   :constructors {[String org.dada.core.Metadata org.dada.core.Model java.util.Map clojure.lang.IFn] []}
   :methods [[writeReplace [] Object]]
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

;; ;; TODO - we need a new key function to generate ref-based keys
;; (defn get-key-fn [^Metadata metadata keys]
;;   (let [keys (map (fn [key] (symbol (name key))) keys)
;;         getter (make-key-getter (symbol ]
;;     ))
   
(defn invert-map [m]
  (reduce (fn [r [k v]] (assoc r v (conj (r v) k))) {} m))

(defn make-notification [old-datum new-datum old-insertions old-alterations old-deletions delete]
  (trace ["make-notification" old-datum new-datum old-insertions old-alterations old-deletions delete])
  (if new-datum
    (if old-datum
      (if delete
	[old-insertions old-alterations (conj old-deletions (Update. old-datum nil))] ;deletion
	[old-insertions (conj old-alterations (Update. old-datum new-datum)) old-deletions]) ;alteration
      [(conj old-insertions (Update. old-datum new-datum)) old-alterations old-deletions]) ;insertion
    [old-insertions old-alterations old-deletions])) ;no change

(defn -init [^String model-name
	     model-metadata
	     ^Model lhs-model
	     rhses
	     join-fn]
  [ ;; super ctor args
   []
   ;; instance state [atom(mutable) immutable]
   (let [lhs-metadata (.getMetadata lhs-model)
	 views nil
	 lhs-mutable (map-new)
	 rhs-mutables (vec-new (repeatedly (count (invert-map rhses)) map-new))]
     [(atom [views lhs-mutable rhs-mutables]) model-name model-metadata])])

;;--------------------------------------------------------------------------------

(defn update-lhs-rhs-indeces [lhs-pk old-lhs new-lhs rhs-i-to-lhs-getters old-rhs-refs old-rhs-indeces delete]
  (trace ["update-lhs-rhs-indeces" rhs-i-to-lhs-getters old-rhs-indeces])
  (reduce
   (fn [[old-rhs-refs-1 old-rhs-indeces-1] [i lhs-getters rhs-index]]
     (reduce
      (fn [[old-rhs-refs-2 old-rhs-indeces-2] [j ^Getter lhs-getter]]
	(trace [" update-lhs-rhs-indeces: inputs" old-rhs-refs-2 old-rhs-indeces-2])
	(let [old-rhs-pk (if old-lhs (.get lhs-getter old-lhs))
	      new-rhs-pk (.get lhs-getter new-lhs)
	      dummy (trace ["old-rhs-pk new-rhs-pk" old-rhs-pk new-rhs-pk])]
	  (if (and (not delete) (= old-rhs-pk new-rhs-pk))
	    (do
	      (trace [" update-lhs-rhs-indeces: unchanged" i old-rhs-pk])
	      [ old-rhs-refs-2 old-rhs-indeces-2])
	    (let [old-rhs-index (vec-get old-rhs-indeces-2 i)
		  ;; remove lhs from old-rhs-pks (if necessary) - TODO - tidy up
		  dummy (trace ["old-rhs-pk" old-rhs-pk old-rhs-index])
		  tmp-rhs-index (if-let [^RHSEntry old-old-rhs-entry (map-get old-rhs-index old-rhs-pk)]
				  (let [old-lhs-pks (.lhs-pks old-old-rhs-entry)
					new-lhs-pks (vec-set old-lhs-pks j (disj (vec-get old-lhs-pks j) lhs-pk))
					new-old-rhs-entry (RHSEntry. (.rhs old-old-rhs-entry) new-lhs-pks)
					dummy (trace ["new-old-rhs-entry" new-old-rhs-entry])
					tmp-rhs-index (map-put old-rhs-index old-rhs-pk new-old-rhs-entry)]
				    tmp-rhs-index)
				  old-rhs-index)
		  ;; add lhs to new-rhs-pks
		  [rhs new-rhs-index] (if delete ;we have already deleted rhs backptrs - do no more
					[nil tmp-rhs-index]
					(let [^RHSEntry old-new-rhs-entry (map-get tmp-rhs-index new-rhs-pk)
					    [rhs old-lhs-pks] (if old-new-rhs-entry
								[(.rhs old-new-rhs-entry) (.lhs-pks old-new-rhs-entry)]
								[nil
								 ;; TODO - should use templated value
								 (vec-new (repeatedly (count lhs-getters) hash-set))])
					    new-lhs-pks (vec-set old-lhs-pks j (conj (vec-get old-lhs-pks j) lhs-pk))
					    new-new-rhs-entry (RHSEntry. rhs new-lhs-pks)
					    dummy (trace ["new-new-rhs-entry" new-new-rhs-entry])
					    new-rhs-index (map-put tmp-rhs-index new-rhs-pk new-new-rhs-entry)]
					[rhs new-rhs-index]))
		  ;; update rhs-indeces
		  new-rhs-indeces (vec-set old-rhs-indeces-2 i new-rhs-index)
		  ;; update rhs-refs
		  new-rhs-refs (vec-set old-rhs-refs-2 i (vec-set (vec-get old-rhs-refs-2 i) j rhs))]
	      (trace [" update-lhs-rhs-indeces: changed" i old-rhs-pk "->" new-rhs-pk])
	      [new-rhs-refs new-rhs-indeces]))))
      [old-rhs-refs-1 old-rhs-indeces-1]
      (map (fn [j lhs-getter] [j lhs-getter]) (range) lhs-getters))
     )
   [old-rhs-refs old-rhs-indeces]
   (map (fn [[i lhs-getter] rhs-index] [i lhs-getter rhs-index]) rhs-i-to-lhs-getters (vec-seq old-rhs-indeces)))
  )

(defn reduce-lhs [reduction updates ^Getter lhs-pk-getter initial-rhs-refs ^Metadata$VersionComparator lhs-version-comparator rhs-i-to-lhs-getters join-fn delete]
  (reduce
   (fn [[old-lhs-index old-rhs-indeces old-insertions old-alterations old-deletions] ^Update update]
     (let [new-lhs (.getNewValue update)
	   latest-lhs (or new-lhs (.getOldValue update))
	   lhs-pk (.get lhs-pk-getter latest-lhs)
	   ^LHSEntry crt-lhs-entry (map-get old-lhs-index lhs-pk)
	   [crt-lhs-entry-version crt-lhs-entry-extant crt-lhs-entry-rhs-refs crt-lhs]
	   (if crt-lhs-entry
	     [(.version crt-lhs-entry)(.extant crt-lhs-entry)(.rhs-refs crt-lhs-entry)(.lhs crt-lhs-entry)]
	     [-1 false initial-rhs-refs nil])]
       (if (and crt-lhs-entry (not ((if delete <= <) (.compareTo lhs-version-comparator crt-lhs (if delete latest-lhs new-lhs)) 0)))
	 (do
	   (trace ["update-lhs - alteration - rejected" crt-lhs new-lhs])
	   [old-lhs-index old-rhs-indeces old-insertions old-alterations old-deletions])
	 (let [[new-rhs-refs new-rhs-indeces]
	       (update-lhs-rhs-indeces lhs-pk (if crt-lhs-entry-extant crt-lhs nil) new-lhs rhs-i-to-lhs-getters crt-lhs-entry-rhs-refs old-rhs-indeces delete)
	       new-lhs-version (if delete crt-lhs-entry-version (inc crt-lhs-entry-version))
	       crt-entry-datum (if crt-lhs-entry (.datum crt-lhs-entry))
	       new-datum (if delete crt-entry-datum (join-fn lhs-pk new-lhs-version new-lhs new-rhs-refs))
	       dummy (trace [new-datum crt-lhs-entry])
	       new-lhs-entry (LHSEntry. (not delete) new-lhs-version (if delete (or new-lhs crt-lhs) new-lhs) new-rhs-refs new-datum)
	       new-lhs-index (map-put old-lhs-index lhs-pk new-lhs-entry)
	       [new-insertions new-alterations new-deletions] (make-notification (if crt-lhs-entry-extant crt-entry-datum nil) new-datum old-insertions old-alterations old-deletions delete)]
	   (trace ["update-lhs - insertion/alteration - accepted" crt-lhs new-lhs])
	   [new-lhs-index new-rhs-indeces new-insertions new-alterations new-deletions]
	   ))))
   reduction
   updates))

(defn update-lhs
  "return new mutable state and events in response to notifications from the left hand side model"
  [[views old-lhs-index old-rhs-indeces] insertions alterations deletions ^Getter lhs-pk-getter ^Metadata$VersionComparator lhs-version-comparator rhs-i-to-lhs-getters join-fn initial-rhs-refs]
  (trace ["update-lhs" rhs-i-to-lhs-getters])
  (apply vector views
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
  (trace ["update-rhs-lhs-index" old-lhs-index lhs-pks-list new-rhs])
  (reduce
   (fn [[old-lhs-index old-insertions old-alterations old-deletions] [j lhs-pks]]
     (reduce
      (fn [[old-lhs-index old-insertions old-alterations old-deletions] lhs-pk]
	(let [^LHSEntry old-lhs-entry (map-get old-lhs-index lhs-pk)
	      old-lhs-rhs-refs (.rhs-refs old-lhs-entry)
	      new-lhs-rhs-refs (vec-set old-lhs-rhs-refs i (vec-set (vec-get old-lhs-rhs-refs i) j new-rhs))
	      new-lhs-version (inc (.version old-lhs-entry))
	      lhs (.lhs old-lhs-entry)
	      old-datum (if old-lhs-entry (.datum old-lhs-entry))
	      new-datum (join-fn lhs-pk new-lhs-version lhs new-lhs-rhs-refs)
	      [new-insertions new-alterations new-deletions] (make-notification old-datum new-datum old-insertions old-alterations old-deletions false) ;TODO
	      new-lhs-index (map-put old-lhs-index lhs-pk (LHSEntry. (.extant old-lhs-entry) new-lhs-version lhs new-lhs-rhs-refs new-datum))]
	  [new-lhs-index new-insertions new-alterations new-deletions]))
      [old-lhs-index old-insertions old-alterations old-deletions]
      lhs-pks))
   [old-lhs-index old-insertions old-alterations old-deletions]
   (map (fn [j lhs-pks][j lhs-pks]) (range) (vec-seq lhs-pks-list))))

(defn update-rhs
  "return new mutable state and events in response to notifications from a right hand side model"
  [[views old-lhs-index old-rhs-indeces] insertions alterations deletions i ^Getter rhs-pk-getter ^Metadata$VersionComparator rhs-version-comparator lhs-getters join-fn]
  (let [initial-lhs-pks (fn [] (vec-new (repeatedly (count lhs-getters) hash-set))) ;TODO - do not rebuild each time...
	old-rhs-index (vec-get old-rhs-indeces i)]
    (trace ["update-rhs" i lhs-getters old-rhs-index])

    ;; insertions/alterations
    (let [[new-lhs-index new-rhs-index new-insertions new-alterations new-deletions]
	  (reduce
	   (fn [[old-lhs-index old-rhs-index old-insertions old-alterations old-deletions] ^Update update]
	     (let [new-rhs (.getNewValue update)
		   rhs-pk (.get rhs-pk-getter new-rhs)]
	       (if-let [^RHSEntry old-rhs-entry (map-get old-rhs-index rhs-pk)]
		 (let [old-rhs (.rhs old-rhs-entry)]
		   (if (or (nil? old-rhs) (< (.compareTo rhs-version-comparator old-rhs new-rhs) 0))
		     (let [new-rhs-entry (RHSEntry. new-rhs (if old-rhs-entry (.lhs-pks old-rhs-entry) (initial-lhs-pks)))
			   new-rhs-index (map-put old-rhs-index rhs-pk new-rhs-entry)
			   [new-lhs-index new-insertions new-alterations-new-deletions]
			   (update-rhs-lhs-index old-lhs-index (.lhs-pks old-rhs-entry) i new-rhs join-fn old-insertions old-alterations old-deletions)]
		       (trace ["update-rhs - alteration - accepted" old-rhs new-rhs])
		       [new-lhs-index new-rhs-index new-insertions new-alterations-new-deletions])
		     (do
		       (trace ["update-rhs - alteration - rejected" old-rhs new-rhs])
		       [old-lhs-index old-rhs-index old-insertions old-alterations old-deletions])))
		 (let [new-rhs-entry (RHSEntry. new-rhs (initial-lhs-pks))
		       new-rhs-index (map-put old-rhs-index rhs-pk new-rhs-entry)]
		   (trace ["update-rhs - insertion" new-rhs])
		   [old-lhs-index new-rhs-index old-insertions old-alterations old-deletions]))))
	   [old-lhs-index old-rhs-index nil nil nil]
	   (concat insertions alterations))
	  new-rhs-indeces (vec-set old-rhs-indeces i new-rhs-index)]
      [views new-lhs-index new-rhs-indeces new-insertions new-alterations new-deletions])
    
    ;; deletions - TO DO
    
    ))

(defn extract-data [^Map lhs]
  (let [[extant extinct]
	(reduce
	 (fn [[extant extinct] ^LHSEntry entry]
	     (if (.extant entry) [(conj extant (.datum entry)) extinct] [extant (conj extinct (.datum entry))]))
	 [nil nil]
	 (.values lhs))]
    (Data. extant extinct)))

(defn -getName [^org.dada.core.JoinModel this]
  (second (.state this)))

(defn -getMetadata [^org.dada.core.JoinModel this]
  (nth (.state this) 2))

(defn -find [^org.dada.core.JoinModel this key]
  (if-let [^LHSEntry entry (map-get (second @(first (.state this))) key)]
    (if (.extant entry)
      (.datum entry))))

;;------------------------------------------------------------------------------
;; TODO - how can we better share this code with SimpleModelView ?

(defn ^Data -attach [^org.dada.core.JoinModel this ^View view]
  (map-locking
   this
   (let [[mutable] (.state this)]
     (let [[views lhs] (swap! mutable (fn [m view] (assoc m 0 (counted-set-inc (m 0) view))) view)]
       (debug ["ATTACH VIEW: " view views])
       (extract-data lhs)))))

(defn ^Data -detach [^org.dada.core.JoinModel this ^View view]
  (map-locking
   this
   (let [[mutable] (.state this)]
     (let [[views lhs] (swap! mutable (fn [m view] (assoc m 0 (counted-set-dec (m 0) view))) view)]
       (debug ["DETACH VIEW: " view views])
       (extract-data lhs)))))

(defn notifyUpdate [^org.dada.core.JoinModel this insertions alterations deletions]
  (map-locking
   this
   (let [[mutable] (.state this)
	 [views] @mutable]
     (trace ["NOTIFY ->" @mutable])
     (if (and (empty? insertions) (empty? alterations) (empty? deletions))
       (warn "empty event raised" (.getStackTrace (Exception.)))
       (dorun (map (fn [^View view]	;dirty - side-effects
		       (try (.update view insertions alterations deletions)
			    (catch Throwable t
				   (error "View notification failure" t)
				   (.printStackTrace t))))
		   (counted-set-vals views)))))))

;;------------------------------------------------------------------------------

(defn -getData [^org.dada.core.JoinModel this]
  (extract-data (second @(first (.state this)))))

;;------------------------------------------------------------------------------

(defn ^{:private true} -writeReplace [^org.dada.core.JoinModel this]
  (map-locking
   this
   (let [[_mutable name metadata] (.state this)]
     (RemoteModel. name metadata))))

;;--------------------------------------------------------------------------------

(defn -post-init [^org.dada.core.JoinModel self ^String model-name _ ^Model lhs-model rhses join-fn]
  (map-locking
   self
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
	   (trace ["post-init: watching rhs:" i rhs-model])
	   (let [^Metadata rhs-metadata (.getMetadata rhs-model)
		 rhs-pk-getter (.getPrimaryGetter rhs-metadata)
		 rhs-version-comparator (.getVersionComparator rhs-metadata)
		 ^Data data
		 (.attach
		  rhs-model
		  (proxy [View] []
			 (update [insertions alterations deletions]
				 (let [[_ _ _ insertions alterations deletions]
				       (swap! mutable update-rhs insertions alterations deletions i rhs-pk-getter rhs-version-comparator lhs-getters join-fn)]
				   (if (or insertions alterations deletions)
				     (notifyUpdate self insertions alterations deletions))))
			 (toString [] (print-object this (str "Proxy:" model-name)))))]
	     (swap! mutable update-rhs
		    (map (fn [extant] (Update. nil extant))(.getExtant data))
		    nil nil i rhs-pk-getter rhs-version-comparator lhs-getters join-fn)))
       i-to-rhs-model-and-lhs-getters))

     (trace ["post-init: watching lhs:" lhs-model])
     (let [initial-rhs-refs (vec-new (map (fn [[k vs]] (vec-new (map (fn [v] nil) vs))) rhs-model-to-lhs-fks))
	   ^Metadata lhs-metadata (.getMetadata lhs-model)
	   lhs-pk-getter (.getPrimaryGetter lhs-metadata)
	   lhs-version-comparator (.getVersionComparator lhs-metadata)
	   ^Data data
	   (.attach
	    lhs-model
	    (proxy [View] []
		   (update [insertions alterations deletions]
			   (let [[_ _ _ insertions alterations deletions]
				 (swap! mutable update-lhs insertions alterations deletions lhs-pk-getter lhs-version-comparator i-to-lhs-getters join-fn initial-rhs-refs)]
			     (if (or insertions alterations deletions)
			       (notifyUpdate self insertions alterations deletions))))))]
       (swap! mutable update-lhs
	      (map (fn [extant] (Update. nil extant))(.getExtant data))
	      nil nil lhs-pk-getter lhs-version-comparator i-to-lhs-getters join-fn initial-rhs-refs) ;; TODO - deletions/extinct
       ))))

(defn ^String -toString [^org.dada.core.JoinModel this]
  (let [[_ name] (.state this)]
    (print-object this name)))

;;------------------------------------------------------------------------------
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
