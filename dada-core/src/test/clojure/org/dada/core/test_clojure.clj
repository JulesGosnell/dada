(ns org.dada.core.test-clojure
    (:use 
     [clojure test]
     [clojure.math.numeric-tower]
     )
    )

(set! *warn-on-reflection* true)

;;--------------------------------------------------------------------------------
;; test the speed of various bits of clojure to help ascertain which way DADA should do things

(defmacro millis [times body]
  `(do
     (System/gc)
     (print (quote ~body))
     (let [start# (System/currentTimeMillis)]
       (do (dotimes [_# ~times] ~body) nil)
       (let [elapsed# (- (System/currentTimeMillis) start#)]
	 (println ": " elapsed# " millis")
	 elapsed#))))

(defmacro faster [times lhs rhs]
  `(let [l# (millis ~times ~lhs) r# (millis ~times ~rhs)] (< l# r#)))

(defmacro slower [times lhs rhs]
  `(let [l# (millis ~times ~lhs) r# (millis ~times ~rhs)] (> l# r#)))

(defmacro similar [times tolerance lhs rhs]
  `(let [l# (millis ~times ~lhs) r# (millis ~times ~rhs)] (< (abs (- l# r#)) ~tolerance)))

(defn ibm? []
  (.contains (.toLowerCase (System/getProperty "java.vm.vendor")) "ibm"))

;;--------------------------------------------------------------------------------

(defrecord Value [v])

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

;; is accessing a record attribute using dot notation comparable in speed to dot notation method invocation ?
;; (deftest test-record-access-vs-pojo-method
;;   (let [^Value r (Value. 0)] (is (similar 100000000 30 (.v r)(.getClass r)))))

;; is accessing a record attribute faster by dot notation than by keyward ?
;; looks like it is currently 5x faster...
(deftest test-get
  (let [^Value v (Value. (Object.))] (is (faster 1000000000 (.v v) (:v v)))))

;; if we wrap the dot notation in a function, is it still faster ?
(deftest test-getter
  (let [^Value v (Value. (Object.)) f (fn [^Value v] (.v v))] (is (faster 1000000000 (f v) (:v v)))))

;; doseq nearly twice as fast

;; (if (not (ibm?))
;;   (deftest doseq-vs-dorun-map
;;     (is (faster 1 (doseq [n (range 10000000)] (identity n)) (dorun (map identity (range 10000000)))))))

;; seems to be true - but so close I can't rely on it not to fail build

;; (deftest assoc-vs-conj
;;   (is (let [n 1000000 i (range n)]
;; 	(faster
;; 	 1
;; 	 (reduce (fn [r i] (conj r [i i])) {} i)
;; 	 (reduce (fn [r i] (assoc r i i)) {} i)))))

;; There does not seem to be much difference between record attribute
;; access via dot notation and array access via index. Perhaps rows
;; should be implemented by arrays ?
(if (not (ibm?))
  (deftest record-vs-array-access
    (let [^Value r (Value. 0) ^objects a (into-array Object [0])] (is (faster 1000000000 (.v r) (aget a 0))))))


(defrecord Foo [^int a])
(defrecord Bar [^Integer a])

;; reading an int out of a record is about 2x as slow as reading an
;; Integer - I guess that this is due to autoboxing...
;; (if (not (ibm?))
;;   (deftest record-int-vs-integer
;;     (let [f (Foo. 1) b (Bar. 1)] (is (slower 1000000000 (.a f) (.a b))))))

;; interesting - reading an int out of a record and pssing it into
;; another fn (causing it to be auto-boxed) is still 5-10x faster than
;; just reading an Integer out of a record and passing it straight
;; into the same fn...

(import 'java.util.concurrent.locks.ReadWriteLock)
(import 'java.util.concurrent.locks.ReentrantReadWriteLock)
(import 'java.util.concurrent.locks.Lock)
(import 'java.util.Map)
(import 'java.util.HashMap)

;; taking a lock, putting an item into a mutable map, releasing the
;; lock appears quicker than swapping a value onto the end of an
;; immutable map in an atom...
(deftest rw-mutable-vs-immutable
  (let [lock ^ReadWriteLock (ReentrantReadWriteLock.)
 	wr (.writeLock lock)
 	m ^Map (HashMap.)
 	a (atom {})
 	n 1000000]
    (is (faster 1
 		(dotimes [i n] (try (.lock wr) (.put m n i) (catch Exception e)(finally (.unlock wr))))
 		(dotimes [i n] (swap! a conj [i i]))
 		))))

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

(defrecord Rec [a b c])

(deftest records
  ;; is it better to modify a record multiple times or to reconstruct it from scratch ?
  ;; it looks like it can be faster to recreate it (but uses more memory?)
  (let [^Rec rec (Rec. 1 2 3)]
    (is (faster 
         1000000
         (Rec. (.c rec) (.b rec) (.a rec))
         (assoc rec :a (.c rec) :c (.a rec))
         ))))  
