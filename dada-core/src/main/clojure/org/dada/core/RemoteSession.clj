(ns org.dada.core.RemoteSession
 (:use
  [clojure.contrib logging]
  [org.dada.core utils]
  )
 (:import
  [java.lang.reflect
   Proxy]
  [java.util
   ArrayList
   Collection
   List
   Timer
   TimerTask]
  [java.util.concurrent
   ExecutorService]
  [javax.jms
   Queue
   TemporaryQueue]
  [clojure.lang
   Atom]
  [org.dada.core
   Data
   Model
   Session
   SessionManager
   View]
  [org.dada.jms
   RemotingFactory
   RemotingFactory$Server
   SynchronousClient]
  )
 (:gen-class
  :implements [org.dada.core.Session java.io.Serializable]
  :constructors {[javax.jms.TemporaryQueue Boolean] []}
  :methods [[hack [javax.jms.Session java.util.concurrent.ExecutorService] void]]
  :init init
  :state state
  )
 )

(defrecord ImmutableState [^TemporaryQueue queue ^Boolean async ^Session peer ^javax.jms.Session jms-session ^ExecutorService thread-pool ^RemotingFactory remoting-factory ^Timer timer ^Atom views])

;; proxy for a server-side Session
;; intercept outward-bound local Views and replace with proxies

(defn -init [^javax.jms.TemporaryQueue queue ^Boolean async]
  (debug "init: " queue async)
  [ ;; super ctor args
   []
   ;; instance state - held in an ArrayList so we can change it (an Atom will not serialise)
   (ArrayList. ^Collection [(ImmutableState. queue async nil nil nil nil nil nil)])])

(defn ^ImmutableState immutable [^org.dada.core.RemoteSession this]
  (first (.state this)))

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
     [^Session peer ^RemotingFactory remoting-factory ^javax.jms.Session jms-session thread-pool views]
     (debug "registerView" model view)
     (let [topic (.createTopic jms-session (str "DADA." (.getName model))) ;TODO - hardwired prefix and Destination type
	   server (.createServer2 remoting-factory view topic thread-pool)
	   ^View client (.createSynchronousClient remoting-factory topic true)]
       (swap! views (fn [views] (assoc views [view model] [topic server client])))
       (.registerView peer model client))))

(defn ^Data -deregisterView [^org.dada.core.RemoteSession this ^Model model ^View view]
  (with-record
   (immutable this)
   [^Session peer views]
   (debug "deregisterView" model view)
   (let [[_ [^Topic topic ^RemotingFactory$Server server ^Proxy client]]
	 (swap2! views (fn [views] (let [key [view model] old-view (views key)] [(dissoc views key) old-view])))
	 data (.deregisterView peer model client)]
     (.close server)
     (.close ^SynchronousClient (Proxy/getInvocationHandler client))
     data)))

(defn ^Model -find [^org.dada.core.RemoteSession this ^Model model key]
  (with-record (immutable this) [^Session peer] (.find peer model key)))

(defn -hack [^org.dada.core.RemoteSession this ^javax.jms.Session jms-session ^java.util.concurrent.ExecutorService thread-pool]
  (debug "hacking: " this)
  (with-record
   (immutable this)
   [^Queue queue ^Boolean async]
   (let [ping-period 5000		;TODO - hardwired
	 remoting-timeout 10000		;TODO - hardwired
	 ^RemotingFactory session-remoting-factory (RemotingFactory. jms-session Session remoting-timeout)
	 ^RemotingFactory view-remoting-factory (RemotingFactory. jms-session View remoting-timeout)
	 ^Session peer (.createSynchronousClient session-remoting-factory queue async)
	 ^Timer session-timer (doto (Timer.) (.schedule (proxy [TimerTask][](run [] (.ping peer))) 0 ping-period))
	 ^Atom views (atom nil)]
     (.set ^List (.state this) 0
	   (ImmutableState. queue async peer jms-session thread-pool view-remoting-factory session-timer views))))
  (debug "hacked: " (first (.state this))))
