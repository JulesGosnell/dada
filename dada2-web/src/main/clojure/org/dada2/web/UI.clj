(ns org.dada2.web.UI
  (:use
   [org.dada2.web content])
  (:gen-class
   :name ^{com.vaadin.annotations.Push
           {"value" com.vaadin.shared.communication.PushMode/AUTOMATIC}} org.dada2.web.UI
   :extends com.vaadin.ui.UI))

(set! *warn-on-reflection* true)

(defn -init [^org.dada2.web.UI ui request]
  (println "ANNOTATIONS: " (.getClass ui) " - " (into [] (.getAnnotations (.getClass ui))))
  (doto ui (.setContent (create-main-layout ui))))

;; TODO
;;; embed repl
