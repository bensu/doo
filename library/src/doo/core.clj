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

(def karma-envs #{:chrome :firefox})

(def doo-envs #{:phantom :slimer :node :rhino})

(def js-envs (set/union doo-envs karma-envs))

(def default-aliases {:headless [:slimer :phantom]})

(defn resolve-alias
  "Given an alias it resolves to a list of the js-envs it represents,
   or an empty list if it represents not js-envs. js-envs resolve to 
   themselves.

   Ex: (resolve-alias :headless) => [:phantom :slimer]
       (resolve-alias :slimer) => [:slimer]
       (resolve-alias :something) => []"
  [alias]
  (cond
    (contains? js-envs alias) [alias]
    (contains? default-aliases alias) (get default-aliases alias)
    :else []))

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
  (println ";;" (str/join "" (take 70 (repeat "="))))
  (println (str ";; Testing with " (str/capitalize (name js-env)) ":")))

;; ====================================================================== 
;; Runners

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
  (let [resource-path (str base-dir "karma.conf.js.tmpl")
        tmpl-opts (assoc compiler-opts
                    js-env true)]
    (.getPath
      (doto (io/file "karma.js")
        (.deleteOnExit)
        (#(io/copy (selmer/render-file resource-path tmpl-opts) %))))))

;; TODO: should be configurable
(def command-table
  {:phantom "phantomjs"
   :slimer "slimerjs" 
   :rhino "rhino"
   :node "node"
   :karma "./node_modules/karma/bin/karma"})

;; Define in terms of multimethods to allow user extensibility
(defmulti js->command (fn [js _] js))

(defmethod js->command :phantom
  [_ _]
  [(command-table :phantom)
   (runner-path! :phantom "unit-test.js") 
   (runner-path! :phantom-shim "phantomjs-shims.js")])

(defmethod js->command :slimer
  [_ _]
  [(command-table :slimer)
   (runner-path! :slimer "unit-test.js")])

(defmethod js->command :rhino
  [_ _]
  [(command-table :rhino) "-opt" "-1" (runner-path! :rhino "rhino.js")])

(defmethod js->command :node
  [_ _]
  [(command-table :node) (runner-path! :node "node-runner.js")])

(defmethod js->command :chrome
  [_ compiler-opts]
  [(command-table :karma)
   "start"
   (karma-runner! :chrome compiler-opts)])

(defmethod js->command :firefox
  [_ compiler-opts]
  [(command-table :karma)
   "start"
   (karma-runner! :firefox compiler-opts)])

;; ====================================================================== 
;; Compiler options

(defn assert-compiler-opts
  "Raises an exception if the compiler options are not valid.
   See valid-compiler-opts?"
  [js-env opts]
  {:pre [(keyword? js-env) (map? opts)]}
  (let [optimization (:optimizations opts)]
    (when (and (not= :node js-env) (= :none optimization))
      (assert (.isAbsolute (File. (:output-dir opts)))
        ":phantom and :slimer do not support relative :output-dir when used with :none. Specify an absolute path or leave it blank."))
    (when (= :node js-env)
      (assert (and (= :nodejs (:target opts)) (false? (:hashbang opts)))
        "node should be used with :target :nodejs and :hashbang false")
      ;; TODO: this is probably a cljs bug
      (when (= :none optimization)
        (assert (not (.isAbsolute (File. (:output-dir opts))))
          "to use :none with node you need to provide a relative :output-dir")))
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

(defn run-script
  "Runs the script defined in :output-to of compiler-opts
   and runs it in the selected js-env."
  [js-env compiler-opts]
  {:pre [(valid-js-env? js-env)]}
  (try
    (let [cmd (conj (js->command js-env compiler-opts)
                        (:output-to compiler-opts))
          r (apply sh cmd)]
      (println (:out r))
      r)
    (catch java.io.IOException e
      (let [cmd (command-table js-env)
            error-msg (format cmd-not-found cmd
                        (if (= js-env :rhino) "rhino -help" (str cmd " -v"))
                        cmd)]
        (println error-msg)
        {:exit 127
         :out error-msg}))))
