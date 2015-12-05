(defproject lein-doo "0.1.6"
  :description "lein-doo is a plugin to run clj.test on different js environments."
  :url "https://github.com/bensu/doo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :scm {:name "git"
        :url "https://github.com/bensu/doo"}

  :deploy-repositories [["clojars" {:creds :gpg}]]

  :eval-in-leiningen true

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [doo "0.1.6"]]

  :test-paths ["test/clj" "test/cljs"]

  :clean-targets ^{:protect false} [:target-path "resources/public/js/" "out"]

  :doo {:build "test"
        :alias {:default [:chrome]
                :browsers [:chrome :firefox]
                :dom [:browsers :headless]}}

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-3308"
                                   :scope "provided"]]}}

  :cljsbuild
  {:builds {:test {:source-paths ["test/cljs"]
                   :compiler {:output-to "resources/public/js/testable.js"
                              :main lein-doo.runner
                              :optimizations :whitespace}}}})
