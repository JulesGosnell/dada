(ns
 org.dada.core.map
 (:import
  [java.util
   Collection
   Date]))

(do

  ;; let's use persistant maps - slower, bigger footprint, lock-free

  (def map-new hash-map)

  (defmacro map-put [map key val]
    `(assoc ~map ~key ~val))

  (defmacro map-rem [map key]
    `(dissoc ~map ~key))

  (defmacro map-get [map key]
    `(~map ~key))

  (defmacro map-vals [map]
    `(vals ~map))

  (defmacro map-count [map]
    `(count ~map))

  (defmacro map-locking [lockee & body]
    `(do ~@body))
  )

;; (do

;;   ;; let's use hashmaps - faster, smaller footprint with locking

;;   (defn map-new [] (java.util.HashMap.))

;;   (defmacro map-put [map key val]
;;     `(let [^java.util.HashMap map# ~map] (.put map# ~key ~val) map#))

;;   (defmacro map-rem [map key]
;;     `(let [^java.util.Map map# ~map] (.remove map# ~key) map#))

;;   (defmacro map-get [map key]
;;     `(let [^java.util.Map map# ~map] (.get map# ~key)))

;;   (defmacro map-vals [map]
;;     `(let [^java.util.Map map# ~map] (into [] (vals map#))))

;;   (defmacro map-count [map]
;;     `(let [^java.util.Map map# ~map] (.size map#)))
  
;;   (defmacro map-locking [lockee & body]
;;     `(locking ~lockee ~@body))

;;   )

;; vector impls

;; (do
  
;;   ;; lets use built in vector

;;   (defmacro vec-new [args]
;;     `(vector ~@args))

;;   (defmacro vec-get [vec i]
;;     `(nth ~vec ~i))

;;   (defmacro vec-set [vec i val]
;;     `(assoc ~vec ~i ~val))

;;   )

(do
  
  ;; lets use an java array

  ;; (defmacro vec-new [args]
  ;;   `(into-array ~args))

  ;; (defmacro vec-get [vec i]
  ;;   `(aget ~vec ~i))

  ;; (defmacro vec-set [vec i val]
  ;;   `(let [vec# ~vec] (aset vec# ~i ~val) vec#))

  )

;; (do

;;   ;; lets use a java.util.ArrayList

;;   (defmacro vec-new [args]
;;     `(let [^java.util.Collection args# ~args] (java.util.ArrayList. args#)))

;;   (defmacro vec-get [vec i]
;;     `(let [^java.util.List vec# ~vec] (.get vec# ~i)))
  
;;   (defmacro vec-set [vec i val]
;;     `(let [^java.util.List vec# ~vec] (.set vec# ~i ~val) vec#))

;;   )
