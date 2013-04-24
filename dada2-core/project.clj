(defproject dada2-core "1.0.0-SNAPSHOT"
  :description "dada :: dada2-core"
  :dependencies [
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [org.clojure/tools.logging "0.2.4"]
                 ]
  :aot :all
  :source-path "src/main/clojure"
  :test-path "src/test/clojure"
  :compile-path "target/classes"
  :target-path "target/"
  :warn-on-reflection true
  :global-vars {*warn-on-reflection* true *assert* false}
  :hooks [leiningen.util.injected]
)

