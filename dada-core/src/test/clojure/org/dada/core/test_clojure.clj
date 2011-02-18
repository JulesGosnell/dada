(ns org.dada.core.test-clojure
    (:use 
     [clojure test]
     )
    )

;; test the speed of various bits of clojure to help ascertain which way DADA should do things

(defmacro millis [times body]
  `(do
     (print (quote ~body))
     (let [start# (System/currentTimeMillis)]
       (do (dotimes [_# ~times] ~body) nil)
       (let [elapsed# (- (System/currentTimeMillis) start#)]
	 (println ": " elapsed# " millis")
	 elapsed#))))

;; todo - time rhs and lhs on different threads ?
(defmacro faster [times lhs rhs]
  `(< (millis ~times ~lhs) (millis ~times ~rhs)))

(defrecord Value [v])

(if (not (.contains (.toLowerCase (System/getProperty "java.vm.vendor")) "ibm")) ;TODO - these assumptions do not hold true on IBM - maybe we should generate code accordingly ?
  (do
    
    (deftest test-get
      (let [^Value v (Value. 0)] (is (faster 100000000 (.v v)(:v v)))))

    (deftest int-arithmetic
      (is (faster 10000000
		  ((fn [^Integer lhs ^Integer rhs](< (int lhs)(int rhs))) 1 2)
		  ((fn [^Integer lhs ^Integer rhs](< lhs rhs)) 1 2)))) ;; on IBM Java6 - this approach seems [very] slightly faster

    ))

(import java.lang.reflect.Constructor)
(deftest test-construction
  (let [^Constructor c (.getConstructor Value (into-array Class [Object]))]
    (is (faster 10000000 (Value. 0) (.newInstance c (into-array Object [0]))))
    (let [^{:tag (type (into-array Object []))} a (into-array Object [0])]
      (is (faster 100000000 (Value. 0) (.newInstance c a))))))

;; I'm not going to test this - just make a note of it...
(deftest test-record-vs-pojo
  (let [^Value r (Value. 0)
	^String p ""]
    (faster 100000000 (.v r)(.length p))))

;; this may be needed for where we need to access a field via a function rather than a java accessor... - not much in it...

(deftest test-get
  (let [^Value v (Value. 0)] (is (faster 1000000000 ((fn [^Value v] (.v v)) v) (:v v)))))

;; doseq nearly twice as fast

(deftest doseq-vs-dorun-map
  (is (faster 1 (doseq [n (range 10000000)] (identity n)) (dorun (map identity (range 10000000))))))

;; seems to be true - but so close I can't rely on it not to fail build

;;(deftest assoc-vs-conj
;;  (is (faster 1 (reduce (fn [r i] (assoc r i i)) {} (range 3000000)) (reduce (fn [r i] (conj r [i i])) {} (range 3000000)))))

;; surely some mistake (on my part) here - I can look up a field in a record 100o x faster than I can in an array - I expected it to be fater, but...
(deftest record-vs-array-access
  (let [^Value r (Value. 0)
	^{:tag (type (into-array Object []))} a (into-array Object [0])]
    (is (faster 1000000 (.v r) (aget a 0)))))
