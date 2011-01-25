(ns org.dada.core.test-dql2
    (:use 
     [clojure test]
     [org.dada core]
     [org.dada.core dql2])
    (:import
     [org.dada.core
      Attribute
      Metadata$VersionComparator
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

(def #^Metadata$VersionComparator int-version-comparator
     (proxy [Metadata$VersionComparator][]
	    (compareTo [lhs rhs] (- (.getVersion lhs) (.getVersion rhs)))))

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

(def id-getter (.getGetter (.getAttribute whale-metadata :id)))

(deftest test-dfrom
  (is (= #{0 1 2 3}
	 (extract (dfrom "Whales") id-getter)))
  )
