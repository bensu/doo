(ns doo.core
  "Runs a Js script in any Js environment. See doo.core/run-script"
  (:import java.io.File)
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [doo.karma :as karma]
            [doo.shell :as shell]))

;; ====================================================================== 
;; JS Environments

;; All js-envs are keywords.

(def doo-envs #{:phantom :slimer :node :rhino})

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

(defn assert-alias [js-env-alias resolved-js-envs]
  (assert (not (empty? resolved-js-envs))
    (str "The given alias: " js-env-alias
      " didn't resolve to any runners. Try any of: "
       (str/join ", " (map name js-envs)) " or "
       (str/join ", " (map name (keys default-aliases))))))

(defn assert-js-env
  "Throws an exception if the js-env is not valid.
   See valid-js-env?"
  [js-env]
  (assert (valid-js-env? js-env)
    (str "The js-env should be one of: "
         (str/join ", " (map name js-envs))
         " and we got: " js-env)))

(defn print-env [js-env]
  (println "")
  (println ";;" (str/join "" (take 70 (repeat "="))))
  (println (str ";; Testing with " (str/capitalize (name js-env)) ":")) 
  (println ""))

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
   :node "node"
   :karma "./node_modules/karma/bin/karma"})

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
                                (karma/runner! js-envs compiler-opts opts')])]
    (doto (shell/exec! cmd)
      (shell/capture-process! opts)
      (shell/set-cleanup! opts "Shutdown Karma Server"))))

(defn karma-run! [opts]
  (let [cmd (shell/flatten-cmd [(command-table :karma opts)
                                "run" "--" "doo.runner.run_BANG_"])]
    (doto (shell/exec! cmd)
      (shell/set-cleanup! opts "Close Karma run"))))

;; ====================================================================== 
;; Compiler options

(defn assert-compiler-opts
  "Raises an exception if the compiler options are not valid.
   See valid-compiler-opts?"
  [js-env compiler-opts]
  {:pre [(keyword? js-env) (map? compiler-opts)]}
  (let [optimization (:optimizations compiler-opts)]
    (when (= :node js-env)
      (assert (= :nodejs (:target compiler-opts))
        "node should be used with :target :nodejs"))
    (when (karma/env? js-env)
      (assert (some? (:output-dir compiler-opts))
        "Karma runners need :output-dir specified"))
    (when (= :rhino js-env)
      (assert (not= :none optimization)
        "rhino doesn't support :optimizations :none"))
    true))

;; ====================================================================== 
;; Bash

(def cmd-not-found
  "We tried running %s but we couldn't find it your system. Try:
\n\t %s \n
If it doesn't work you need to install %s, see https://github.com/bensu/doo#setting-up-environments\n
If it does work, file an issue and we'll sort it together!")

(def default-opts {:verbose true
                   :karma {:install? false}})

(defn run-script
  "Runs the script defined in :output-to of compiler-opts
   in the selected js-env.

  (run-script js-env compiler-opts)
  (run-script js-env compiler-opts opts)

where: 

  js-env - any of :phantom, :slimer, :node, :rhino, :chrome, :firefox, 
           :ie, :safari, or :opera
  compiler-opts - the options passed to the ClojureScript when it
                  compiled the script that doo should run
  opts - a map that can contain:
    :verbose - bool (default true) that determines if the scripts
               output should be printed and returned (verbose false)
               or only returned (verbose true).
    :paths - a map from runners (keywords) to string commands for bash."
  ([js-env compiler-opts]
   (run-script js-env compiler-opts {}))
  ([js-env compiler-opts opts]
   {:pre [(valid-js-env? js-env)]}
   (let [doo-opts (merge default-opts opts)
         cmd (conj (js->command js-env compiler-opts doo-opts)
                   (:output-to compiler-opts))]
     (try
       (let [r (shell/sh cmd doo-opts)]
         ;; Phantom/Slimer don't return correct exit code when
         ;; provided bad opts
         ;; Try `phantomjs --bad-opts=asdfasdf main.js` followed by
         ;; `echo $?` for phantomjs 1.9.0 / slimerjs 0.9.6
         (cond-> r
           (and (not (empty? (:err r))) (zero? (:exit r))) (assoc :exit 1)))
       (catch java.io.IOException e
         (let [js-path (first cmd)
               error-msg (format cmd-not-found js-path 
                           (if (= js-env :rhino)
                             "rhino -help"
                             (str js-path " -v"))
                           js-path)]
           (when (:verbose doo-opts)
             (println error-msg))
           {:exit 127
            :err error-msg
            :out ""}))))))
