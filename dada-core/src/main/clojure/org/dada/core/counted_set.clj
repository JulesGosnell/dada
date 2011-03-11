(ns org.dada.core.counted-set)

;; a set that maintains a count of how many times an element has been inserted/removed...

(defn counted-set-xxx [f s elt]
  (let [s (if (empty? s) {} s)
	n (f (or (s elt) 0))]
    (if (zero? n)
      (dissoc s elt)
      (assoc s elt n))))

(defn counted-set-inc [s elt]
  (counted-set-xxx inc s elt))

(defn counted-set-dec [s elt]
  (counted-set-xxx dec s elt))
  
(defn counted-set-vals [set]
  (keys set))
