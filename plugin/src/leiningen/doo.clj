(ns leiningen.doo
  "Provides a command line wrapper around doo.core/run-script.
   See the main function: doo"
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [leiningen.core.main :as lmain]
            [leiningen.core.eval :as leval]
            [doo.core :as doo]))

;; ======================================================================
;; Leiningen Boilerplate

(defn project->builds [project]
  (get-in project [:cljsbuild :builds]))

(defn add-sources [project sources]
  (update-in project [:source-paths] #(into % sources)))

;; TODO: what's this for?
(defn make-subproject [project]
  (-> project
      ;; This might be protecting against something, remove?
      (select-keys [:checkout-deps-shares
                    :eval-in
                    :jvm-opts
                    :local-repo
                    :repositories
                    :resource-paths
                    :source-paths
                    :dependencies
                    :managed-dependencies])
      (assoc :local-repo-classpath true)
      (with-meta (meta project))))

(defn add-dep
  "Adds one dependency (needs to be a vector with a quoted symbol)
  to the project's dependencies.
  Ex: (add-dep project ['doo \"0.1.0-SNAPSHOT\"])"
  [project dep]
  (update-in project [:dependencies] #(conj % dep)))

;; well this is private in the leiningen.cljsbuild ns & figwheel!
(defn run-local-project
  "Runs both forms (requires and form) in the context of the project"
  [project requires form]
  (let [project' (-> project
                   make-subproject
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

;; ======================================================================
;; cljsbuild opts

(defn correct-main [compiler-opts]
  (cond-> compiler-opts
    (and (some? (:main compiler-opts))
         (not (seq? (:main compiler-opts))))
    (update-in [:main] name)))

(defn correct-builds [project]
  (update-in project [:cljsbuild :builds]
    (fn [builds]
      (cond
        (map? builds)
        (mapv (fn [[k v]]
                (-> (assoc v :id (name k))
                  (update-in [:compiler] correct-main)))
          builds)

        (or (vector? builds) (seq? builds))
        (mapv #(update-in % [:compiler] correct-main) builds)

        :else (throw (Exception. ":cljsbuild :builds needs to be either a vector or a map"))))))

(defn find-by-id
  "Out of a seq of builds, returns the one with the given id"
  [builds id]
  {:pre [(not (empty? builds))]}
  (let [build (first (filter #(= id (:id %)) builds))]
    (assert (not (empty? build))
      (str "The given build (" id ") was not found in these options: "
        (str/join ", " (map :id builds))))
    build))

;; ======================================================================
;; CLI

(defn default? [cli-opt]
  (or (nil? cli-opt) (= :default cli-opt)))

(defn watch-mode? [arg]
  (contains? #{"auto" "once"} arg))

(defn args->cli
  "Parses & validates the cli arguments into a consistent format"
  [args]
  (let [[js-env build-id & xs] (remove watch-mode? args)]
    (assert (empty? xs)
      (str "We couldn't parse " xs " as a watch-mode,"
        " only auto or once are supported"))
    {:alias (keyword (or js-env "default"))
     :build (or build-id :default)
     :watch-mode (keyword (or (first (filter watch-mode? args)) "auto"))}))

(defn cli->js-envs
  "Returns the js-envs where doo should be run from the cli arguments
   and the project.clj options"
  [{cli-alias :alias} {alias-map :alias :or {alias-map {}}}]
  (assert (not (and (default? cli-alias) (not (contains? alias-map :default))))
          "\n
 To call lein doo without a js-env you need a :default :alias in
 your project.clj and a default build. For example:

   {:doo {:build {:source-paths [\"src\" \"test\"]}
          :alias {:default [:firefox]}}}

 then you can simply use

   lein doo\n")
  (doo/resolve-alias cli-alias alias-map))

;; Not being used
(defn project->test-build [project]
  {:source-paths (set/union (set (:source-paths project))
                            (set (:test-paths project)))
   :compiler {:output-to "out/doo-testable.js"
              :optimizations :simple}})

(defn cli->build
  "Returns the build form the cli arguments and the project options"
  [{cli-build :build} project {opts-build :build}]
  {:post [(not (empty? (:source-paths %)))]}
  (assert (or (nil? opts-build) (string? opts-build))
    (let [build-ids (map :id (project->builds project))]
      (str "\n\n Incorrect value for :doo :build: " opts-build "\n"
        "
 The default build under :doo :build should be a string with
 the build-id of a build under :cljsbuild build"
        (if (empty? build-ids)
        ". For example:

 {:doo {:build \"test-build\"}\n"
        (str " like " (str/join ", " (map pr-str build-ids)))) "\n")))
  (assert (not (and (default? cli-build) (empty? opts-build)))
    "\n
 To call lein doo without a build id, you need to configure a build
 in your project.clj. For example:

 {:doo {:build \"test-build\"}}

 where \"test-build\" can be found under :cljsbuild. Then you can
 call that build with:

   lein doo phantom\n")
  (if-let [build-id (if (default? cli-build)
                      opts-build
                      cli-build)]
    (find-by-id (project->builds project) build-id)
    (project->test-build project)))

;; ======================================================================
;; doo

(def help-string
"
doo - run cljs.test in any JS environment.

Usage:

  lein doo {js-env}

  lein doo {js-env} {build-id}

  lein doo {js-env} {build-id} {watch-mode}

  - js-env: slimer, phantom, node, chrome, firefox, or an alias like headless
  - build-id: any of the ids under the :cljsbuild map in your project.clj
  - watch-mode: either auto (default) or once\n

All arguments are optional provided there is a corresponding default under :doo
in project.clj.\n")

(defn ^{:doc help-string}
  doo
  ([project]
   (doo project "default" :default "auto"))
  ([project & args]
   ;; FIX: execute in a try catch like the one in run-local-project
   (let [{:keys [alias watch-mode] :as cli} (args->cli args)
         opts (:doo project)
         js-envs (cli->js-envs cli opts)
         ;; FIX: get the version dynamically
         project' (-> project
                      correct-builds
                      (add-dep ['doo "0.1.7"]))
         {:keys [source-paths compiler]}
         (cli->build cli project' opts)]
     (doo/assert-alias alias js-envs (:alias opts))
     (doseq [js-env js-envs]
       (doo/assert-js-env js-env))
     ;; FIX: there is probably a bug regarding the incorrect use of builds
     ;; Important to add sources to the classpath
     (run-local-project (add-sources project' source-paths)
       '(require 'cljs.build.api 'doo.core 'doo.karma)
       `(let [compiler# (cljs.build.api/add-implicit-options ~compiler)]
          (doseq [js-env# ~js-envs]
            (doo.core/assert-compiler-opts js-env# compiler#))
          (if (= :auto ~watch-mode)
            (let [karma-envs# (vec (filter doo.karma/env? ~js-envs))
                  karma-on?# (atom false)
                  non-karma-envs# (vec (remove doo.karma/env? ~js-envs))]
              (cljs.build.api/watch
                (apply cljs.build.api/inputs ~(vec source-paths))
                (assoc compiler#
                  :watch-fn
                  (fn []
                    (when (and (not (empty? karma-envs#)) (not @karma-on?#))
                      ;; Karma needs to be installed after
                      ;; compilation, so that the files to be included exist
                      (swap! karma-on?# not)
                      (doo.core/install! karma-envs# compiler# ~opts)
                      ;; We wait for the Karma server to be setup before we kick off tests
                      (Thread/sleep 1000))
                    (doseq [js-env# non-karma-envs#]
                      (doo.core/print-envs js-env#)
                      (doo.core/run-script js-env# compiler# ~opts))
                    (when @karma-on?#
                      (apply doo.core/print-envs karma-envs#)
                      (doo.core/karma-run! ~opts))))))
            (do
              (cljs.build.api/build
                (apply cljs.build.api/inputs ~(vec source-paths)) compiler#)
              (let [ok# (->> ~js-envs
                          (map (fn [e#]
                                 (doo.core/print-envs e#)
                                 (doo.core/run-script e# compiler# ~opts)))
                          (every? (comp zero? :exit)))]
                (System/exit (if ok# 0 1))))))))))
