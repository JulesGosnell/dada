(ns org.dada2.utils
 (:import
  [clojure.lang
   Atom])
 (:use
  [clojure test]
  [clojure.tools logging])
 )

;; so why is this useful ?:
;; it allows us to return by-products from atomic updates which can be used to drive side-effects...

(defn swap*! [^Atom atom f & args]
  "like swap! but function must return a sequence of which only the first element is used as the atom's new value"
  (loop []
    (let [old-val @atom
	  results (apply f old-val args)]
      (if (compare-and-set! atom old-val (first results))
	results
	(recur)))))