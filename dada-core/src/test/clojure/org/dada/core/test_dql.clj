(ns org.dada.core.test-dql
    (:use 
     [clojure test]
     [org.dada core]
     [org.dada.core dql])
    (:import
     [org.dada.core
      Attribute
      Model
      Result])
    )

;;--------------------------------------------------------------------------------
;; THOUGHTS:
;; we should be able to test serially and concurrently (using exclusive lock)

;;--------------------------------------------------------------------------------
;; data model

(def whale-attributes
     (list
      [:id       (Integer/TYPE) false]
      [:version  (Integer/TYPE) true]
      ;;[:time     Date           true]
      ;;[:reporter String         true]
      [:type     clojure.lang.Keyword         false]	;a whale cannot change type
      [:ocean    clojure.lang.Keyword         true]
      [:length   (Float/TYPE)   true]
      ;;[:weight   (Float/TYPE)   true]
      ))

(def whale-data
     [[0 0 :blue :atlantic 100]
      [1 0 :blue :pacific  100]
      [2 0 :grey :atlantic 50]
      [3 0 :grey :pacific  50]])

;;(def whale-metadata (seq-metadata (count (first whale-data))))
(def whale-metadata (custom-metadata "org.dada.core.tmp.Whale" 
				     Object
				     [:id]
				     [:version] 
				     int-version-comparator 
				     whale-attributes))

(def whales (model "Whales" whale-metadata))
(insert *metamodel* whales)

;;(insert-n whales whale-data)
(let [creator (.getCreator whale-metadata)]
      (insert-n
       whales
       (map (fn [datum] (.create creator (into-array Object datum))) whale-data)))

;;--------------------------------------------------------------------------------
;; utils

(defn reduction-value [[metadata-fn data-fn]]
  (let [[#^Model metamodel] (data-fn)
	#^Model model (.getModel (first (.getExtant (.getData metamodel))))
	value (first (.getExtant (.getData model)))]
    (.get (.getGetter #^Attribute (nth (.getAttributes (.getMetadata model)) 1)) value)))

(defn to-list [#^Whale whale]
  (map (fn [#^Attribute attribute] (.get (.getGetter attribute) whale)) (.getAttributes whale-metadata)))

(defn flat-split-values [[metadata-fn data-fn]]
  (reduce
   (fn [values #^Result result]
       ;;(println (.getModel result) (.getPrefix result) "," (.getPairs result) "," (.getOperation result))
       (conj values [(map second (.getPairs result)) 
		     (map to-list (.getExtant (.getData (.getModel result))))]))
   {}
   (.getExtant (.getData (.getModel (data-fn))))))

(defn valid-pairs [pairs]
  ;;(println "VALID PAIRS" pairs)
  (reduce (fn [current pair] (if (= (count pair) 2) (conj current pair) current)) '() pairs))

(defn nested-split-values2 [result]
  (let [[#^Model model prefix pairs operation] result
	key (second (first (valid-pairs pairs)))
	values (.getExtant (.getData model))]
    [[key]
     (if (every? (fn [value] (instance? Result value)) values) ;TODO - is this the best I can do ?
       (reduce (fn [map value] (conj map (nested-split-values2 value))) {} values)
       (map to-list values))]))

(defn nested-split-values [[metadata-fn data-fn]]
    (second (nested-split-values2 (data-fn))))

(defn flat-group-by [fns data]
  (group-by (apply juxt fns) whale-data))

(defn nested-group-by [[fns-head & fns-tail] data]
  (reduce (fn [current [key val]] (conj current [key (if fns-tail (nested-group-by fns-tail val) val)])) {} (group-by (juxt fns-head) data)))

;;--------------------------------------------------------------------------------
;; tests

(deftest test-dfrom
  (let [tuple0 (? (dfrom "Whales"))
	metaresult0 ((first tuple0))
	result0 ((second tuple0))
	model0 (.getModel result0)]
    (is (= (.getMetadata metaresult0) result-metadata))
    (is (= (first (.getOperation metaresult0)) :from))
    (is (= (.getPairs result0) []))
    (is (= (first (.getOperation result0)) :from))
    (let [extant1 (.getExtant (.getData model0))]
      (is (= (count extant1) 1))
      (let [result1 (first extant1)
	    model1 (.getModel result1)]
	(is (= (.getChildMetadata metaresult0) (.getMetadata model1)))
	(is (= model1 whales))
	(is (= (.getPrefix result1) "Whales"))
	(is (= (.getPairs result1) []))
	(is (= (first (.getOperation result1)) :from))
	))))

(deftest test-dunion
  (let [tuple0 (? (dunion)(dfrom "Whales"))
	metaresult0 ((first tuple0))
	result0 ((second tuple0))
	model0 (.getModel result0)]
    (is (= (.getMetadata metaresult0) result-metadata))
    (is (= (first (.getOperation metaresult0)) :union))
    (is (= (.getPairs result0) []))
    (is (= (first (.getOperation result0)) :union))
    (let [extant1 (.getExtant (.getData model0))]
      (is (= (count extant1) 1))
      (let [result1 (first extant1)
	    model1 (.getModel result1)]
	(is (= (.getMetadata model1) (.getChildMetadata metaresult0)))
	(is (= (.getMetadata model1) whale-metadata))
	;;(is (= (.getPrefix result1) "Whales"))
	(is (= (.getPairs result1) []))
	(is (= (first (.getOperation result1)) :union))
	))))

(deftest test-dcount
  (is (= (reduction-value (? (dcount)(dfrom "Whales")))
	 (count whale-data))))

(deftest test-dsum
  (is (= (reduction-value (? (dsum :length)(dfrom "Whales")))
	 (reduce (fn [total whale] (+ total (nth whale 4))) 0 whale-data))))

;; one dimension -  {[[key][whale...]]...}
(deftest test-split-1d
  (is (= (flat-split-values (? (dsplit :type)(dfrom "Whales")))
	 (flat-group-by [(fn [[_ _ type]] type)] whale-data))))

(deftest test-split-1d-2
  (let [tuple0 (? (dsplit :type)(dfrom "Whales"))
	metaresult0 ((first tuple0))
	result0 ((second tuple0))
	model0 (.getModel result0)]
    ;; check outer result.
    (is (= (.getMetadata metaresult0) (.getMetadata model0) result-metadata))
    (is (=
	 ;;(.getPairs metaresult0)
	 (.getPairs result0) [[:type]]))
    (is (= (first (.getOperation result0)) :split))
    (let [extant1 (.getExtant (.getData model0))
	  type-to-whales (group-by (fn [[_ _ type]] type) whale-data)]
      (is (= (count extant1) 2))
      (is (= (count type-to-whales) 2))
      ;; check inner results
      (doall
       (map
	(fn [[model prefix pairs operation]]
	    (is (= (.getMetadata model) (.getChildMetadata metaresult0) whale-metadata))
	    (is (= (count pairs) 1))
	    (is (first (first pairs)) :type)
	    (let [getters (map (fn [attribute] (.getGetter attribute)) (.getAttributes (.getMetadata model)))]
	      ;; check correct whales are present
	      (is (=
		   ;; make a list of lists of values extracted from model's whales
		   (map (fn [whale] (map (fn [getter] (.get getter whale)) getters))(.getExtant (.getData model)))
		   ;; compare to whale-data of this type
		   (type-to-whales (second (first pairs))))))
	    (is (= (first operation) :split))
	    )
	extant1
	))
      )))

(deftest test-union-split-1d
  (let [tuple0 (? (dunion)(dsplit :type)(dfrom "Whales"))
	metaresult0 ((first tuple0))
	result0 ((second tuple0))
	model0 (.getModel result0)]
    (is (= (.getPairs result0) []))
    (is (= (first (.getOperation result0)) :union))
    (let [extant1 (.getExtant (.getData model0))]
      (is (= (count extant1) 1))
       (let [result1 (first extant1)
	     model1 (.getModel result1)
	     extant (.getExtant (.getData model1))]
	 (is (= (.getMetadata model1) (.getChildMetadata metaresult0) whale-metadata))
	 (is (= (count extant) 4))
	 (is  (= (reduce (fn [set whale] (conj set (.getId whale))) #{} extant) #{0 1 2 3}))
	 ;;(is (= (.getPrefix result1) "Whales"))
	 (is (= (.getPairs result1) []))
	 (is (= (first (.getOperation result1)) :union))
	 )
      )))

(deftest test-count-split-1d
  (let [tuple0 (? (dcount)(dsplit :type)(dfrom "Whales"))
	metaresult0 ((first tuple0))
	result0 ((second tuple0))
	model0 (.getModel result0)]
    ;; (is (= (.getPairs result0) []))
    ;; (is (= (first (.getOperation result0)) :union))
    ;; (let [extant1 (.getExtant (.getData model0))]
    ;;   (is (= (count extant1) 1))
    ;;    ;; (let [result1 (first extant1)
    ;;    ;; 	     model1 (.getModel result1)
    ;;    ;; 	     extant (.getExtant (.getData model1))]
    ;;    ;; 	 (is (= (.getMetadata model1) (.getChildMetadata metaresult0) whale-metadata))
    ;;    ;; 	 (is (= (count extant) 4))
    ;;    ;; 	 (is  (= (reduce (fn [set whale] (conj set (.getId whale))) #{} extant) #{0 1 2 3}))
    ;;    ;; 	 ;;(is (= (.getPrefix result1) "Whales"))
    ;;    ;; 	 (is (= (.getPairs result1) []))
    ;;    ;; 	 (is (= (first (.getOperation result1)) :union))
    ;;    ;; 	 )
    ;;   )
    ))

(deftest test-sum-split-1d
  (let [tuple0 (? (dsum :length)(dsplit :type)(dfrom "Whales"))
	metaresult0 ((first tuple0))
	result0 ((second tuple0))
	model0 (.getModel result0)]
    ;; (is (= (.getPairs result0) []))
    ;; (is (= (first (.getOperation result0)) :union))
    ;; (let [extant1 (.getExtant (.getData model0))]
    ;;   (is (= (count extant1) 1))
    ;;    ;; (let [result1 (first extant1)
    ;;    ;; 	     model1 (.getModel result1)
    ;;    ;; 	     extant (.getExtant (.getData model1))]
    ;;    ;; 	 (is (= (.getMetadata model1) (.getChildMetadata metaresult0) whale-metadata))
    ;;    ;; 	 (is (= (count extant) 4))
    ;;    ;; 	 (is  (= (reduce (fn [set whale] (conj set (.getId whale))) #{} extant) #{0 1 2 3}))
    ;;    ;; 	 ;;(is (= (.getPrefix result1) "Whales"))
    ;;    ;; 	 (is (= (.getPairs result1) []))
    ;;    ;; 	 (is (= (first (.getOperation result1)) :union))
    ;;    ;; 	 )
    ;;   )
    ))

(deftest misc-queries
  (? (dunion)(dcount)(dsplit :type)(dfrom "Whales"))
  (? (dunion)(dcount)(dsplit :type )(dsplit :ocean)(dfrom "Whales"))
  (? (dsum :length)(dsplit :type )(dsplit :ocean)(dfrom "Whales"))
  )

;; 2 dimensions - flat - {[[key1 key2][whale...]]...}
(deftest test-flat-split-2d
  (is (= (flat-split-values (? (dsplit :ocean)(dsplit :type)(dfrom "Whales")))
	 (flat-group-by [(fn [[_ _ type]] type)(fn [[_ _ _ ocean]] ocean)] whale-data))))

;; copy of test-union-split-1d
(deftest test-union-flat-split-2d
  (let [tuple0 (? (dunion)(dsplit :ocean)(dsplit :type)(dfrom "Whales"))
	metaresult0 ((first tuple0))
	result0 ((second tuple0))
	model0 (.getModel result0)]
    (is (= (.getMetadata metaresult0) result-metadata))
    (is (= (.getPairs result0) []))
    (is (= (first (.getOperation result0)) :union))
    (let [extant1 (.getExtant (.getData model0))]
      (is (= (count extant1) 1))
       (let [result1 (first extant1)
	     model1 (.getModel result1)
	     extant (.getExtant (.getData model1))]
	 (is (= (.getMetadata model1) (.getChildMetadata metaresult0) whale-metadata))
	 (is (= (count extant) 4))
	 (is  (= (reduce (fn [set whale] (conj set (.getId whale))) #{} extant) #{0 1 2 3}))
	 ;;(is (= (.getPrefix result1) "Whales"))
	 (is (= (.getPairs result1) []))
	 (is (= (first (.getOperation result1)) :union))
	 )
      )))

;;2 dimensions - nested - a map of {[[key1]{[[key2][whale...]]}]...}
(deftest test-nested-split-2d
  (is (= (nested-split-values (? (dsplit :type list [(dsplit :ocean)])(dfrom "Whales")))
	 (nested-group-by [(fn [[_ _ type]] type)(fn [[_ _ _ ocean]] ocean)] whale-data))))

(deftest test-nested-split-2d
  (let [tuple0 (? (dsplit :type list [(dsplit :ocean)])(dfrom "Whales"))
	metaresult0 ((first tuple0))
	result0 ((second tuple0))
	model0 (.getModel result0)]
    (is (= (.getMetadata metaresult0) result-metadata))
    (is (= (.getPairs result0) [[:type]]))
    (is (= (first (.getOperation result0)) :split))
    (let [extant1 (.getExtant (.getData model0))]
      (is (= (count extant1) 2))
      (doall
       (map
	(fn [[model1 prefix1 pairs1 operation1]]
	    (is (= (count pairs1) 2))
	    (let [metadata1 (.getMetadata model1)
		  [type-pair ocean-pair] pairs1]
	      (is (= metadata1 (.getChildMetadata metaresult0)))
	      (is (= metadata1 result-metadata))
	      (is (= (count type-pair) 2))
	      (is (= (first type-pair) :type))
	      (is (contains? #{:blue :grey} (second type-pair)))
	      (is (= (count ocean-pair) 1))
	      (is (= (first ocean-pair) :ocean))
	      (is (= (first operation1) :split))
	      (let [extant2 (.getExtant (.getData model1))]
		(is (= (count extant2) 2))
		(doall
		 (map
		  (fn [[model2 prefix2 pairs2 operation2]]
		      (is (= (count pairs2) 2))
		      (let [metadata2 (.getMetadata model2)
			    [type-pair ocean-pair] pairs2]
			(is (= metadata2 whale-metadata))
			(is (= (count type-pair) 2))
			(is (= (first type-pair) :type))
			(is (contains? #{:blue :grey} (second type-pair)))
			(is (= (count ocean-pair) 2))
			(is (= (first ocean-pair) :ocean))
			(is (contains? #{:atlantic :pacific} (second ocean-pair)))
			(is (= (first operation2) :split))
			(let [extant3 (.getExtant (.getData model2))
			      whale (first extant3)]
			  (is (= (count extant3) 1))
			  (is (= (.getType whale) (second type-pair)))
			  (is (= (.getOcean whale) (second ocean-pair)))
			  )
			)     
		      )
		  extant2)))
	      ))
	extant1)))))


(defn print-keys [metadata]
  (println (map (fn [attribute](.getKey attribute))(.getAttributes metadata))))


;; returns a metametamodel, containing two metamodels (the unions), containing a model, containing 2 whales each
(deftest test-inner-union-nested-split-2d
  (let [tuple0 (? (dsplit :type list [(dunion)(dsplit :ocean)])(dfrom "Whales"))
	metaresult0 ((first tuple0))
	result0 ((second tuple0))
	model0 (.getModel result0)]
    (is (= (.getMetadata metaresult0) result-metadata))
    (is (= (.getPairs result0) [[:type]]))
    (is (= (first (.getOperation result0)) :split))
    (let [extant1 (.getExtant (.getData model0))]
      (is (= (count extant1) 2))
      (doall
       (map
	(fn [[model1 prefix1 pairs1 operation1]]
	    (is (= (.getMetadata model1)(.getChildMetadata metaresult0) result-metadata))
	    (let [extant2 (.getExtant (.getData model1))]
	      (is (= (count extant2) 1))
	      (doall
	       (map
		(fn [[model2 prefix2 pairs2 operation2]]
		    (is (= (.getMetadata model2) whale-metadata))
		    (is (= pairs2 [])) ;TODO - is this right ?
		    (is (= (first operation2) :union))
		    (let [extant3 (.getExtant (.getData model2))]
		      (is (= (count extant3) 2))
		      (doall
		       (map
			(fn [whale]
			    (is (contains? #{0 1 2 3} (.getId whale))))
			extant3))))
		extant2))))
	extant1)))
    ))

;; returns a metametamodel containing a single metamodel (the union), containing 4 models, containing a whale each...
(deftest test-outer-union-nested-split-2d
  (let [tuple0 (? (dunion)(dsplit :type list [(dsplit :ocean)])(dfrom "Whales"))
	metaresult0 ((first tuple0))
	result0 ((second tuple0))
	model0 (.getModel result0)]
    (is (= (.getMetadata metaresult0) result-metadata))
    (is (= (.getPairs result0) []))
    (is (= (first (.getOperation result0)) :union))
    (let [extant1 (.getExtant (.getData model0))]
      (is (= (count extant1) 1))
      (doall
       (map
	(fn [[model1 prefix1 pairs1 operation1]]
	    (is (= pairs1 []))		;TODO - is this right ?
	    (is (= (first operation1) :union))
	    (is (= (.getMetadata model1) (.getChildMetadata metaresult0) result-metadata))
	    (let [extant2 (.getExtant (.getData model1))]
	      (is (= (count extant2) 4))
	      (doall
	       (map
		(fn [[model2 prefix2 [[type-key type-val][ocean-key ocean-val]] operation2]]
		    (is (= (.getMetadata model2) whale-metadata))
		    (is (= type-key :type))
		    (is (= ocean-key :ocean))
		    (is (= (first operation2) :split))
		    (let [extant3 (.getExtant (.getData model2))]
		      (is (= (count extant3) 1))
		      (doall
		       (map
			(fn [whale]
			    (is (= (.getType whale) type-val))
			    (is (= (.getOcean whale) ocean-val)))
			extant3))))
		extant2))))
	extant1)))))

;; try a simple pivot...
;; ...  and a nested pivot...
