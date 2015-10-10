(ns leiningen.doo
  "Provides a command line wrapper around doo.core/run-script.
   See the main function: doo"
  (:require [clojure.java.io :as io] 
            [clojure.string :as str]
            [leiningen.core.main :as lmain]
            [leiningen.core.eval :as leval]
            [doo.core :as doo]))

;; ====================================================================== 
;; Leiningen Boilerplate

;; TODO: what's this for?
;; I think it is to ensure that the source paths are in the classpath 
;; Then it might resolved to (update-in [:source-paths] concat)
(defn make-subproject [project builds]
  (-> project
    ;; This might be protecting against something, remove?
    (select-keys [:checkout-deps-shares
                  :eval-in
                  :jvm-opts
                  :local-repo
                  :repositories
                  :resource-paths
                  :dependencies])
    ;; This might be the important function
    (merge {:local-repo-classpath true
            :source-paths (concat
                            (:source-paths project)
                            (mapcat :source-paths builds))})
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

        (vector? builds) (mapv #(update-in % [:compiler] correct-main) builds)

        :else (throw (Exception. ":cljsbuild :builds needs to be either a vector or a map"))))))

(defn find-by-id
  "Out of a seq of builds, returns the one with the given id"
  [builds id]
  (first (filter #(= id (:id %)) builds)))

;; ====================================================================== 
;; doo

(def help-string
"
doo - run cljs.test in any JS environment.

Usage:

  lein doo {js-env} {build-id}

  lein doo {js-env} {build-id} {watch-mode}

  - js-env: slimer, phantom, rhino, node, chrome, firefox, safari, ie, or opera.
  - build-id: any of the ids under the :cljsbuild map in your project.clj
  - watch-mode (optional): either auto (default) or once\n")


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
         project' (-> project
                      correct-builds
                      (add-dep ['doo "0.1.6-SNAPSHOT"]))
         builds (get-in project' [:cljsbuild :builds])
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
              (let [ok# (->> ~js-envs
                          (map (fn [e#]
                                 (doo.core/print-env e#)
                                 (doo.core/run-script e# ~compiler ~doo-opts)))
                          (every? (comp zero? :exit)))]
                (System/exit (if ok# 0 1)))))))))
