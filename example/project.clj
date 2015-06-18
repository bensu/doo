(defproject lein-doo-example "0.1.1-SNAPSHOT"
  :description "Project to test lein-doo. Do not take it as an example"
  :url "https://github.com/bensu/doo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0-RC2"]
                 [org.clojure/clojurescript "0.0-3308"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-doo "0.1.1-SNAPSHOT"]]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/"]

  :cljsbuild
  {:builds {:dev {:source-paths ["src" "dev"]
                  :main 'example.dev 
                  :compiler {:output-to "resources/public/js/dev.js"
                             :optimizations :none}}
            :test {:source-paths ["src" "test"]
                   :compiler {:output-to "resources/public/js/testable.js"
                              :main 'example.runner
                              :optimizations :whitespace}}}})
