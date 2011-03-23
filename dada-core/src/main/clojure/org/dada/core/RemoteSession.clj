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

(defrecord ImmutableState [^TemporaryQueue queue ^Boolean async ^Session peer ^javax.jms.Session jms-session ^ExecutorService thread-pool ^RemotingFactory remoting-factory ^Timer timer])
(defrecord MutableState [views])

;; proxy for a server-side Session
;; intercept outward-bound local Views and replace with proxies

(defn -init [^javax.jms.TemporaryQueue queue ^Boolean async]
  (debug "init: " queue async)
  [ ;; super ctor args
   []
   ;; instance state
   (ArrayList. ^Collection [(ImmutableState. queue async nil nil nil nil nil) nil])])

(defn immutable [^org.dada.core.RemoteSession this]
  (first (.state this)))

(defn mutable [^org.dada.core.RemoteSession this]
  (second (.state this)))

(defn -ping [^org.dada.core.RemoteSession this]
  (with-record (immutable this) [^Session peer] (trace "ping: " peer) (.ping peer)))

(defn -close [^org.dada.core.RemoteSession this]
  (let [[immutable mutable] (.state this)
	{views :views} @mutable]
    (with-record
     immutable
     [^Session peer ^Timer timer]
     (debug "close: " peer)
     ;; close outstanding Views
     (doseq [[view model] (keys views)] (.deregisterView this model view))
     (.cancel timer)
     (.close peer))))

;; TODO - only supports one registration per model/view pair - could
;; be a problem if someone uses API wrongly

(defn ^Data -registerView [^org.dada.core.RemoteSession this ^Model model ^View view]
  (let [[immutable mutable] (.state this)]
    (with-record
     immutable
     [^Session peer ^RemotingFactory remoting-factory ^javax.jms.Session jms-session thread-pool]
     (debug "registerView" model view)
     (let [topic (.createTopic jms-session (str "DADA." (.getName model))) ;TODO - hardwired prefix and Destination type
	   server (.createServer2 remoting-factory view topic thread-pool)
	   ^View client (.createSynchronousClient remoting-factory topic true)]
       (swap!
	mutable
	(fn [^MutableState state]
	    (assoc state :views (assoc (:views state) [view model] [topic server client]))))
       (.registerView peer model client)))))

(defn ^Data -deregisterView [^org.dada.core.RemoteSession this ^Model model ^View view]
  (let [[immutable mutable] (.state this)]
    (with-record
     immutable
     [^Session peer]
     (debug "deregisterView" model view)
     (let [[_ [^Topic topic ^RemotingFactory$Server server ^Proxy client]]
	   (swap2!
	    mutable
	    (fn [^MutableState state]
		(let [views (:views state)
		      old-view (views [view model])]
		  [(assoc state :views (dissoc views [view model])) old-view])))
	   data (.deregisterView peer model client)]
       (.close server)
       (.close ^SynchronousClient (Proxy/getInvocationHandler client))
       data))))

(defn ^Model -find [^org.dada.core.RemoteSession this ^Model model key]
  (with-record (immutable this) [^Session peer] (.find peer model key)))

(defn -hack [^org.dada.core.RemoteSession this ^javax.jms.Session jms-session ^java.util.concurrent.ExecutorService thread-pool]
  (debug "hacking: " this)
  (let [^List mutable-list (.state this)
	[immutable mutable] mutable-list]
    (with-record
     immutable
     [^Queue queue ^Boolean async]
     (let [ping-period 5000		;TODO - hardwired
	   remoting-timeout 10000	;TODO - hardwired
	   ^RemotingFactory session-remoting-factory (RemotingFactory. jms-session Session remoting-timeout)
	   ^Session peer (.createSynchronousClient session-remoting-factory queue async)]
       (.set
	mutable-list
	0
	(ImmutableState.
	 queue
	 async
	 peer
	 jms-session
	 thread-pool
	 (RemotingFactory. jms-session View remoting-timeout)
	 (doto (Timer.) (.schedule (proxy [TimerTask][](run [] (.ping peer))) 0 ping-period))))
       (.set mutable-list 1 (atom nil)))))
  (debug "hacked: " (first (.state this))))
