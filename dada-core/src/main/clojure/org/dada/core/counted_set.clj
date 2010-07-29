(ns org.dada.core.counted-set)

(defn counted-set-inc [set elt]
  (let [map (if (empty? set) {} set)]
    (conj map [elt (+ (or (map elt) 0) 1)])))

(defn counted-set-dec [set elt]
  (if (empty? set)
    set
    (let [val (set elt)]
      (if (nil? val)
	set
	(let [count (- val 1)]
	  (if (> count 0)
	    (conj set [elt count])
	    (dissoc set elt)))))))

(defn counted-set-vals [set]
  (keys set))
