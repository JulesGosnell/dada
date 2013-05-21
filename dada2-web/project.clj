(defproject org.dada2/web "1.0.0-SNAPSHOT" ; version "1.0.0-SNAPSHOT"
  :dependencies [
                 [org.dada/dada2-core "1.0-SNAPSHOT"]
                 [com.vaadin/vaadin-server "7.1.0.beta1"]
                 [com.vaadin/vaadin-client-compiled "7.1.0.beta1"]
                 [com.vaadin/vaadin-client "7.1.0.beta1"]
                 [com.vaadin/vaadin-themes "7.1.0.beta1"]
                 [com.vaadin/vaadin-push "7.1.0.beta1"]
                 [javax.servlet/servlet-api "2.4" :scope "provided"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 ]
  :plugins [[lein-servlet "0.2.0"]]
  :servlet {:deps [[lein-servlet/adapter-jetty7 "0.2.0"]] :config {:engine :jetty :host "0.0.0.0" :port 8080}}
  :aot :all
  :repl-options {:init-ns org.dada2.web.content :host "0.0.0.0" :port 7888}
  :warn-on-reflection true
  :global-vars {*warn-on-reflection* true *assert* false}
  :source-paths ["src/main/clojure"]
  :test-paths ["test" "src/test/clojure"]
  :resource-paths ["src/main/resource"]
  :compile-path "target/classes"
  :target-path "target/"
  :javac-options ["-target" "1.6" "-source" "1.6" "-Xlint:-options"]
  :omit-source true
  :jvm-opts ["-Xmx1g"]
  :parent [org.dada/dada "1.0-SNAPSHOT" :relative-path "../pom.xml"]
  )
