(ns leiningen.cljs-test
  (:require [leiningen.core.main :as lmain]
            [leiningen.cljsbuild :as cljsbuild]
            [clojure.pprint :refer [pprint]]))

(def js-envs #{:phantom :slimer :node})

(defn cljs-test
  "I don't do a lot."
  [project js-env & cljsbuild-args]
  {:pre [(contains? js-envs (keyword js-env))]}
  (pprint (:cljsbuild project))
  (apply cljsbuild/cljsbuild project cljsbuild-args))
