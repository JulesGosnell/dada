(ns 
 org.dada.core.dql2
 (:use [org.dada.core])
 (:import
  [org.dada.core
   Getter
   Metadata
   Metadata$VersionComparator
   Model
   Result2
   MetaResult2
   View])
 )

;;--------------------------------------------------------------------------------
;; a fresh approach :

;; e.g. Whales is the universal set
;; all splits are nested
;; union un-nests
;; does this work ?
;;--------------------------------------------------------------------------------

(def #^Metadata result-metadata
     (custom-metadata3 org.dada.core.Result
		       [:model]	   ;TODO - this is a BAD idea for a PK
		       [:model]	;TODO - we need a proper version - or code that can handle versionless data
		       (proxy [Metadata$VersionComparator][](compareTo [lhs rhs] -1))
		       [[:model org.dada.core.Model false]
			[:prefix String false]
			[:pairs java.util.Collection false]
			[:operation java.util.Collection false]]))

;;--------------------------------------------------------------------------------

(defmulti extract (fn [arg getter] (class arg)))

(defmethod extract java.util.Collection [[metadata-fn data-fn] getter]
  (extract (data-fn) getter))

(defmethod extract org.dada.core.Result2 [#^Result2 result getter]
  (extract (first (.getModelList result)) getter))

(defmethod extract org.dada.core.Model [#^Model model getter]
  (reduce (fn [total row] (conj total (extract row getter))) #{} (.getExtant (.getData model))))

(defmethod extract :default [datum #^Getter getter]
  (.get getter datum))

;;--------------------------------------------------------------------------------
;; dfrom

(defn get-prefix [#^String model-name]
  model-name)

(defmulti dfrom (fn [arg] (class arg)))

(defmethod dfrom java.lang.String [#^String model-name]
   (dfrom (.find *metamodel* model-name)))

(defmethod dfrom org.dada.core.Model [#^Model model]
   [(fn [] (MetaResult2. [(.getMetadata model)] [])) ;; metadata
    (fn [] (Result2. [model] []))]);; data

;;--------------------------------------------------------------------------------

(defn dunion []
     (fn [[src-metadata-fn src-data-fn]]
	 (let [[[_ & src-metadata-list] [_ & src-keys]](src-metadata-fn)
	       tgt-metadata (first src-metadata-list)]
	   [(fn []
		(MetaResult2. (cons tgt-metadata src-metadata-list) src-keys))
	    (fn []
		(let [[src-models src-pairs] (src-data-fn)
		      src-model (first src-models)
		      tgt-name "TODO" ;TODO: should strip off last split - and test to see if already exists
		      tgt-model (model tgt-name tgt-metadata)]
		  ;; insert tgt-model into metamodel ?
		  (connect
		   src-model
		   (proxy [View][]
			  (update [i a d]
				  ;; these should crack open into further models
				  ;; view them and dump contents into tgt-model
				  )))
		  (Result2. (cons tgt-model) (cons (map vector keys) src-pairs))))])))

;;--------------------------------------------------------------------------------

(defn dsplit [keys & [function]]
  (fn [[src-metadata-fn src-data-fn]]
      (let [[src-metadata-list src-keys](src-metadata-fn)
	    tgt-metadata result-metadata]
	[(fn []
	     (MetaResult2. (cons tgt-metadata src-metadata-list) (cons keys src-keys)))
	 (fn []
	     (let [[src-models src-pairs] (src-data-fn)
		   src-model (first src-models)
		   tgt-name [:split keys function]
		   tgt-model (model (pr-str tgt-name) tgt-metadata)
		   src-metadata (first src-metadata-list)
		   submodels (atom {})]
	       ;; insert tgt-model into metamodel ?
	       (connect
		src-model
		(proxy [View][]
		       (update [i a d]
			       ;; handle updates
			       ;; group them and insert them into submodels
			       ;; create submodels on fly and insert them into tgt model as Results
			       )))
	       (Result2. (cons tgt-model) (cons (map vector keys) src-pairs))))])))
  
;;--------------------------------------------------------------------------------


