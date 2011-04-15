(ns
 org.dada.core.utils
 (:import
  [clojure.lang Atom])
 )

;; simplifies syntax for the destructuring of a record

;; (defmacro with-record [record fields & body]
;;   `(let [~(reduce (fn [m field] (conj m [field (keyword field)])) (array-map) fields) ~record]
;;      ~@body))

;; 2nd attempt - uses method instead of keyword
(defmacro with-record [record fields & body]
  `(let ~(apply vector 'r# record (mapcat (fn [field] [field (list '. 'r# field)]) fields))
     ~@body))

;; so why is this useful ?:
;; it allows us to return by-products from atomic updates which can be used to drive side-effects...

(defn swap2! [^Atom atom f & args]
  "like swap! but function must return a sequence of which only the first element is used as the atom's new value"
  (loop []
    (let [old-val @atom
	  results (apply f old-val args)]
      (if (compare-and-set! atom old-val (first results))
	results
	(recur)))))
