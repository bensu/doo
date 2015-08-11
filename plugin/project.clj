(defproject lein-doo "0.1.4"
  :description "lein-doo is a plugin to run clj.test on different js environments."
  :url "https://github.com/bensu/doo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :scm {:name "git"
        :url "https://github.com/bensu/doo"}

  :deploy-repositories [["clojars" {:creds :gpg}]]

  :eval-in-leiningen true
  
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 [doo "0.1.4"]]

  :plugins [[lein-cljsbuild "1.0.5"]]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/"]

  :cljsbuild
  {:builds {:dev {:source-paths ["src" "test"]
                  :main 'lein-doo.core-test
                  :compiler {:output-to "resources/public/js/testable.js"
                             :optimizations :none}}
            :test {:source-paths ["src" "test"]
                   :compiler {:output-to "resources/public/js/testable.js"
                              :main 'lein-doo.runner
                              :optimizations :whitespace}}}})
