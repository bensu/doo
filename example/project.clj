(defproject lein-doo-example "0.1.12-SNAPSHOT"
  :description "Project to test lein-doo. Do not take it as an example"
  :url "https://github.com/bensu/doo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [andare "0.9.0"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.12-SNAPSHOT"]]

  :source-paths ["src" "test" "failing-tests"]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/" "out"]

  :doo {:build "test"
        :paths {:slimer "./node_modules/.bin/slimerjs"}
        :alias {:default [:slimer]
                :browsers [:chrome :firefox]
                :all [:browsers :headless]}
        :karma {:launchers {:chrome-no-security {:plugin "karma-chrome-launcher" :name "Chrome_no_security"}}
                :config {"customLaunchers"
                         {"Chrome_no_security" {"base" "Chrome"
                                                "flags" ["--disable-web-security"]}}}}}

  :jvm-opts ["-Xmx1g"]

  :cljsbuild
  {:builds {:dev                {:source-paths ["src"]
                                 :compiler     {:output-to     "resources/public/js/dev.js"
                                                :main          example.core
                                                :optimizations :none}}
            :test               {:source-paths ["src" "test"]
                                 :compiler     {:output-to     "out/testable.js"
                                                :main          example.runner
                                                :optimizations :simple}}
            :advanced           {:source-paths ["src" "test"]
                                 :compiler     {:output-to     "out/testable.js"
                                                :main          "example.runner"
                                                :optimizations :advanced}}
            :none-test          {:source-paths ["src" "test"]
                                 :compiler     {:output-to     "out/testable.js"
                                                :main          example.runner
                                                :source-map    true
                                                :optimizations :none}}
            :node-none          {:source-paths ["src" "test"]
                                 :compiler     {:output-to     "out/testable.js"
                                                :main          example.runner
                                                :optimizations :none
                                                :target        :nodejs}}
            :node-advanced      {:source-paths ["src" "test"]
                                 :compiler     {:output-to     "out/testable.js"
                                                :main          example.runner
                                                :optimizations :advanced
                                                :target        :nodejs}}

            ;; These cljsbuild configs are for CI testing only.
            :dev-fail           {:source-paths ["src"]
                                 :compiler     {:output-to     "resources/public/js/dev.js"
                                                :main          example.core
                                                :optimizations :none}}
            :test-fail          {:source-paths ["src" "failing-tests"]
                                 :compiler     {:output-to     "out/testable.js"
                                                :main          'example.failing-runner
                                                :optimizations :simple}}
            :advanced-fail      {:source-paths ["src" "failing-tests"]
                                 :compiler     {:output-to     "out/testable.js"
                                                :main          "example.failing-runner"
                                                :optimizations :advanced}}
            :none-test-fail     {:source-paths ["src" "failing-tests"]
                                 :compiler     {:output-to     "out/testable.js"
                                                :main          example.failing-runner
                                                :source-map    true
                                                :optimizations :none}}
            :node-none-fail     {:source-paths ["src" "failing-tests"]
                                 :compiler     {:output-to     "out/testable.js"
                                                :main          example.failing-runner
                                                :optimizations :none
                                                :target        :nodejs}}
            :node-advanced-fail {:source-paths ["src" "failing-tests"]
                                 :compiler     {:output-to     "out/testable.js"
                                                :main          example.failing-runner
                                                :optimizations :advanced
                                                :target        :nodejs}}}})
