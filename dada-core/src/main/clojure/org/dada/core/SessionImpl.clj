(ns org.dada.core.SessionImpl
 (:use [clojure.contrib logging])
 (:import
  [clojure.lang IFn]
  [java.util Map]
  [org.dada.core Data Model View]
  )
 (:gen-class
  :implements [org.dada.core.Session]
  :constructors {[org.dada.core.Model clojure.lang.IFn] []}
;;  :methods [[toString [] String]]
  :init init
  :state state
  )
 )

(defrecord SessionState [^Long lastPing ^Map views ^Map old-views])

(defn -init [^Model metamodel ^IFn close-hook]
  [ ;; super ctor args
   []
   ;; instance state
   (let [mutable (atom (SessionState. (System/currentTimeMillis) {} nil))
	 immutable [metamodel close-hook]]
     [mutable immutable])])

(defn mutable [^org.dada.core.SessionImpl this]
  (first (.state this)))

(defn -ping [^org.dada.core.SessionImpl this]
  (debug "LOCAL SESSION - PING")
  (swap! (mutable this) assoc :lastPing (System/currentTimeMillis)))

(defn -close [^org.dada.core.SessionImpl this]
  (let [[mutable [_ close-hook]] (.state this)]
    (close-hook this)
    (let [state (swap!
		 mutable
		 (fn [state]
		   (let [views (state :views)]
		     (assoc
			 state
		       :old-views
		       (assoc state :views nil) views))))
	  views (state :old-views)]
      (debug "LOCAL SESSION - CLOSE" (keys views))
      (doseq [[^View view ^Model model] views] (.deregisterView model view)))))

(defn ^Data -registerView [^org.dada.core.SessionImpl this ^Model model ^View view]
  (debug "LOCAL SESSION - REGISTER VIEW")
  (let [model-name (.getName model)
	[[_ ^Model metamodel] mutable] (.state this)
	^Model model (.find metamodel model-name)]
    (if (nil? model)
      (throw (IllegalArgumentException. (str "no Model for name: " model-name)))
      (let [data (.registerView model view)]
	(swap!
	 mutable
	 (fn [state]
	   (let [view-to-models (state :views)
		 models (view-to-models view)
		 models (conj models model)
		 view-to-models (assoc view-to-models view models)]
	     (assoc state :views view-to-models))))
	data))))

(defn ^Data -deregisterView [^org.dada.core.SessionImpl this ^Model model ^View view]
  (debug "LOCAL SESSION - DEREGISTER VIEW")
  (let [model-name (.getName model)
	[[_ ^Model metamodel ^ServiceFactory service-factory] mutable] (.state this)
	^Model model (.find metamodel model-name)]
    (if (nil? model)
      (throw (IllegalArgumentException. (str "no Model for name: " model-name)))
      (let [data (.deregisterView model view)]
	(swap!
	 mutable
	 (fn [state]
	   (let [view-to-models (state :views)
		 models (view-to-models view)
		 models (remove (fn [m] (= m model)) models)
		 view-to-models (assoc view-to-models view models)]
	     (assoc state :views view-to-models))))
	data))))

;;------------------------------------------------------------------------------

;; (defn ^String -toString [^org.dada.core.SessionImpl this]
;;   (let [state (mutable this)]
;;     (str "<SessionImpl: " (state :lastPing) ">")))
