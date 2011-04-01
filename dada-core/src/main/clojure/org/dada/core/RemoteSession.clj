(ns org.dada.core.RemoteSession
 (:use
  [clojure.contrib logging]
  [org.dada.core utils]
  [org.dada.core proxy]
  [org.dada.core.remote])
 (:require
  [org.dada.core RemoteView])
 (:import
  [java.util
   Timer
   TimerTask]
  [java.util.concurrent
   ExecutorService]
  [java.util.concurrent.atomic
   AtomicReference]
  [clojure.lang
   Atom]
  [org.dada.core
   Data
   Model
   Session
   SessionManager
   View
   RemoteView]
  [org.dada.core.remote
   AsyncMessageClient
   MessageServer
   MessageStrategy
   Remoter
   Translator
   ]
  )
 (:gen-class
  :implements [org.dada.core.Session java.io.Serializable]
  :constructors {[Object] []}
  :methods [[hack [org.dada.core.remote.Remoter] void]]
  :init init
  :state state
  )
 )

(defrecord ImmutableState [^Object send-to ^AsyncMessageClient client ^Session peer ^Remoter remoter ^Timer timer ^Atom views])

;; proxy for a server-side Session
;; intercept outward-bound local Views and replace with proxies

(defproxy-type SessionProxy Session)

(defn -init [^Object send-to]
  (debug "init: " send-to)
  [ ;; super ctor args
   []
   ;; instance state - held in an AtomicReference so we can change it (a Clojure Atom will not serialise)
   (AtomicReference. (ImmutableState. send-to nil nil nil nil nil))])

(defn ^ImmutableState immutable [^org.dada.core.RemoteSession this]
  (.get ^AtomicReference (.state this)))

(defn -ping [^org.dada.core.RemoteSession this]
  (with-record (immutable this) [^Session peer] (trace "ping: " peer) (.ping peer)))

(defn -close [^org.dada.core.RemoteSession this]
  (with-record
   (immutable this)
   [^Session peer ^Timer timer views]
   (debug "close: " peer)
   ;; close outstanding Views
   (doseq [[view model] (keys @views)] (.deregisterView this model view))
   (.cancel timer)
   (.close peer)))

;; TODO - only supports one registration per model/view pair - could
;; be a problem if someone uses API wrongly

(defn ^Data -registerView [^org.dada.core.RemoteSession this ^Model model ^View view]
    (with-record
     (immutable this)
     [^Session peer ^Remoter remoter views]
     (debug "registerView" model view)
     (let [topic (.endPoint remoter (str "DADA." (.getName model)) true) ;TODO - hardwired prefix and Destination type
	   ^AsyncMessageServer server (.server remoter view topic)
	   ^View client (RemoteView. topic)]
       (swap! views (fn [views] (assoc views [view model] [topic server client])))
       (.registerView peer model client))))

(defn ^Data -deregisterView [^org.dada.core.RemoteSession this ^Model model ^View view]
  (with-record
   (immutable this)
   [^Session peer views]
   (debug "deregisterView" model view)
   (let [[_ [^Topic topic ^MessageServer server ^View client]]
	 (swap2! views (fn [views] (let [key [view model] old-view (views key)] [(dissoc views key) old-view])))
	 data (.deregisterView peer model client)]
     (.close server)
     ;;(.close ^SynchronousClient (Proxy/getInvocationHandler client)) ;; TODO - we need to remember the client
     data)))

(defn ^Model -find [^org.dada.core.RemoteSession this ^Model model key]
  (with-record (immutable this) [^Session peer] (.find peer model key)))

(defn ^Data -getData [^org.dada.core.RemoteSession this ^Model model]
  (with-record (immutable this) [^Session peer] (.getData peer model)))

(defn ^Model -query [^org.dada.core.RemoteSession this ^String query]
  (with-record (immutable this) [^Session peer] (.query peer query)))

(defn -hack [^org.dada.core.RemoteSession this ^Remoter remoter]
  (debug "hacking: " this)
  (with-record
   (immutable this)
   [send-to]
   (let [ping-period 5000		;TODO - hardwired
	 client (.syncClient remoter send-to)
	 ^Session peer (SessionProxy. (fn [i] (.sendSync client i)) (fn [i] (.sendAsync client i)))
	 ^Timer timer (doto (Timer.) (.schedule (proxy [TimerTask][](run [] (.ping peer))) 0 ping-period))
	 ^Atom views (atom nil)]
     (.set ^AtomicReference (.state this)
	   (ImmutableState. send-to client peer remoter timer views))))
  (debug "hacked: " this))
