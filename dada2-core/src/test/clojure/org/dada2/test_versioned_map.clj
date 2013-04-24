(ns org.dada2.test-versioned-map
    (:use
     [clojure test]
     [clojure.tools logging]
     [org.dada2 utils]
     [org.dada2 core]
     [org.dada2 test-core]
     )
    )

;; need to inject comparator fn
;; what if we already have (vk-fn new-value) in our hand ?
;; what about deletion ?
(defn versioned-map-optimistic-assoc [old-map pk vk-fn new-value]
  (let [new-map (assoc old-map pk new-value)]
    (if (identical? old-map new-map)
      old-map
      (let [old-value (old-map pk)]
        (if (and old-value (>= (vk-fn old-value) (vk-fn new-value)))
          old-map
          new-map)))))

(defn versioned-map-optimistic-dissoc [old-map pk vk-fn new-version]
  (let [new-map (dissoc old-map pk)]
    (if (identical? old-map new-map)
      old-map
      (let [old-value (old-map pk)]
        (if (and old-value (> (vk-fn old-value) new-version ))
          old-map
          new-map)))))

(defn versioned-map-pessimistic-assoc [old-map pk vk-fn new-value]
  (let [old-value (old-map pk)]
    (if (and old-value (>= (vk-fn old-value) (vk-fn new-value)))
      old-map
      (assoc old-map pk new-value))))

(defn versioned-map-pessimistic-dissoc [old-map pk vk-fn new-version]
  (let [old-value (old-map pk)]
    (if (and old-value (> (vk-fn old-value) new-version)) 
      old-map
      (dissoc old-map pk))))

(defn versioned-map-dissoc [old-map pk]
  (dissoc old-map pk))

(defn versioned-map-get [vm pk]
  (vm pk))

;; some aliases
(def vm:opt-assoc versioned-map-optimistic-assoc)
(def vm:opt-dissoc versioned-map-optimistic-dissoc)
(def vm:pess-assoc versioned-map-pessimistic-assoc)
(def vm:pess-dissoc versioned-map-pessimistic-dissoc)
(def vm:get versioned-map-get)

(deftest test-versioned-map
  
  (is (= (vm:opt-assoc {} :a first [0 "a"]) {:a [0 "a"]}))
  (is (= (vm:opt-assoc (vm:opt-assoc {} :a first [0 "a"]) :a first [1 "a"]) {:a [1 "a"]}))
  (is (= (vm:opt-assoc (vm:opt-assoc {} :a first [0 "a"]) :a first [-1 "a"]) {:a [0 "a"]}))
  (is (= (vm:get (vm:opt-assoc {} :a first [0 "a"]) :a) [0 "a"]))
  (is (= (vm:opt-dissoc (vm:opt-assoc {} :a first [0 "a"]) :a first -1) {:a [0 "a"]}))
  (is (= (vm:opt-dissoc (vm:opt-assoc {} :a first [0 "a"]) :a first 0) {}))
  (is (= (vm:opt-dissoc (vm:opt-assoc {} :a first [0 "a"]) :a first 1) {}))

  (is (= (vm:pess-assoc {} :a first [0 "a"]) {:a [0 "a"]}))
  (is (= (vm:pess-assoc (vm:pess-assoc {} :a first [0 "a"]) :a first [1 "a"]) {:a [1 "a"]}))
  (is (= (vm:pess-assoc (vm:pess-assoc {} :a first [0 "a"]) :a first [-1 "a"]) {:a [0 "a"]}))
  (is (= (vm:get (vm:pess-assoc {} :a first [0 "a"]) :a) [0 "a"]))
  (is (= (vm:pess-dissoc (vm:pess-assoc {} :a first [0 "a"]) :a first -1) {:a [0 "a"]}))
  (is (= (vm:pess-dissoc (vm:pess-assoc {} :a first [0 "a"]) :a first 0) {}))
  (is (= (vm:pess-dissoc (vm:pess-assoc {} :a first [0 "a"]) :a first 1) {}))
  )
