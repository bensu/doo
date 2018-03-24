(ns doo.coverage
  (:require [meta-merge.core :refer [meta-merge]]))

(defn- package->preprocessors
  "Adds preprocessors for sources from a given packages"
  [->out-dir package]
  (let [package-path (-> (name package)
                         (clojure.string/replace #"\." "/")
                         (clojure.string/replace #"-" "_"))
        package-files #(->out-dir (str "/" package-path %))
        preprocessors ["coverage"]]
    [(package-files "/**/!(*test).js") preprocessors]))

(defn- ->preprocessors
  "Creates preprocessors for all the packages"
  [->out-dir packages]
  (println packages)
  (into {}
        (comp
         (mapcat (partial package->preprocessors ->out-dir))
         (partition-all 2))
        packages))

(defn- coverage-packages
  "Obtains :packages provided in the leiningen configuration
  where coverage is to be added"
  [coverage-opts]
  (-> coverage-opts
      :packages
      seq))

;; Karma settings

(defn- reporter-settings
  "coverageReporter settings for karma-coverage"
  [coverage-opts]
  (meta-merge
   {"type" "html"               ;; default reporter config
    "dir" "coverage/"
    "includeAllSources" true    ;; honor sources which aren't used by the tests
    "instrumentOptions" {"instanbul" {"noConcat" true}}
    }
   (:reporter coverage-opts)))

;; exposed API

(defn settings
  "Creates coverage config for karma via Instanbul and karma-coverage"
  [->out-dir opts]
  (or
   (when-let [coverage-opts (get opts :coverage)]
     {"preprocessors" (->preprocessors ->out-dir
                                       (coverage-packages coverage-opts))
      "reporters" ["progress" "coverage"]
      "coverageReporter" (reporter-settings coverage-opts)
      "plugins" ["karma-coverage"]
      })
   {}))
