(ns org.dada.core.test-clojure
    (:use 
     [clojure test]
     )
    )

(set! *warn-on-reflection* true)

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

(defn ibm? []
  (.contains (.toLowerCase (System/getProperty "java.vm.vendor")) "ibm"))

;; (if (not (ibm?)) ;TODO - these assumptions do not hold true on IBM - maybe we should generate code accordingly ?
;;   (do
    
;;     ;; (deftest test-get
;;     ;;   (let [^Value v (Value. 0)] (is (faster 100000000 (.v v)(:v v)))))

;;     (deftest int-arithmetic
;;       (is (faster 10000000
;; 		  ((fn [^Integer lhs ^Integer rhs](< (int lhs)(int rhs))) 1 2)
;; 		  ((fn [^Integer lhs ^Integer rhs](< lhs rhs)) 1 2)))) ;; on IBM Java6 - this approach seems [very] slightly faster

;;     ))

;; (import java.lang.reflect.Constructor)
;; (deftest test-construction
;;   (let [^Constructor c (.getConstructor Value (into-array Class [Object]))]
;;     (is (faster 10000000 (Value. 0) (.newInstance c (into-array Object [0]))))
;;     (let [^{:tag (type (into-array Object []))} a (into-array Object [0])]
;;       (is (faster 100000000 (Value. 0) (.newInstance c a))))))

;; I'm not going to test this - just make a note of it...
(deftest test-record-vs-pojo
  (let [^Value r (Value. 0)
	^String p ""]
    (faster 100000000 (.v r)(.length p))))

;; this may be needed for where we need to access a field via a function rather than a java accessor... - not much in it...

;; (deftest test-get
;;   (let [^Value v (Value. 0)] (is (faster 1000000000 ((fn [^Value v] (.v v)) v) (:v v)))))

;; doseq nearly twice as fast

;; (if (not (ibm?))
;;   (deftest doseq-vs-dorun-map
;;     (is (faster 1 (doseq [n (range 10000000)] (identity n)) (dorun (map identity (range 10000000)))))))

;; seems to be true - but so close I can't rely on it not to fail build

;;(deftest assoc-vs-conj
;;  (is (faster 1 (reduce (fn [r i] (assoc r i i)) {} (range 3000000)) (reduce (fn [r i] (conj r [i i])) {} (range 3000000)))))

;; surely some mistake (on my part) here - I can look up a field in a record 100o x faster than I can in an array - I expected it to be faster, but...
;; this is down to reflection - investigate how to use an Object array efficiently
;; type hint was wrong - but why ?
(if (not (ibm?))
  (deftest record-vs-array-access
    (let [^Value r (Value. 0)
	  ^objects a (into-array Object [0])]
      (is (faster 1000000000 (.v r) (aget a 0))))))

;; but accessing a record field still appears to be 10 times faster than an array - is the int being boxed on the way out ?
(if (not (ibm?))
  (deftest record-vs-int-array-access
    (let [^Value r (Value. 0)
	  ^ints a (int-array [0])]
      (is (faster 1000000000 (.v r) (aget a 0))))))

;; no it's not autoboxing slowing down the int-array test - array access is slower than record access - by a factor of 10
(if (not (ibm?))
  (deftest record-vs-object-array-access
    (let [^Value r (Value. 0)
	  ^objects a (object-array [0])]
      (is (faster 1000000000 (.v r) (aget a 0))))))

(defrecord Foo (^int a))
(defrecord Bar (^Integer a))

;; int is faster (5x) - to be expected

;; (if (not (ibm?))
;;   (deftest record-int-vs-integer
;;     (let [f (Foo. 1)
;; 	  b (Bar. 1)]
;;       (is (faster 1000000000 (.a b)  (.a f))))))

;; interesting - reading an int out of a record and pssing it into
;; another fn (causing it to be auto-boxed) is still 5-10x faster than
;; just reading an Integer out of a record and passing it straight
;; into the same fn...

(if (not (ibm?))
  (deftest record-boxed-int-vs-integer
    (let [f (Foo. 1)
	  b (Bar. 1)]
      (is (faster 1000000000 (identity (.a b))  (identity (.a f)))))))
  
(import 'java.util.concurrent.locks.ReadWriteLock)
(import 'java.util.concurrent.locks.ReentrantReadWriteLock)
(import 'java.util.concurrent.locks.Lock)
(import 'java.util.Map)
(import 'java.util.HashMap)

;; (deftest rw-mutable-vs-immutable
;;   (let [lock ^ReadWriteLock (ReentrantReadWriteLock.)
;; 	wr (.writeLock lock)
;; 	m ^Map (HashMap.)
;; 	a (atom {})
;; 	n 10000000]
;;     (is (faster 1
;; 		(dotimes [i n] (try (.lock wr) (.put m i i) (catch Exception e)(finally (.unlock wr))))
;; 		(dotimes [i n] (swap! a conj [i i]))
;; 		))))

;; atom 4 times faster on J6/C1.3a4
;; (if (not (ibm?))
;;   (deftest atom-vs-ref
;;     (let [a (atom 0)
;; 	  r (ref 0)]
;;       (dosync
;;        (is (faster 10000000
;; 		   (swap! a inc)
;; 		   (alter r inc)))))))

;; [with two cores] pmap is about 50x slower than map - i.e. the
;; overhead of dispatching and collating each thread means that you
;; need to do a significant amount of work on it before it will pay
;; off....
;; (deftest map-vs-pmap
;;   (let [data (doall (range 1000000))]
;;     (is (faster 
;; 	 1
;; 	 (doall (map identity data))
;; 	 (doall (pmap identity data))))))
		    
;; (import 'java.util.concurrent.Executors)
;; (import 'java.util.concurrent.ExecutorService)
;; (import 'java.util.concurrent.CountDownLatch)
;; (import 'java.util.concurrent.ConcurrentLinkedQueue)
;; (import 'java.util.Queue)


;; (def ^ExecutorService pool (Executors/newFixedThreadPool 2))

;; (defn jmap [f & seqs]
;;   (let [n (atom 0)
;; 	^CountDownLatch latch (CountDownLatch. 1)
;; 	^Queue results (ConcurrentLinkedQueue.)]
;;     (dorun
;;      (apply
;;       map
;;       (fn [& elts]
;; 	  (swap! n inc)
;; 	  (.execute
;; 	   pool
;; 	   (fn []	
;; 		(.add results (apply f elts))
;; 		(if (= 0 (swap! n dec)) (.countDown latch))
;; 		)))
;;       seqs))
;;     (.await latch)
;;     results))

;; ;;(let [data (doall (range 1000000))] (millis 1 (doall (jmap identity data))))



;; (deftest map-vs-pmap
;;   (let [data (doall (range 1000000))]
;;     (is (faster 
;; 	 1
;; 	 (doall (map identity data))
;; 	 (doall (pmap identity data))))))


;; ;; (deftest transient-vs-persistemt
;; ;;   (let [times 1000000]
;; ;;     (is (faster 
;; ;; 	 1
;; ;; 	 (persistent! (reduce (fn [r i] (conj! r i)) (transient []) (range times)))
;; ;; 	 (reduce (fn [r i] (conj r i)) [] (range times))))))

;; ;; (deftest transient-vector-vs-ArrayList
;; ;;   (let [times 1000000]
;; ;;     (is (faster 
;; ;; 	 1
;; ;; 	 (reduce (fn [^java.util.List r i] (doto r (.add i))) (java.util.ArrayList.) (range times))
;; ;; 	 (persistent! (reduce (fn [r i] (conj! r i)) (transient []) (range times)))))))

;; (deftest transient-map-vs-HashMap
;;   (let [times 1000000]
;;     (is (faster 
;; 	 1
;; 	 (reduce (fn [^java.util.Map r i] (doto r (.put i i))) (java.util.HashMap.) (range times))
;; 	 (persistent! (reduce (fn [r i] (conj! r [i i])) (transient {}) (range times)))))))  
