(ns org.dada.core.test-JoinModel
  (:use 
   [clojure test]
   [clojure.contrib logging]
   [org.dada core]
   )
  (:import
   [org.dada.core Data JoinModel Model Update]
   [org.dada.core.JoinModel LHSEntry RHSEntry]
   )
  )

(defn get-mutable [^JoinModel model] @(first (.state model)))

;; problem - rhs indeces will be arranged in an indeterminate order...

(deftest rhs-first

  (def transaction-model (model "Transactions" (seq-metadata 6))) ;; id, version, amount, from-account-id, to-account-id, currency-id
  (def account-model  (model "Accounts" (seq-metadata 3))) ;; id, version, name
  (def currency-model (model "Currencies" (seq-metadata 3))) ;; id, version, name
  
  (def ^JoinModel join-model (JoinModel.
			      "Join"
			      (seq-metadata 3)
			      transaction-model
			      {3 account-model
			      4 account-model
			      5 currency-model}
			      (fn [id version txn [[ccy] [to-acc from-acc]]]
				  ;;(info ["JOIN" id version txn from-acc to-acc ccy])
				  [id version txn from-acc to-acc ccy]
				  )))

  (is (= (get-mutable join-model) [{} [{}{}] nil nil nil]))

  ;; insert rhs (lhs does not exist)
  (def jules-0 [:jules 0 "Julian Gosnell"])
  (insert account-model jules-0)
  (is (= (get-mutable join-model) [{}
				   [{}
				    {:jules (RHSEntry. jules-0 [#{} #{}])}]
				   nil nil nil]))
  
  ;; insert rhs (lhs does not exist)
  (def jane-0  [:jane  0 "Jane Ely"])
  (insert account-model jane-0)
  (is (= (get-mutable join-model) [{}
				   [{}
				    {:jane  (RHSEntry. jane-0  [#{} #{}])
				     :jules (RHSEntry. jules-0 [#{} #{}])}]
				   nil nil nil]))

  ;; insert rhs (lhs does not exist)
  (def gbp-0 [:gbp 0 "British Pounds"])
  (insert currency-model gbp-0)
  (is (= (get-mutable join-model) [{}
				   [{:gbp   (RHSEntry. gbp-0   [#{}])}
				    {:jane  (RHSEntry. jane-0  [#{} #{}])
				     :jules (RHSEntry. jules-0 [#{} #{}])}
				    ]
				   nil nil nil]))

  ;; insert lhs (rhs exists)
  (def txn-0 [0 0 -100 :jules :jane :gbp])
  (insert transaction-model txn-0)
  (let [new-datum [0 0 txn-0 jules-0 jane-0 gbp-0]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 0 txn-0 [[gbp-0] [jane-0 jules-0]] new-datum)}
				     [{:gbp   (RHSEntry. gbp-0   [#{0}])}
				      {:jane  (RHSEntry. jane-0  [#{0} #{}])
				       :jules (RHSEntry. jules-0 [#{} #{0}])}
				      ]
				     [(Update. nil new-datum)] nil nil])))

  ;; insert rhs (lhs does not exist)
  (def usd-0 [:usd 0 "United States Dollars"])
  (insert currency-model usd-0)
  (is (= (get-mutable join-model) [{0 (LHSEntry. true 0 txn-0 [[gbp-0] [jane-0 jules-0]]
						 [0 0 txn-0 jules-0 jane-0 gbp-0])}
   				   [{:usd   (RHSEntry. usd-0   [#{}])
				     :gbp   (RHSEntry. gbp-0   [#{0}])}
				    {:jane  (RHSEntry. jane-0  [#{0} #{}])
				     :jules (RHSEntry. jules-0 [#{} #{0}])}
				    ]
   				   nil nil nil]))

  ;; update lhs (rhses exist)
  (def txn-1 [0 1  100 :jane :jules :usd])
  (insert transaction-model txn-1)
  (let [old-datum [0 0 txn-0 jules-0 jane-0 gbp-0]
	new-datum [0 1 txn-1 jane-0 jules-0 usd-0]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 1 txn-1 [[usd-0] [jules-0 jane-0]] new-datum)}
				     [{:usd   (RHSEntry. usd-0   [#{0}])
				       :gbp   (RHSEntry. gbp-0   [#{}])}
				      {:jane  (RHSEntry. jane-0  [#{} #{0}])
				       :jules (RHSEntry. jules-0 [#{0} #{}])}
				      ]
				     nil [(Update. old-datum new-datum)] nil])))

  ;; update rhs (lhs exists)
  (def jules-1 [:jules 1 "J.A.F.Gosnell"])
  (insert account-model jules-1)
  (let [old-datum [0 1 txn-1 jane-0 jules-0 usd-0]
	new-datum [0 2 txn-1 jane-0 jules-1 usd-0]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 2 txn-1 [[usd-0] [jules-1 jane-0]] new-datum)}
				     [{:usd   (RHSEntry. usd-0   [#{0}])
				       :gbp   (RHSEntry. gbp-0   [#{}])}
				      {:jane  (RHSEntry. jane-0  [#{} #{0}])
				       :jules (RHSEntry. jules-1 [#{0} #{}])}
				      ]
				     nil [(Update. old-datum new-datum)] nil])))
  
  (def jane-1 [:jane 1 "J.K.Ely"])
  (insert account-model jane-1)
  (let [old-datum [0 2 txn-1 jane-0 jules-1 usd-0]
	new-datum [0 3 txn-1 jane-1 jules-1 usd-0]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 3 txn-1 [[usd-0] [jules-1 jane-1]] new-datum)}
				     [{:usd   (RHSEntry. usd-0   [#{0}])
				       :gbp   (RHSEntry. gbp-0   [#{}])}
				      {:jane  (RHSEntry. jane-1  [#{} #{0}])
				       :jules (RHSEntry. jules-1 [#{0} #{}])}
				      ]
				     nil [(Update. old-datum new-datum)] nil])))

  ;; new lhs - but no change to joins
  (def txn-2 [0 2  -100 :jane :jules :usd])
  (insert transaction-model txn-2)
  (let [old-datum [0 3 txn-1 jane-1 jules-1 usd-0]
	new-datum [0 4 txn-2 jane-1 jules-1 usd-0]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 4 txn-2 [[usd-0] [jules-1 jane-1]] new-datum)}
				     [{:usd   (RHSEntry. usd-0   [#{0}])
				       :gbp   (RHSEntry. gbp-0   [#{}])}
				      {:jane  (RHSEntry. jane-1  [#{} #{0}])
				       :jules (RHSEntry. jules-1 [#{0} #{}])}
				      ]
				     nil [(Update. old-datum new-datum)] nil])))

  ;; lhs - change one join
  (def txn-3 [0 3  -100 :jane :jules :gbp])
  (insert transaction-model txn-3)
  (let [old-datum [0 4 txn-2 jane-1 jules-1 usd-0]
	new-datum [0 5 txn-3 jane-1 jules-1 gbp-0]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 5 txn-3 [[gbp-0] [jules-1 jane-1]] new-datum)}
				     [{:usd   (RHSEntry. usd-0   [#{}])
				       :gbp   (RHSEntry. gbp-0   [#{0}])}
				      {:jane  (RHSEntry. jane-1  [#{} #{0}])
				       :jules (RHSEntry. jules-1 [#{0} #{}])}
				      ]
				     nil [(Update. old-datum new-datum)] nil])))
  )

(deftest lhs-first

  (def transaction-model (model "Transactions" (seq-metadata 6))) ;; id, version, amount, from-account-id, to-account-id, currency-id
  (def account-model  (model "Accounts" (seq-metadata 3))) ;; id, version, name
  (def currency-model (model "Currencies" (seq-metadata 3))) ;; id, version, name
  
  (def ^Model join-model (JoinModel.
			  "Join"
			  (seq-metadata 3)
			  transaction-model
			  {3 account-model
			  4 account-model
			  5 currency-model}
			  (fn [id version txn [[ccy] [to-acc from-acc]]]
			      ;;(info ["JOIN" id version txn from-acc to-acc ccy])
			      [id version txn from-acc to-acc ccy]
			      )
			  ))

  (is (= (get-mutable join-model) [{} [{}{}] nil nil nil]))

  ;; insert lhs (rhses do not exist)
  (def txn-0 [0 0 -100 :jules :jane :gbp])
  (insert transaction-model txn-0)
  (let [old-datum nil
	new-datum [0 0 txn-0 nil nil nil]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 0 txn-0 [[nil] [nil nil]] new-datum)}
				     [{:gbp   (RHSEntry. nil [#{0}])}
				      {:jane  (RHSEntry. nil [#{0} #{}])
				       :jules (RHSEntry. nil [#{} #{0}])}]
				     [(Update. old-datum new-datum)] nil nil])))

  ;; update lhs (rhses do not exist)
  (def txn-1 [0 1 100 :jane :jules :usd])
 (insert transaction-model txn-1)
  (let [old-datum [0 0 txn-0 nil nil nil]
	new-datum [0 1 txn-1 nil nil nil]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 1 txn-1 [[nil] [nil nil]] new-datum)}
				     [{:usd   (RHSEntry. nil [#{0}])
				       :gbp   (RHSEntry. nil [#{}])}
				      {:jane  (RHSEntry. nil [#{} #{0}])
				       :jules (RHSEntry. nil [#{0} #{}])}]
				     nil [(Update. old-datum new-datum)] nil])))
  ;; insert rhs (lhs exists)
  (def jules-0 [:jules 0 "Julian Gosnell"])
  (insert account-model jules-0)
  (let [old-datum [0 1 txn-1 nil nil nil]
	new-datum [0 2 txn-1 nil jules-0 nil]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 2 txn-1 [[nil] [jules-0 nil]] new-datum)}
				     [{:usd   (RHSEntry. nil [#{0}])
				       :gbp   (RHSEntry. nil [#{}])}
				      {:jane  (RHSEntry. nil [#{} #{0}])
				       :jules (RHSEntry. jules-0 [#{0} #{}])}]
				     nil [(Update. old-datum new-datum)] nil])))  

  ;; insert rhs (lhs exists)
  (def jane-0 [:jane 0 "Jane Ely"])
  (insert account-model jane-0)
  (let [old-datum [0 2 txn-1 nil jules-0 nil]
	new-datum [0 3 txn-1 jane-0 jules-0 nil]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 3 txn-1 [[nil] [jules-0 jane-0]] new-datum)}
				     [{:usd   (RHSEntry. nil [#{0}])
				       :gbp   (RHSEntry. nil [#{}])}
				      {:jane  (RHSEntry. jane-0  [#{} #{0}])
				       :jules (RHSEntry. jules-0 [#{0} #{}])}]
				     nil [(Update. old-datum new-datum)] nil])))  

  ;; insert rhs (lhs does not exist) - N.B. important that this does not create a new version of join
  (def gbp-0 [:gbp 0 "British Pounds"])
  (insert currency-model gbp-0)
  (is (= (get-mutable join-model) [{0 (LHSEntry. true 3 txn-1 [[nil] [jules-0 jane-0]]
						 [0 3 txn-1 jane-0 jules-0 nil])}
   				   [{:usd   (RHSEntry. nil [#{0}])
				     :gbp   (RHSEntry. gbp-0   [#{}])}
				    {:jane  (RHSEntry. jane-0  [#{} #{0}])
				     :jules (RHSEntry. jules-0 [#{0} #{}])}]
   				   nil nil nil]))

  ;; insert rhs (lhs exists)
  (def usd-0 [:usd 0 "United States Dollars"])
  (insert currency-model usd-0)
  (let [old-datum [0 3 txn-1 jane-0 jules-0 nil]
	new-datum [0 4 txn-1 jane-0 jules-0 usd-0]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 4 txn-1 [[usd-0] [jules-0 jane-0]] new-datum)}
				     [{:usd   (RHSEntry. usd-0   [#{0}])
				       :gbp   (RHSEntry. gbp-0   [#{}])}
				      {:jane  (RHSEntry. jane-0  [#{} #{0}])
				       :jules (RHSEntry. jules-0 [#{0} #{}])}]
				     nil [(Update. old-datum new-datum)] nil])))

  ;; getData
  ;; TODO: should be able to compare two Data instances...
  (let [data (.getData ^Model join-model)]
    (is (= (.getExtant data) [[0 4 txn-1 jane-0 jules-0 usd-0]]))
    (is (= (.getExtinct data) [])))

  ;; find
  (is (= (.find join-model 0) [0 4 txn-1 jane-0 jules-0 usd-0]))
  (is (= (.find join-model 1) nil))
  
  ;; delete lhs - lhs -> nil
  (delete transaction-model txn-1)
  (let [old-datum [0 4 txn-1 jane-0 jules-0 usd-0]]
    (is (= (get-mutable join-model)
	   [{0 (LHSEntry. false 4 txn-1 [[nil] [nil nil]] old-datum)}
	    [{:usd   (RHSEntry. usd-0   [#{}])
	      :gbp   (RHSEntry. gbp-0   [#{}])}
	     {:jules (RHSEntry. jules-0 [#{} #{}])
	      :jane  (RHSEntry. jane-0  [#{} #{}])}]
	    nil nil [(Update. old-datum nil)]])))

  ;; reinsert txn, so we can delete again
  (def txn-2 [0 2 100 :jane :jules :usd])
  (insert transaction-model txn-2)
  (let [new-datum [0 5 txn-2 jane-0 jules-0 usd-0]]
    (is (= (get-mutable join-model) [{0 (LHSEntry. true 5 txn-2 [[usd-0] [jules-0 jane-0]] new-datum)}
				     [{:usd   (RHSEntry. usd-0 [#{0}])
				       :gbp   (RHSEntry. gbp-0 [#{}])}
				      {:jules (RHSEntry. jules-0 [#{0} #{}])
				       :jane  (RHSEntry. jane-0 [#{} #{0}])}]
				     [(Update. nil new-datum)] nil nil])))
  
  ;; split delete x -> x+1 (in another model)
  (def txn-3 [0 3 100 :jane :jules :usd])
  (.update transaction-model [] [] [(Update. txn-2 txn-3)])
  (let [old-datum [0 5 txn-2 jane-0 jules-0 usd-0]
  	new-datum [0 5 txn-3 jane-0 jules-0 usd-0]]
    (is (= (get-mutable join-model)
  	   [{0 (LHSEntry. false 5 txn-3 [[nil] [nil nil]] old-datum)}
  	    [{:usd   (RHSEntry. usd-0   [#{}])
  	      :gbp   (RHSEntry. gbp-0   [#{}])}
  	     {:jules (RHSEntry. jules-0 [#{} #{}])
  	      :jane  (RHSEntry. jane-0  [#{} #{}])}]
  	    nil nil [(Update. old-datum nil)]])))

  ;; second delete on top of first - should be ignored ?
  (.update transaction-model [] [] [(Update. txn-2 nil)])
  (let [old-datum [0 5 txn-2 jane-0 jules-0 usd-0]
  	new-datum [0 5 txn-3 jane-0 jules-0 usd-0]]
    (is (= (get-mutable join-model)
  	   [{0 (LHSEntry. false 5 txn-3 [[nil] [nil nil]] old-datum)}
  	    [{:usd   (RHSEntry. usd-0   [#{}])
  	      :gbp   (RHSEntry. gbp-0   [#{}])}
  	     {:jules (RHSEntry. jules-0 [#{} #{}])
  	      :jane  (RHSEntry. jane-0  [#{} #{}])}]
  	    nil nil [(Update. old-datum nil)]])))

  ;; another delete
  (.update transaction-model [] [] [(Update. txn-3 nil)])
  (let [old-datum [0 5 txn-2 jane-0 jules-0 usd-0]
  	new-datum [0 5 txn-3 jane-0 jules-0 usd-0]]
    (is (= (get-mutable join-model)
  	   [{0 (LHSEntry. false 5 txn-3 [[nil] [nil nil]] old-datum)}
  	    [{:usd   (RHSEntry. usd-0   [#{}])
  	      :gbp   (RHSEntry. gbp-0   [#{}])}
  	     {:jules (RHSEntry. jules-0 [#{} #{}])
  	      :jane  (RHSEntry. jane-0  [#{} #{}])}]
  	    nil nil [(Update. old-datum nil)]])))  
  )
