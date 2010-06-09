(ns 
 #^{:author "Jules Gosnell" :doc "HTTP Class Server for DADA"}
 org.dada.web
 (:use [org.dada.core])
 (:import [org.eclipse.jetty.server
	   Request Server]
	  [org.eclipse.jetty.server.handler
	   AbstractHandler]
	  [java.io
	   OutputStream]	  
	  [javax.servlet.http
	   HttpServletRequest HttpServletResponse])
 )


(def #^Server *jetty* nil)

(defn start-jetty [port]
  (let [jetty (Server. port)
	handler (proxy [AbstractHandler] []
		       (handle [#^String target
				#^Request base-request
				#^Request request
				#^HttpServletResponse response] 
			       (let [path (.getPathInfo base-request)
				     name (.substring path (+ (.lastIndexOf path "/") 1))
				     #^"[B" bytes (@*exported-classes* name)
				     #^OutputStream stream (.getOutputStream response)]
				 (.setContentType response "application/binary")
				 (.setContentLength response (count bytes))
				 (.setStatus response 200)
				 (println "Serving" request)
				 (println "Serving" name "=" bytes)
				 (.write stream bytes)
				 (.close stream))
			       (.setHandled request true)))]
    (.setHandler jetty handler)
    (def #^Server *jetty* jetty)
    (.start jetty)))

(defn stop-jetty []
  (.stop *jetty*))
