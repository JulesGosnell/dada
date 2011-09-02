(ns
 org.dada.core.map
 (:import
  [java.util
   Collection
   Date]))

(do

  ;; let's use persistant maps - slower, bigger footprint, lock-free

  (def map-new hash-map)
  (def map-new2 hash-map)

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

;;   ;; let's use j.u.HashMap - faster, smaller footprint with locking

;;   (defn map-new []
;;     (java.util.HashMap.))

;;   (defn map-new2 [& args]
;;     (java.util.HashMap. ^java.util.Map (apply hash-map args)))

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

(do
  
  ;; lets use built in vector

  (defmacro vec-new [args]
    `(apply vector ~args))

  (defmacro vec-get [vec i]
    `(nth ~vec ~i))

  (defmacro vec-set [vec i val]
    `(assoc ~vec ~i ~val))

  (defmacro vec-seq [vec]
    `~vec)

  )

;; (do
  
;;   ;; lets use built in vector

;;   (defn vec-new [args]
;;     (apply vector args))

;;   (def vec-get nth)

;;   (def vec-set assoc)

;;   (def vec-seq identity)

;;   )

;; (do
  
;;   ;; lets use an java array

;;   (defmacro vec-new [args]
;;     `(into-array Object ~args))

;;   (defmacro vec-get [vec i]
;;     `(let [^"[Ljava.lang.Object;" vec# ~vec] (aget vec# ~i)))

;;   (defmacro vec-set [vec i val]
;;     `(let [^"[Ljava.lang.Object;" vec# ~vec] (aset vec# ~i ~val) vec#))

;;   (defmacro vec-seq [vec]
;;     `~vec)

;;   )

;; (do

;;   ;; lets use a java.util.ArrayList

;;   (defmacro vec-new [args]
;;     `(let [^java.util.Collection args# ~args] (java.util.ArrayList. args#)))

;;   (defmacro vec-get [vec i]
;;     `(let [^java.util.List vec# ~vec] (.get vec# ~i)))
  
;;   (defmacro vec-set [vec i val]
;;     `(let [^java.util.List vec# ~vec] (.set vec# ~i ~val) vec#))

;;   )

;; (do

;;   ;; lets use a java.util.ArrayList - functions, whilst we debug

;;   (defn vec-new [args]
;;     (apply vector args))
  
;;   (defn vec-get [vec i]
;;     (vec i))
  
;;   (defn vec-set [vec i val]
;;     (assoc vec i val))

;;   )

;; (do

;;   (deftype Opaque [^java.util.List l])

;;   ;; lets use a java.util.ArrayList - functions, whilst we debug

;;   (defn vec-new [args]
;;     (Opaque. (java.util.ArrayList. ^java.util.Collection args)))
  
;;   (defn vec-get [^Opaque vec i]
;;     (.get (.l vec) i))
  
;;   (defn vec-set [^Opaque vec i val]
;;     (.set (.l vec) i val)
;;     vec)

;;   (defn vec-seq [^Opaque vec]
;;     (.l vec))

;;   )
