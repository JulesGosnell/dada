
;; load core.clj

(def metamodel (start-server "Server0"))
(start-client "Server0")

(def jules
     (proxy
      [org.dada.core.ModelView]
      []
      (start [])
      (stop [])
      (getName [] "Jules")
      (registerView [view])
      (deregisterView [view])
      (update [insertions updates deletions])
      )
     )

(def jane
     (proxy
      [org.dada.core.ModelView]
      []
      (start [])
      (stop [])
      (getName [] "Jane")
      (registerView [view])
      (deregisterView [view])
      (update [insertions updates deletions])
      )
     )

(def xav
     (proxy
      [org.dada.core.ModelView]
      []
      (start [])
      (stop [])
      (getName [] "Xav")
      (registerView [view])
      (deregisterView [view])
      (update [insertions updates deletions])
      )
     )

(def iona
     (proxy
      [org.dada.core.ModelView]
      []
      (start [])
      (stop [])
      (getName [] "Iona")
      (registerView [view])
      (deregisterView [view])
      (update [insertions updates deletions])
      )
     )

(insert metamodel jules)
(insert metamodel jane)
(insert metamodel iona)
(insert metamodel xav)
(update metamodel jules xav)
(delete metamodel jane)
(delete metamodel xav)

;; create a router to select via a lambda
;; create a new model and plug it in to router
;; standback and watch the fun...

(import org.dada.core.Router)
(import org.dada.core.Router$Strategy)
(import org.dada.core.VersionedModelView)
(import org.dada.core.GetterMetadata)
(import org.dada.core.Getter)

;; TODO: should not be using versioning...

(def query
     (VersionedModelView.
      "Query-0"
      (GetterMetadata. (list "key") (list (proxy [Getter] [] (get [value] value))))
      (proxy [Getter] [] (get [value] value)) ;key
      (proxy [Getter] [] (get [value] 0)) ;version
      ))

(insert metamodel query)

(def router
     (Router.
      (proxy
       [Router$Strategy]
       []
       (getMutable [] false)
       (getRoute [#^String value] 0)
       (getViews [#^Integer route] (list query))
       )))

(connect metamodel router)
(disconnect metamodel router)




;;(map #(if (.startsWith % "S") "Weekend!" %) days)

;; need to get contents of registration and insert into router...

