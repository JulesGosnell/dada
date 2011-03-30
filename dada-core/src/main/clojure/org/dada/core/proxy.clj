(ns org.dada.core.proxy
  (:import
   [java.lang.reflect Method]
   )
  )

;; creates a type which when instantiated with a function, will call
;; that function on each invocation of a method on the instance. The
;; function is passed a function which represents the invocation just
;; made, so that we can make it on another object.

;; e.g.
;; org.dada.core.proxy=> (import org.dada.core.View)
;; org.dada.core.View
;; org.dada.core.proxy=> (clojure.pprint/pprint (macroexpand-1 '(defproxy-type ViewProxy org.dada.core.View)))
;; (clojure.core/deftype
;;  ViewProxy
;;  [invoker]
;;  org.dada.core.View
;;  (update
;;   [this arg0 arg1 arg2]
;;   (invoker (fn [target] (.update target arg0 arg1 arg2)))))
;; nil
;; org.dada.core.proxy=> (def v (proxy [View][](update [& args] (println "View:update(" args ")"))))
;; #'org.dada.core.proxy/v
;; org.dada.core.proxy=> (defproxy-type ViewProxy org.dada.core.View)
;; org.dada.core.proxy.ViewProxy
;; org.dada.core.proxy=> (def vp (ViewProxy. (fn [func] (println "->")(func v)(println "<-"))))
;; #'org.dada.core.proxy/vp
;; org.dada.core.proxy=> (.update v nil nil nil)
;; View:update( (nil nil nil) )
;; nil
;; org.dada.core.proxy=> (.update vp nil nil nil)
;; ->
;; View:update( (nil nil nil) )
;; <-
;; nil
;; org.dada.core.proxy=> 

(defn make-proxy-method [^Class interface ^Method method]
   (let [method-name (.getName method)
 	params (map (fn [i] (symbol (str "arg" i))) (range (count (.getParameterTypes method))))]
     (list
      (symbol method-name) (apply vector 'this params)
      (list 'invoker (list 'fn (vector (with-meta 'target {:tag (.getName ^Class (eval interface))})) (apply list (symbol (str "." method-name)) 'target params)))
      )))

(defmacro defproxy-type [name & interfaces]
  (let [fields '[invoker]]
    `(deftype ~name ~fields
       ~@(mapcat
	  (fn [interface]
	      (concat
	       [interface]
	       (map
		(fn [^Method method]
		    (make-proxy-method interface method))
		(.getMethods ^Class (eval interface)))))
	  interfaces)
       )))

;; TODO - handle method param overloading - I'll leave that one for later :-)

;; this should be better than using a java.lang.proxy as there is no
;; introspection needed to invoke said method on other side - we just
;; pull the class that implements the fn over (only the first time)
;; then execute the function's bytecode every time we receive the
;; invocation - no further compilation, introspection or anything :-)

;; we should be able to implement a similar proxy/interceptor that
;; passes the method id through so that it can be used to compose and
;; XML message
;; at the other end the XML xould be unpacked and the method looked up
;; through symbol evaluation or introspection - i should do some
;; timings to see which is fastest.
