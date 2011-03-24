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

;; drill into v1 with k1 then k2 and add v4 to the resulting vector, rebuilding structure on way out

(defn assoc2m [v1 k1 k2 v4]
  (let [v2 (or (.get v1 k1) {})
	v3 (or (.get v2 k2) [])]
    (assoc v1 k1 (assoc v2 k2 (conj v3 v4)))))

;; drill into v1 with k1 then k2 and remove v4 to the resulting vector, rebuilding structure on way out

(defn dissoc2m [v1 k1 k2 v4]
  (let [v2 (or (.get v1 k1) {})
	v3 (or (.get v2 k2) [])]
    (assoc v1 k1 (assoc v2 k2 (remove (fn [v] (= v v4)) v3)))))