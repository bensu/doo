(ns leiningen.doo
  "Provides a command line wrapper around doo.core/run-script.
   See the main function: doo"
  (:require [clojure.java.io :as io] 
            [clojure.string :as str]
            [doo.core :as doo]
            [leiningen.core.main :as lmain]
            [leiningen.cljsbuild.config :as config]
            [leiningen.cljsbuild.subproject :as subproject]
            [leiningen.core.eval :as leval]
            [clojure.pprint :refer [pprint]]))

;; Assumes the project is packaged in the same jar
(defn get-lib-version [proj-name]
  {:pre [(string? proj-name)]}
  (let [[_ coords version]
        (-> (io/resource (str "META-INF/leiningen/" proj-name 
                           "/" proj-name "/project.clj"))
          slurp
          read-string)]
    (assert (= coords (symbol proj-name))
      (str "Something very wrong, could not find " proj-name
        "'s project.clj, actually found: " coords))
    (assert (string? version)
      (str "Something went wrong, version of " proj-name
        " is not a string: " version))
    version))

;; Needed to ensure cljsbuild compatibility
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

(defn add-dep
  "Adds one dependency (needs to be a vector with a quoted symbol)
  to the project's dependencies.
  Ex: (add-dep project ['doo \"0.1.0-SNAPSHOT\"])"
  [project dep]
  (update-in project [:dependencies] #(conj % dep)))

;; well this is private in the leiningen.cljsbuild ns & figwheel!
(defn run-local-project
  "Runs both forms (requires and form) in the context of the project"
  [project builds requires form]
  (let [project' (-> project
                   (make-subproject builds)
                   ;; just for use inside the plugin
                   (dissoc :eval-in))]
    (leval/eval-in-project project'
      `(try
         (do
           ~form
           (System/exit 0))
         (catch Exception e#
           (do
             (.printStackTrace e#)
             (System/exit 1))))
      requires)))

(def help-string
"
doo - run cljs.test in any JS environment.

Usage:

  lein doo {js-env} {build-id}

  lein doo {js-env} {build-id} {watch-mode}

  - js-env: slimer, phantom, rhino, node, chrome, firefox, safari, ie, or opera.
  - build-id: any of the ids under the :cljsbuild map in your project.clj
  - watch-mode (optional): either auto (default) or once\n")

(defn find-by-id
  "Out of a seq of builds, returns the one with the given id"
  [builds id]
  (first (filter #(= id (:id %)) builds)))

(defn ^{:doc help-string}
  doo 
  ([project] (lmain/info help-string))
  ([project js-env]
   (lmain/info
     (str "We have the js-env (" js-env
       ") but we are missing the build-id. See `lein doo` for help.")))
  ([project js-env-alias build-id] (doo project js-env-alias build-id "auto"))
  ([project js-env-alias build-id watch-mode]
   (assert (contains? #{"auto" "once"} watch-mode)
     (str "Possible watch-modes are auto or once, " watch-mode " was given."))
   ;; FIX: execute in a try catch like the one in run-local-project
   (let [doo-opts (:doo project)
         js-envs (doo/resolve-alias (keyword js-env-alias) (:alias doo-opts))
         ;; FIX: get the version dynamically
         project' (add-dep project ['doo "0.1.5-SNAPSHOT"])
         builds (-> project' config/extract-options :builds)
         {:keys [source-paths compiler] :as build} (find-by-id builds build-id)]
     (doo/assert-alias js-env-alias js-envs)
     (doseq [js-env js-envs]
       (doo/assert-js-env js-env))
     (assert (not (empty? build))
       (str "The given build (" build-id ") was not found in these options: "
         (str/join ", " (map :id builds))))
     (doseq [js-env js-envs]
       (doo/assert-compiler-opts js-env compiler))
     ;; FIX: there is probably a bug regarding the incorrect use of builds
     (run-local-project project' [builds]
       '(require 'cljs.build.api 'doo.core)
       (if (= "auto" watch-mode)
         `(cljs.build.api/watch
            (apply cljs.build.api/inputs ~source-paths)
            (assoc ~compiler
              :watch-fn
              (fn []
                (doseq [js-env# ~js-envs]
                  (doo.core/print-env js-env#)
                  (doo.core/run-script js-env# ~compiler ~doo-opts)))))
         `(do (cljs.build.api/build
                (apply cljs.build.api/inputs ~source-paths) ~compiler)
              (let [rs# (map #(do (doo.core/print-env %)
                                  (doo.core/run-script % ~compiler ~doo-opts))
                             ~js-envs)
                    exit-code# (if (some (comp not zero? :exit) rs#) 1 0)]
                (System/exit exit-code#))))))))
