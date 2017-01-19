(ns doo.core
  "Runs a Js script in any Js environment. See doo.core/run-script"
  (:import java.io.File)
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [doo.karma :as karma]
            [doo.shell :as shell]
            [doo.utils :as utils]))

;; ======================================================================
;; JS Environments

;; All js-envs are keywords.

(def doo-envs #{:phantom :slimer :node :rhino :nashorn})

(def js-envs (set/union doo-envs karma/envs))

(def default-aliases {:headless [:slimer :phantom]})

(defn resolve-alias
  "Given an alias it resolves to a list of the js-envs it represents,
   or an empty list if it represents not js-envs. js-envs resolve to
   themselves.

   Ex: (resolve-alias :headless {}) => [:phantom :slimer]
       (resolve-alias :slimer {}) => [:slimer]
       (resolve-alias :something {}) => []"
  [alias alias-table]
  {:pre [(keyword? alias) (or (nil? alias-table) (map? alias-table))]}
  (assert (every? vector? (vals alias-table))
          (format "The values for the alias tables must be vectors but at least one of them, %s, is not.\n\nEx: {:default [:firefox]}"
                  (first (remove vector? (vals alias-table)))))
  (let [alias-table (merge default-aliases alias-table)
        stack-number (atom 0)]
    (letfn [(resolve-alias' [alias]
              (if (< 10 @stack-number)
                (throw (Exception. (str "There is a circular dependency in the alias Map: " (pr-str alias-table))))
                (do (swap! stack-number inc)
                    (cond
                      (contains? js-envs alias) [alias]
                      (contains? alias-table alias)
                      (vec (mapcat resolve-alias' (get alias-table alias)))
                      :else []))))]
      (resolve-alias' alias))))

(defn valid-js-env? [js-env]
  {:pre [(keyword? js-env)]}
  (contains? js-envs js-env))

(defn assert-alias
  ([js-env-alias resolved-js-envs]
   (assert-alias js-env-alias resolved-js-envs {}))
  ([js-env-alias resolved-js-envs user-aliases]
   (assert (not (empty? resolved-js-envs))
     (str "The given alias: " js-env-alias
       " didn't resolve to any runners. Try any of: "
       (str/join ", " (map name (concat js-envs
                                  (keys default-aliases)
                                  (keys user-aliases))))))))

(defn assert-js-env
  "Throws an exception if the js-env is not valid.
   See valid-js-env?"
  [js-env]
  (assert (valid-js-env? js-env)
    (str "The js-env should be one of: "
         (str/join ", " (map name js-envs))
         " and we got: " js-env)))

(defn print-envs [& js-envs]
  (let [env-name (str/join ", " (mapv (comp str/capitalize name) js-envs))]
    (println "")
    (println ";;" (str/join "" (take 70 (repeat "="))))
    (println (str ";; Testing with " env-name ":"))
    (println "")))

;; ======================================================================
;; Runners

;; doo createst the files necessary for the runners and cleans them up
;; when the JVM is shut down which is not ideal when scripting, since
;; it can be called several times in one JVM session.

;; Sadly, we can't just point Phantom to the file inside the jar
;; http://stackoverflow.com/questions/25307667/launching-phantomjs-as-a-local-resource-when-using-an-executable-jar

;; TODO: runner arg is not necessary
(defn runner-path!
  "Creates a temp file for the given runner resource file."
  ([runner filename]
   (runner-path! runner filename {:common? false}))
  ([runner filename {:keys [common?]}]
   (letfn [(slurp-resource [res]
             (slurp (io/resource (str shell/base-dir res))))
           (add-common [file]
             (when common?
               (spit file (slurp-resource "common.js"))))]
     (.getAbsolutePath
       (doto (File/createTempFile (name runner) ".js")
         .deleteOnExit
         add-common
         (spit (slurp-resource filename) :append true))))))

(def default-command-table
  {:phantom "phantomjs"
   :slimer "slimerjs"
   :rhino "rhino"
   :nashorn "jjs"
   :node "node"
   :karma "karma"})

(defn command-table [js-env opts]
  {:post [(some? %)]}
  (some-> default-command-table
    (merge (:paths opts))
    (get js-env)
    (str/split #" ")))

(defmulti js->command*
  (fn [js _ _]
    (if (karma/env? js)
      :karma
      js)))

(defmethod js->command* :phantom
  [_ _ opts]
  [(command-table :phantom opts)
   (runner-path! :phantom "headless.js" {:common? true})
   (runner-path! :phantom-shim "phantomjs-shims.js")])

(defmethod js->command* :slimer
  [_ _ opts]
  [(command-table :slimer opts)
   (runner-path! :slimer "headless.js" {:common? true})])

(defmethod js->command* :rhino
  [_ _ opts]
  [(command-table :rhino opts)
   "-opt" "-1"
   (runner-path! :rhino "rhino.js" {:common? true})])

(defmethod js->command* :nashorn
  [_ _ opts]
  [(command-table :nashorn opts)
   (runner-path! :nashorn "nashorn.js" {:common? true})
   "--"])

(defmethod js->command* :node
  [_ _ opts]
  [(command-table :node opts)])

(defmethod js->command* :karma
  [js-env compiler-opts opts]
  [(command-table :karma opts)
   "start"
   (karma/runner! [js-env] compiler-opts opts)])

(defn js->command [js-env compiler-opts opts]
  {:post [(every? string? %)]}
  (shell/flatten-cmd (js->command* js-env compiler-opts opts)))

;; ======================================================================
;; Karma Server

;; Only here to keep command-table in doo.core

(defn install!
  "Installs a karma server"
  [js-envs compiler-opts opts]
  (let [opts' (assoc-in opts [:karma :install?] true)
        cmd (shell/flatten-cmd [(command-table :karma opts')
                                "start"
                                (karma/runner! js-envs compiler-opts opts')])
        process (shell/exec! cmd (:exec-dir opts))]
    (utils/debug-log "Started karma server")
    (shell/set-cleanup! process opts "Shutdown Karma Server")
    (shell/capture-process! process (assoc opts :verbose true))
    process))

(defn karma-run!
  "Runs karma once, assuming there is a karma server already running.
  Takes doo-options as passed to run-script."
  [opts]
  (let [cmd (shell/flatten-cmd [(command-table :karma opts)
                                "run" "--" "doo.runner.run_BANG_"])
        process (shell/exec! cmd (:exec-dir opts))]
    (utils/debug-log "Started karma run")
    (shell/set-cleanup! process opts "Close Karma run")
    process))

;; ======================================================================
;; Compiler options

(defn assert-compiler-opts
  "Raises an exception if the compiler options are not valid.
   See valid-compiler-opts?"
  [js-env compiler-opts]
  {:pre [(keyword? js-env) (map? compiler-opts)]}
  (let [optimization (:optimizations compiler-opts)]
    (assert (some? (:output-to compiler-opts)) ":output-to can't be nil")
    (when (= :node js-env)
      (assert (= :nodejs (:target compiler-opts))
        "node should be used with :target :nodejs"))
    (when (karma/env? js-env)
      (assert (some? (:output-dir compiler-opts))
        "Karma runners need :output-dir specified"))
    (when (= :rhino js-env)
      (assert (not= :none optimization)
        "rhino doesn't support :optimizations :none"))
    (when (= :nashorn js-env)
      (assert (not= :none optimization)
        "Nashorn doesn't support :optimizations :none"))
    true))

;; ======================================================================
;; Bash

(def cmd-not-found
  "We tried running %s but we couldn't find it your system. Try:
\n\t %s \n
If it doesn't work you need to install %s, see https://github.com/bensu/doo#setting-up-environments\n
If it does work, file an issue and we'll sort it together!")

(def default-opts {:verbose true
                   :debug false
                   :karma {:install? false}})

(def untrustworthy-exit-env
  "The set of JS envs having executables which may not be able to signify
  failure with a non-zero exit code."
  #{:phantom :slimer})

(defn run-script
  "Runs the script defined in :output-to of compiler-opts
   in the selected js-env.

  (run-script js-env compiler-opts)
  (run-script js-env compiler-opts opts)

where:

  js-env - any of :phantom, :slimer, :node, :rhino, :nashorn,
                  :chrome, :firefox, :ie, :safari, or :opera
  compiler-opts - the options passed to the ClojureScript when it
                  compiled the script that doo should run
  opts - a map that can contain:
    :verbose - bool (default true) that determines if the scripts
               output should be printed and returned (verbose true)
               or only returned (verbose false).
    :debug - bool (default false) to log to standard-out internal events
             to aid debugging
    :paths - a map from runners (keywords) to string commands for bash.
    :exec-dir - a directory path (file) from where runner should be
                executed. Defaults to nil which resolves to the current dir"
  ([js-env compiler-opts]
   (run-script js-env compiler-opts {}))
  ([js-env compiler-opts opts]
   {:pre [(valid-js-env? js-env)]}
   (let [doo-opts (merge default-opts opts)
         cmd (conj (js->command js-env compiler-opts doo-opts)
                   (:output-to compiler-opts))]
     (when (:debug doo-opts)
       (utils/debug-log "Command to run script:" cmd))
     (try
       (let [{:keys [err exit] :as r} (shell/sh cmd doo-opts)]
         ;; Phantom/Slimer don't return correct exit code when
         ;; provided bad opts
         ;; Try `phantomjs --bad-opts=asdfasdf main.js` followed by
         ;; `echo $?` for phantomjs 1.9.0 / slimerjs 0.9.6
         (cond-> r
           (and (untrustworthy-exit-env js-env)
                (not-empty err)
                (zero? exit)) (assoc :exit 1)))
       (catch java.io.IOException e
         (utils/debug-log "Failed to run command: " (pr-str e))
         (let [js-path (first cmd)
               error-msg (format cmd-not-found js-path
                           (cond
                            (= js-env :rhino) "rhino -help"
                            (= js-env :nashorn) "jjs -h"
                            :else (str js-path " -v"))
                           js-path)]
           (when (:verbose doo-opts)
             (println error-msg))
           {:exit 127
            :err error-msg
            :out ""}))))))
