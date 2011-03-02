(ns 
 #^{:author "Jules Gosnell" :doc "HTTP Class Server for DADA"}
 org.dada.web
 (:use clojure.contrib.logging)
 (:use [org.dada.core])
 (:import
  [clojure.lang DynamicClassLoader]
  [org.eclipse.jetty.server Request Server]
  [org.eclipse.jetty.server.handler AbstractHandler]
  [java.io OutputStream]
  [javax.servlet.http HttpServletResponse])
 )

;; only works when URLClassLoader has been given a URL ending in '/'
(defn handle-request [^String target ^Request base-request ^Request request ^HttpServletResponse response] 
  (let [^String path-info (.getPathInfo base-request)
	class-name (.replace (.substring path-info 1 (- (.length path-info) 6)) \/ \.)]
    (if-let [^"[B" bytes (DynamicClassLoader/byteCodeForName class-name)]
      (let [size (count bytes)]
	(info (str "Serving: " class-name " (" size " bytes)"))
	(doto response
	  (.setContentType "application/binary")
	  (.setContentLength (count bytes))
	  (.setStatus 200))
	(with-open [^OutputStream stream (.getOutputStream response)]
	  (doseq [byte bytes] (.write stream (int byte)))))
      (do
	(info (str "Not Serving: " class-name))
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
