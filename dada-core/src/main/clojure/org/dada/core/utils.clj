(ns
 org.dada.core.utils
 (:import
  [java.util Collection]
  [clojure.lang Atom]
  [org.dada.core Creator Getter Metadata])
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

(defn ^String print-object
  ([^Object object ^String internals]
     (str "#<" (.getSimpleName ^Class (.getClass object)) "@" (Integer/toHexString (System/identityHashCode object)) " " internals ">"))
  ([object]
     (print-object object "")))

;;--------------------------------------------------------------------------------

(defn make-pk-fn [^Metadata metadata]
  (let [keys (.getPrimaryKeys metadata)
	^Getter key-getter (.getPrimaryGetter metadata)]
    (if (= (count keys) 1)
      (fn [value] (.get key-getter value))
      (let [^Creator key-creator (.getKeyCreator metadata)]
	(println (str "KEY CREATOR=" (type key-creator) " : " key-creator))
	(fn [value]
	    (let [k (.get key-getter value)]
	      (println (str "K=" k))
	      (.create key-creator (into-array Object k))))))))

;;--------------------------------------------------------------------------------

;; (defmacro make-singleton-key-fn [datum-class key]
;;   "return a fn that given a datum of class datum-class returns the value of .key applied to that datum."
;;   ;; (let [datum 'datum#
;;   ;; 	datum-with-meta (with-meta datum {:tag datum-class})]
;;   ;;   `(fn [~datum-with-meta] (. ~datum ~key))
;;   ;;   )
;;   ;;(list 'fn [(with-meta 'datum {:tag datum-class})] (list '. 'datum key)))

;;   ;; (let [datum 'datum#]
;;   ;; `(fn [~(with-meta datum {:tag datum-class})] (. ~datum ~key))
;;   ;; )

;;   ;;  `(fn [~(with-meta datum# {:tag datum-class})] (. ~datum ~key))

  
;;   ;;)

;;   ;;(let [datum (gensym 'datum)]
;;   ;;(list 'fn [(with-meta datum {:tag datum-class})] (list '. datum key)))

;;   (let [datum (gensym 'datum)]
;;     `(fn [~(with-meta datum {:tag datum-class})]
;;        (. ~datum ~key)))
  
;;   )

;; (defmacro make-singleton-key-creator [datum-class key]
;;   (let [datum (gensym 'datum)]
;;     `(reify Creator (create [this [~(with-meta datum {:tag datum-class}) & _#]] (. ~datum ~key)))))

(defmacro make-singleton-key-getter [datum-class key]
  (let [datum (gensym 'datum)]
    `(reify Getter (get [this ~datum] (. ~(with-meta datum {:tag datum-class}) ~key)))))

;; (defmacro make-compound-key-fn [datum-class keys]
;;   "return a fn that given a datum of class datum-class and a set of
;; keys, creates a key class that references an instance of datum-class
;; and implements its hash code and equality in terms of the values
;; associated with said keys on referenced instance and returns a fn that
;; will manufacture and instance of this class that refers to an instance
;; of datum-class. - i,e, creates a minimal footprint compound key."
;;   (let [key-class (symbol (str datum-class "Key"))
;; 	datum (with-meta 'datum {:tag datum-class})
;; 	that-datum (with-meta 'that-datum {:tag datum-class})
;; 	that 'that
;; 	that-with-type (with-meta that {:tag key-class})
;; 	]
;;     (eval
;;      `(deftype ~key-class
;; 	[~datum]
;; 	Object
;; 	(^int hashCode [this]
;; 	      (+ ~@(map (fn [key] `(. ~datum ~key)) keys)))
;; 	(^boolean equals [this ~that]
;; 		  (and (not (nil? ~that))
;; 		       (instance? ~key-class ~that)
;; 		       ~@(map 
;; 			  (fn [key]
;; 			      `(= (. ~datum ~key)
;; 				  (let [~that-datum (.datum ~that-with-type)]
;; 				    (. ~that-datum ~key))))
;; 			  keys)))
;; ;;	(^String toString [this]
;; ;;		 (str ~keys))
;; 	))
;;     `(fn [datum#]
;;      	 (new ~key-class datum#))
;;     ))

;; (defmacro make-compound-key-creator [datum-class keys]
;;   "return a fn that given a datum of class datum-class and a set of
;; keys, creates a key class that references an instance of datum-class
;; and implements its hash code and equality in terms of the values
;; associated with said keys on referenced instance and returns a fn that
;; will manufacture and instance of this class that refers to an instance
;; of datum-class. - i,e, creates a minimal footprint compound key."
;;   (let [key-class (symbol (str datum-class "Key"))
;; 	datum (with-meta 'datum {:tag datum-class})
;; 	that-datum (with-meta 'that-datum {:tag datum-class})
;; 	that 'that
;; 	that-with-type (with-meta that {:tag key-class})
;; 	]
;;     (eval
;;      `(deftype ~key-class
;;           [~datum]
;; 	Object
;; 	(^int hashCode [this]
;;           (+ ~@(map (fn [key] `(. ~datum ~key)) keys)))
;; 	(^boolean equals [this ~that]
;;           (and (not (nil? ~that))
;;                (instance? ~key-class ~that)
;;                ~@(map 
;;                   (fn [key]
;;                     `(= (. ~datum ~key)
;;                         (let [~that-datum (.datum ~that-with-type)]
;;                           (. ~that-datum ~key))))
;;                   keys)))
;;         ;;	(^String toString [this]
;;         ;;		 (str ~keys))
;; 	))
;;     (let [datum (gensym 'datum)]
;;       `(reify Creator (create [this [~(with-meta datum {:tag datum-class}) & _#]] (new ~key-class ~datum))))
;;     ))

(defmacro make-compound-key-getter [datum-class keys]
  "return a fn that given a datum of class datum-class and a set of
keys, creates a key class that references an instance of datum-class
and implements its hash code and equality in terms of the values
associated with said keys on referenced instance and returns a fn that
will manufacture and instance of this class that refers to an instance
of datum-class. - i,e, creates a minimal footprint compound key."
  (let [key-class (symbol (str datum-class "Key"))
	datum (with-meta 'datum {:tag datum-class})
	that-datum (with-meta 'that-datum {:tag datum-class})
	that 'that
	that-with-type (with-meta that {:tag key-class})
	]
    (eval
     `(deftype ~key-class
          [~datum]
	Object
	(^int hashCode [this]
          (+ ~@(map (fn [key] `(. ~datum ~key)) keys)))
	(^boolean equals [this ~that]
          (and (not (nil? ~that))
               (instance? ~key-class ~that)
               ~@(map 
                  (fn [key]
                    `(= (. ~datum ~key)
                        (let [~that-datum (.datum ~that-with-type)]
                          (. ~that-datum ~key))))
                  keys)))
        ;;	(^String toString [this]
        ;;		 (str ~keys))
	))
    (let [datum (gensym 'datum)]
      `(reify Getter (get [this ~datum] (new ~key-class ~(with-meta datum {:tag datum-class})))))
    ))

;; (defmacro make-ref-key-fn [clazz keys]
;;   (if (instance? Collection keys)
;;      `(make-compound-key-fn ~clazz ~keys)
;;      `(make-singleton-key-fn ~clazz ~keys)
;;      ))

(defmacro make-key-getter [clazz keys]
  (if (> (count keys) 1)
    `(make-compound-key-getter ~clazz ~keys)
    `(make-singleton-key-getter ~clazz ~(first keys))
    ))

