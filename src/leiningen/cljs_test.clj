(ns leiningen.cljs-test
  (:require [leiningen.core.main :as lmain]
            [leiningen.cljsbuild :as cljsbuild]
            [leiningen.core.eval :as leval]
            [clojure.pprint :refer [pprint]]))

(def js-envs #{:phantom :slimer :node})

(defn find-build-map [cljs-map build-id]
  )

(defn cljs-test
  "I don't do a lot."
  [project js-env & cljsbuild-args]
  {:pre [(contains? js-envs (keyword js-env))]}
  (let [project (assoc-in project
                  [:cljsbuild :builds :main :notify-command]
                  ["cat" "project.clj"])])
  (leval/eval-in-project {:dependencies '[[org.clojure/clojure "1.7.0-beta2"]
                                          [org.clojure/tools.reader "0.9.1"]
                                          [lein-cljsbuild "1.0.5"]]
                          :eval-in-leiningen true
                          :plugins '[[lein-cljsbuild "1.0.5"]]} 
    `(apply println ~project  
       ~cljsbuild-args)
    '(require '[leiningen.uberjar])))
