(defproject doo "0.1.5"
  :description "doo is a library to run clj.test on different js environments."
  :url "https://github.com/bensu/doo"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :scm {:name "git"
        :url "https://github.com/bensu/doo"}

  :deploy-repositories [["clojars" {:creds :gpg}]]
  
  :test-paths ["test/clj"]

  :resource-paths ["resources"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]
                 [selmer "0.8.9"]
                 [karma-reporter "0.1.0"]])
