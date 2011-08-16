(ns
 org.dada.core.map
 (:import
  [java.util
   Collection
   Date
   HashMap]))

(do

  ;; let's use persistant maps - slower, bigger footprint, lock-free

  (def map-new hash-map)

  (defmacro map-put [map key val]
    `(assoc ~map ~key ~val))

  (defmacro map-get [map key]
    `(~map ~key))

  (defmacro map-count [map]
    `(count ~map))

  (defmacro map-locking [lockee & body]
    `(do ~@body))
  )

;; (do

;;   ;; let's use hashmaps - faster, smaller footprint with locking

;;   (defn map-new [] (HashMap.))

;;   (defmacro map-put [map key val]
;;     `(do (.put ~(with-meta map {:tag 'HashMap}) ~key ~val) ~map))

;;   (defmacro map-get [map key]
;;     `(.get ~(with-meta map {:tag 'HashMap}) ~key))

;;   (defmacro map-count [map]
;;     `(.size ~(with-meta map {:tag 'HashMap})))
  
;;   (defmacro map-locking [lockee & body]
;;     `(locking ~lockee ~@body))

;;   )

