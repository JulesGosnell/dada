(ns ^{:author "Jules Gosnell" :doc "HTTP Class Server for Clojure classes"} org.dada.clojure.web
    (:use clojure.tools.logging)
    (:import
     [clojure.lang DynamicClassLoader]
     [org.eclipse.jetty.server Request Server]
     [org.eclipse.jetty.server.handler AbstractHandler]
     [java.io InputStream OutputStream ByteArrayOutputStream]
     [javax.servlet.http HttpServletResponse])
    )

;; TODO: we should use a package name filter to avoid serving homonyms with different content...

(defn static-class-bytes [^String path]
  (if-let [is (.getResourceAsStream (.getContextClassLoader (Thread/currentThread)) path)]
      (let [bufsize 256
	    os (ByteArrayOutputStream.)
	    bytes (byte-array bufsize)]
	(try
	 (loop []
	   (let [
		 nbytes (.read is bytes 0 bufsize)]
	     (if (> nbytes 0)
	       (do
		 (.write os bytes 0 nbytes)
		 (recur))
	       ["static class " (.toByteArray os)])))
	 (finally
	  (.close is)
	  (.close os))))))

;; delay evaluation of DynamicClassLoader/byteCodeForName until runtime, when our hacks will be in place...
(def byteCodeForName
     (delay (eval '(fn [name] (DynamicClassLoader/byteCodeForName name)))))

(defn dynamic-class-bytes [^String path-info]
  (if-let [bytes (@byteCodeForName (.replace (.substring path-info 0 (- (.length path-info) 6)) \/ \.))]
      ["dynamic class" bytes]))


;; only works when URLClassLoader has been given a URL ending in '/'
(defn handle-request [^String target ^Request base-request ^Request request ^HttpServletResponse response] 
  (let [^String path-info (.getPathInfo base-request)
	^String path-info (if (.startsWith path-info "/") (.substring path-info 1) path-info)]
    (if-let [[class-type ^"[B" bytes] (or (dynamic-class-bytes path-info) (static-class-bytes path-info))]
	(let [size (count bytes)]
	  (debug (str "Serving " class-type " : " path-info " (" size " bytes)"))
	  (doto response
	    (.setContentType "application/binary")
	    (.setContentLength (count bytes))
	    (.setStatus 200))
	  (with-open [^OutputStream stream (.getOutputStream response)]
	      (doseq [byte bytes] (.write stream (int byte)))))
      (do
	(trace (str "Not Serving: " path-info))
	(doto response
	  (.setContentLength 0)
	  (.setStatus 404)))))
  (.setHandled request true))

(defn ^Server start-jetty [^Integer port]
  (doto (Server. port)
    (.setHandler (proxy [AbstractHandler] [] (handle [& args] (apply handle-request args))))
    (.start)))

(defn stop-jetty [^Server jetty]
  (.stop jetty))
