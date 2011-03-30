(ns org.dada.core.RemoteView
 (:use
  [clojure.contrib logging]
  [org.dada.core utils]
  [org.dada.core proxy]
  [org.dada jms])
 (:import
  [java.util
   Collection]
  [java.util.concurrent.atomic
   AtomicReference]
  [org.dada.core
   View]
  ;; TODO - lose dep on JMS
  [org.dada.jms
   ServiceFactory]
  )
 (:gen-class
  :implements [org.dada.core.View java.io.Serializable]
  :constructors {[Object] []}
  :methods [[hack [org.dada.jms.ServiceFactory] void]]
  :init init
  :state state
  )
 )

(defrecord ImmutableState [^Object send-to ^View peer])

(defproxy-type ViewProxy View)

;; proxy for a client-side View

(defn -init [^Object send-to]
  [ ;; super ctor args
   []
   ;; instance state - held in an AtomicReference so we can change it (a Clojure Atom will not serialise)
   (AtomicReference. (ImmutableState. send-to nil))])

(defn ^ImmutableState immutable [^org.dada.core.RemoteView this]
  (.get ^AtomicReference (.state this)))

(defn -update [^org.dada.core.RemoteView this insertions alterations deletions]
  (with-record
   (immutable this)
   [^View peer]
   (.update peer insertions alterations deletions)))

(defn -hack [^org.dada.core.RemoteView this ^ServiceFactory service-factory]
  (debug "hacking: " this)
  (with-record
   (immutable this)
   [send-to]
   (let [client (.syncClient service-factory send-to)
	 ;; TODO - this should be sendSync ultimately (when syncClient supports async interaction)
	 ^View peer (ViewProxy. (fn [invocation] (.sendAsync client invocation)))]
     (.set ^AtomicReference (.state this)
	   (ImmutableState. send-to peer))))
  (debug "hacked: " this))

;;------------------------------------------------------------------------------

(defn -equals [^org.dada.core.RemoteView this that]
  (and
   that
   (instance? org.dada.core.RemoteView that)
   (.equals
    (with-record (immutable this) [send-to] send-to)
    (with-record (immutable that) [send-to] send-to))))

(defn -hashCode [^org.dada.core.RemoteView this]
  (with-record (immutable this) [send-to]
	       (.hashCode send-to)))
