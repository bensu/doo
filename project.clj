(defproject doo "0.1.0-SNAPSHOT"
  :description "doo is a library and lein plugin to run clj.test on different js environments."
  :url "https://github.com/bensu/doo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :scm {:name "git"
        :url "https://github.com/bensu/doo"}

  :deploy-repositories [["clojars" {:creds :gpg}]]

  :eval-in-leiningen true
  
  :resource-paths ["resources"]

  :dependencies [[org.clojure/clojure "1.7.0-beta2"]
                 [org.clojure/clojurescript "0.0-3269"]]

  :plugins [[lein-cljsbuild "1.0.5"]]

  :clean-targets ^{:protect false} ["resources/public/js/"]

  :cljsbuild
  {:builds {:main {:source-paths ["src" "test"]
                   :compiler {:output-to "resources/public/js/testable.js"
                              :main 'test.runner
                              :optimizations :whitespace}}}})
