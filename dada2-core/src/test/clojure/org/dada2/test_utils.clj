(ns org.dada2.test-utils
 (:use
  [clojure test]
  [clojure.tools logging]
  [org.dada2 utils]))

(deftest test-swap*!
  (let [old-state 1
	state (atom old-state)
	old-increment 2
	new-increment (swap*! state (fn [state increment] [(+ state increment) increment]) old-increment)]
    (is (= @state (+ old-state old-increment)))
    (is (= new-increment old-increment))
    ))
