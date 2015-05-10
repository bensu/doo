(ns leiningen.cljs-test
  (:require 
            [leiningen.core.main :as lmain]
            [leiningen.cljsbuild :as cljsbuild]
            [leiningen.cljsbuild.config :as config]
            [leiningen.cljsbuild.subproject :as subproject]
            [leiningen.core.eval :as leval]
            [clojure.pprint :refer [pprint]]))

(def js-envs #{:phantom :slimer :node})

(defn find-build-map [cljs-map build-id]
  )

;; Add my own dependencies
;; (update-in [:dependencies] conj ['figwheel-sidecar figwheel-sidecar-version])

(defn make-subproject [project builds]
  (with-meta
    (merge
      (select-keys project [:checkout-deps-shares
                            :eval-in
                            :jvm-opts
                            :local-repo
                            :repositories
                            :resource-paths])
      {:local-repo-classpath true
       :dependencies (subproject/merge-dependencies (:dependencies project))
       :source-paths (concat
                       (:source-paths project)
                       (mapcat :source-paths builds))})
    (meta project)))

;; well this is private in the leiningen.cljsbuild ns & figwheel!
(defn run-local-project [project builds requires form]
  (let [project' (make-subproject project builds)] 
    (pprint form)
    (leval/eval-in-project (dissoc project' :eval-in)
      `(try
         (do
           ~form
           (System/exit 0))
         (catch Exception e#
           (do
             (.printStackTrace e#)
             (System/exit 1))))
      requires)))

(defn cljs-test
  "I don't do a lot."
  [project js-env build-id]
  {:pre [(contains? js-envs (keyword js-env))]}
  (let [{:keys [source-paths compiler]}
        (first (:builds (config/extract-options project)))]
    (pprint source-paths)
    (pprint compiler)
    (run-local-project project [build-id]
      '(require 'cljs.build.api 'cljs.closure)
      `(cljs.build.api/watch
        (apply cljs.build.api/inputs ~source-paths)
        ~compiler)
      )))
