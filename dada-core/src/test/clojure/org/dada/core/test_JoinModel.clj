(ns org.dada.core.test-JoinModel
  (:use 
   [clojure test]
   [org.dada core]
   )
  (:import
   [org.dada.core JoinModel]
   [org.dada.core.JoinModel LHSEntry RHSEntry]
   )
  )

;; (def child-model (model "Children" (seq-metadata 4))) ;; id, version, name, father-id

;; (def paternity-model (JoinModel.
;; 		      "Paternities"
;; 		      child-model
;; 		      nil
;; 		      [[3] child-model])) ;father


;; (deftest join-model
;;   ;; test insertion (joined)
;;   (insert child-model [0 0 "loop" 0])
;;   (is (= (.find paternity-model 0) [0 0 0 0 "loop" 0 0 0 "loop" 0]))
;;   ;; test alteration (joined)
;;   (insert child-model [0 1 "loop" 0])
;;   (is (= (.find paternity-model 0) [0 1 0 1 "loop" 0 0 1 "loop" 0]))
;;   ;; insertion - unjoined
;;   (insert child-model [2 0 "jules" 1])
;;   (is (= (.find paternity-model 2) nil))
;;   ;; insertion - joined
;;   (insert child-model [3 0 "anthony" 2])
;;   (is (= (.find paternity-model 3) [3 0 3 0 "anthony" 2 2 0 "jules" 1]))
;;   (insert child-model [4 0 "alexandra" 2])
;;   (is (= (.find paternity-model 4) [4 0 4 0 "alexandra" 2 2 0 "jules" 1]))
;;   ;; update - joined -> unjoined
;;   (insert child-model [4 1 "alexandra" -1])
;;   (is (= (.find paternity-model 4) nil))
;;   ;; update - unjoined -> joined
;;   (insert child-model [4 2 "alexandra" 2])
;;   (is (= (.find paternity-model 4) [4 0 4 2 "alexandra" 2 2 0 "jules" 1])) ;; TODO - version should be higher...
  
;;   (insert child-model [4 0 "anthony" 3])
;;   (insert child-model [5 0 "harry" 1])
;;   (insert child-model [6 0 "duncan" 1])
;;   (insert child-model [7 0 "john" 1])
;;   )

;;--------------------------------------------------------------------------------


(def txn-1 [1 0  100 :jules :jane :gbp])

(defn get-mutable [^JoinModel model] @(first (.state model)))

(deftest financial

  (def transaction-model (model "Transactions" (seq-metadata 6))) ;; id, version, amount, from-account-id, to-account-id, currency-id
  (def account-model  (model "Accounts" (seq-metadata 3))) ;; id, version, name
  (def currency-model (model "Currencies" (seq-metadata 3))) ;; id, version, name
  
  (def ^JoinModel join-model (JoinModel.
			       "Join"
			       transaction-model
			       (fn [& args] args)
			       [[[[3] [4]] account-model]
				[[[5]] currency-model]]
			       ))
  
  (def jules [:jules 0 "Julian Gosnell"])
  (def jane  [:jane  0 "Jane Ely"])

  (def gbp [:gbp 0 "British Pounds"])
  
  (def txn-0 [0 0 -100 :jules :jane :gbp])
  
  (is (= (get-mutable join-model) [{} [{}{}]]))
  
  (insert account-model jules)
  (is (= (get-mutable join-model) [{} [{:jules (RHSEntry. jules #{})}{}] nil nil nil]))
  (insert account-model jane)
  (is (= (get-mutable join-model) [{} [{:jules (RHSEntry. jules #{}) :jane (RHSEntry. jane #{})}{}] nil nil nil]))

  (insert currency-model gbp)
  (is (= (get-mutable join-model) [{}
				   [{:jules (RHSEntry. jules #{}) :jane (RHSEntry. jane #{})}
				    {:gbp (RHSEntry. gbp #{})}]
				   nil nil nil]))

  (insert transaction-model txn-0)
  (is (= (get-mutable join-model) [{0 (LHSEntry. true 0 txn-0 [[[jules][jane]][[gbp]]] nil)}
   				   [{:jules (RHSEntry. jules #{}) :jane (RHSEntry. jane #{})}
				    {:gbp (RHSEntry. gbp #{})}]
   				   nil nil nil]))
  
  ;; (insert transaction-model txn-0)
  ;; (is (= (.find join-model 0) [0 0 txn-0 jules]))
  ;; (insert transaction-model txn-1)
  ;; (is (= (.find join-model 1) [1 0 txn-1 jane]))

  ;; (insert account-model [:jane  3 "Jane Gosnell"])
  )
