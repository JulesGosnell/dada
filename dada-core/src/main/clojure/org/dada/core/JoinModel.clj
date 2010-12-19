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
   :constructors {[String Object java.util.Collection java.util.Collection org.dada.core.Model java.util.Collection java.util.Collection org.dada.core.Model] [String org.dada.core.Metadata]}
   :init init
   :state state
   :post-init post-init)
  )


;; N.B. listens to TWO (currently) Models - is NOT a View
;; why is lhs->rhs 1->1, yet rhs->lhs 1->N ?

(defn extract-attributes [keys ^Metadata metadata]
  (map
   (fn [key]
     (let [attribute (.getAttribute metadata key)]
       [key (.getType attribute) (.getMutable attribute)]))
   keys))

(defn extract-getters [keys metadata]
  (map
   (fn [key] (.getGetter (.getAttribute metadata)))
   keys))

(defn -init [^String model-name
	     join-key
	     lhs-id-keys
	     lhs-data-keys
	     ^Model lhs-model
	     rhs-id-keys
	     rhs-data-keys
	     ^Model rhs-model]
  (let [lhs-metadata (.getMetadata lhs-model)
	lhs-keys (concat lhs-id-keys lhs-data-keys)
	rhs-metadata (.getMetadata rhs-model)
	rhs-keys (concat rhs-id-keys rhs-data-keys)
	model-metadata
	(custom-metadata
	 (name (gensym "org.dada.core.Join"))
	 Object
	 (concat lhs-id-keys rhs-id-keys)
	 [:version]
	 (proxy [Metadata$Comparator][] (higher [old new] true)) ;TODO - (> (.getVersion new) (.getVersion old))
	 (concat (extract-attributes lhs-keys lhs-metadata) (extract-attributes rhs-keys rhs-metadata)))

	creator (.getCreator model-metadata) ;we are just going to copy data into new rows
	lhs-id-getters (extract-getters lhs-id-keys lhs-metadata)
	rhs-id-getters (extract-getters rhs-id-keys rhs-metadata)
	lhs-data-getters (extract-getters lhs-data-keys lhs-metadata)
	rhs-data-getters (extract-getters rhs-data-keys rhs-metadata)

	lhs-id-fn  (fn [lhs] (map (fn [getter] (.get getter lhs)) lhs-id-getters))
	rhs-id-fn  (fn [rhs] (map (fn [getter] (.get getter rhs)) rhs-id-getters))


	join-fn (fn [lhs-ids lhs rhs-ids rhs]
		  (.create creator
			   (into-array Object
				       (concat lhs-ids
					       rhs-ids
					       0;; version - TODO: look up old version that we are replacing
					       (map (fn [getter] (.get getter lhs)) lhs-data-getters)
					       (map (fn [getter] (.get getter rhs)) rhs-data-getters)))))

	lhs-unjoined {} ;; left-hand sides that have no right hand partner - indexed by left hand pk
	lhs-joined {} ;; left-hand sides that have a right hand partner - indexed by left hand pk
	rhs-unjoined {} ;; right-hand-sides that have no left-hand partner - indexed by right hand pk
	rhs-joined {} ;; right-hand sides that have one or more left hand partners  - indexed by right hand pk
	joined {} ;; joined l and r hand sides indexed by join-key
	
	]
    (info "Join: init")
    [;; super ctor args
     [model-name model-metadata]
     ;; instance state [atom(mutable) immutable]
     [(atom [lhs-unjoined lhs-joined rhs-unjoined rhs-joined joined])
      [model-metadata]]]))

(defn update-lhs [mutable immutable insertions alterations deletions]
  
  ;; (let [lhs-pk-getter(.getPrimaryGetter lhs-metadata)
  ;; 	lhs-pk-fn (fn [lhs] (.get lhs-pk-getter lhs))]

  ;;   (let [new-lhs (.getNew update)
  ;; 	  lhs-pk (lhs-pk-fn new-lhs)
  ;; 	  old-joined (by-lhs-pk lhs-pk)]
  ;;     (if old-joined
  ;; 	(lhs-rejoin old-joined new-lhs)
  ;; 	;; add to lhs-unjoined
  ;; 	)
      ;; update by-lhs
      ;; update by-rhs
      ;; update joined
  ;;      )
  
  ;; get lhs-pk
  ;; pull lhs-extant-joins with this key
  ;; rejoin them
  ;; update lhs-extant-joins
  ;; if join-key is mutable and it has changed value will have to update rhs-extant-joins with old and new value

  ;;(info (str "Join: lhs update " insertions " " alterations " " deletions))

  ;; need
  ;; lhs-to-rhs
  ;; lhs-id-fn
  ;; rejoin-lhs
  ;; extant-joins
  
  ;; foreach insertion/alteration
  ;; (let [new-lhs (.getNew update)
  ;; 	old-joins (lhs-to-rhs (lhs-id-fn new-lhs))
  ;; 	new-joins (rejoin-lhs new-lhs old-joins)]
  ;;   ;; update old-joins
  ;;   ;; update extant-joins
  
  )

(defn update-rhs [mutable immutable insertions alterations deletions]
  (info (str "Join: rhs update " insertions " " alterations " " deletions)))

(defn -post-init [^org.dada.core.JoinModel this _ _ _ _ lhs-model _ _ rhs-model]
   (.registerView
    lhs-model
    (proxy [View] []
      (update [insertions alterations deletions]
	      (let [[mutable immutable] (.state this)]
		(swap! mutable update-lhs immutable insertions alterations deletions)))))
  (.registerView
   rhs-model
   (proxy [View] []
     (update [insertions alterations deletions]
	     (let [[mutable immutable] (.state this)]
	       (swap! mutable update-rhs immutable insertions alterations deletions)))))
  )

(defn -getData [^org.dada.core.JoinModel this]
  (info "Join: getData")
  )

;;--------------------------------------------------------------------------------
