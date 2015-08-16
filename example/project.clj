(defproject lein-doo-example "0.1.5-SNAPSHOT"
  :description "Project to test lein-doo. Do not take it as an example"
  :url "https://github.com/bensu/doo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-doo "0.1.5-SNAPSHOT"]]

  :source-paths ["src" "test"]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/"]
  
  :doo {:paths {:karma "karma"}
        :alias {:browsers [:chrome]
                :all [:browsers :headless]}}

  :cljsbuild
  {:builds {:dev {:source-paths ["src" "dev"]
                  :main 'example.dev 
                  :compiler {:output-to "resources/public/js/dev.js"
                             :optimizations :none}}
            :test {:source-paths ["src" "test"]
                   :compiler {:output-to "target/testable.js"
                              :output-dir "target/test"
                              :main 'example.runner
                              :optimizations :whitespace}}
            :advanced {:source-paths ["src" "test"]
                       :compiler {:output-to "target/testable.js"
                                  :main 'example.runner
                                  :optimizations :advanced}}
            :none-test {:source-paths ["src" "test"]
                        :compiler {:output-to "target/testable.js"
                                   :main 'example.runner
                                   :optimizations :none}}
            :node-none {:source-paths ["src" "test"]
                        :compiler {:output-to "target/testable.js"
                                   :output-dir "target/node"
                                   :main 'example.runner
                                   :hashbang false
                                   :optimizations :none
                                   :target :nodejs}}
            ;; FIX: not working
            :node-advanced {:source-paths ["src" "test"]
                            :compiler {:output-to "target/testable.js"
                                       :main 'example.runner
                                       :hashbang false
                                       :optimizations :advanced
                                       :target :nodejs}}}})
