(ns doo.core
  "Runs a Js script in any Js environment. See doo.core/run-script"
  (:import java.io.File)
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [selmer.parser :as selmer]))

;; ====================================================================== 
;; JS Environments

;; All js-envs are keywords.

(def karma-envs #{:chrome :firefox :safari :opera :ie})

(def doo-envs #{:phantom :slimer :node :rhino})

(def js-envs (set/union doo-envs karma-envs))

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

(def base-dir "runners/")

(defn runner-path!
  "Creates a temp file for the given runner resource file"
  [runner filename]
  (let [full-path (str base-dir filename)]
    (.getAbsolutePath
      (doto (File/createTempFile (name runner) ".js")
        (.deleteOnExit)
        (#(io/copy (slurp (io/resource full-path)) %))))))

(defn karma-runner! 
  "Creates a file for the given runner resource file in the users dir"
  [js-env compiler-opts]
  {:pre [(some? (:output-dir compiler-opts))]}
  (let [resource-path (str base-dir "karma.conf.js.tmpl")
        tmpl-opts (assoc compiler-opts
                    js-env true
                    :none (= :none (:optimizations compiler-opts)))]
    ;; TODO: get karma to work with an absolute path instead
    (.getPath
      (doto (io/file "doo_karma_runner.js")
        (.deleteOnExit)
        (#(io/copy (selmer/render-file resource-path tmpl-opts) %))))))

(def default-command-table
  {:phantom "phantomjs"
   :slimer "slimerjs" 
   :rhino "rhino"
   :node "node"
   :karma "./node_modules/karma/bin/karma"})

(defn command-table [js-env opts]
  {:post [(some? %)]}
  (get (merge default-command-table (:paths opts)) js-env))

(defmulti js->command
  (fn [js _ _]
    (if (contains? karma-envs js)
      :karma
      js)))

(defmethod js->command :phantom
  [_ _ opts]
  [(command-table :phantom opts)
   (runner-path! :phantom "unit-test.js") 
   (runner-path! :phantom-shim "phantomjs-shims.js")])

(defmethod js->command :slimer
  [_ _ opts]
  [(command-table :slimer opts)
   (runner-path! :slimer "unit-test.js")])

(defmethod js->command :rhino
  [_ _ opts]
  [(command-table :rhino opts) "-opt" "-1" (runner-path! :rhino "rhino.js")])

(defmethod js->command :node
  [_ _ opts]
  [(command-table :node opts)])

(defmethod js->command :karma
  [js-env compiler-opts opts]
  [(command-table :karma opts) "start" (karma-runner! js-env compiler-opts)])

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
    (when (contains? karma-envs js-env)
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

(def default-opts {:verbose true})

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
    :paths - a map from runners (keywords) to string commands for bash.
    :alias - a map from aliases (keywords) to vectors of aliases and
             runners. See doo.core/resolve-alias"
  ([js-env compiler-opts]
   (run-script js-env compiler-opts {}))
  ([js-env compiler-opts opts]
   {:pre [(valid-js-env? js-env)]}
   (let [doo-opts (merge default-opts opts)
         cmd (conj (js->command js-env compiler-opts doo-opts)
                   (:output-to compiler-opts))]
     (try
       (let [r (apply sh cmd)]
         (when (:verbose doo-opts)
           (println (:out r)))
         r)
       (catch java.io.IOException e
         (let [js-path (first cmd)
               error-msg (format cmd-not-found js-path 
                           (if (= js-env :rhino)
                             "rhino -help"
                             (str js-path " -v"))
                           js-path)]
           (println error-msg)
           {:exit 127
            :out error-msg}))))))
