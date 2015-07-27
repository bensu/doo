(ns doo.core
  "Runs a Js script in any Js environment. See doo.core/run-script"
  (:import java.io.File)
  (:require [clojure.string :as str]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]))

;; ====================================================================== 
;; JS Environments

;; Inside this ns all js-envs are keywords.
(def js-envs #{:phantom :slimer :node :rhino})

(defn valid-js-env? [js-env]
  {:pre [(keyword? js-env)]}
  (contains? js-envs js-env))

(defn assert-js-env
  "Throws an exception if the js-env is not valid.
   See valid-js-env?"
  [js-env]
  (assert (valid-js-env? js-env)
    (str "The js-env should be one of: "
         (str/join ", " (map name js-envs))
         " and we got: " js-env)))

;; ====================================================================== 
;; Runners

(def base-dir "runners/")

(defn runner-path!
  "Creates a temp file for the given runner resource file"
  [runner filename]
  (let [full-path (str base-dir filename)
        runner-path (.getAbsolutePath
                      (doto (File/createTempFile (name runner) ".js")
                        (.deleteOnExit)
                        (#(io/copy (slurp (io/resource full-path)) %))))]
    runner-path))

(defn get-resource [rs]
  (.getPath (io/resource (str base-dir rs))))

(def command-table
  {:phantom "phantomjs" 
   :slimer "slimerjs" 
   :rhino "rhino"
   :node "node"})

;; Define in terms of multimethods to allow user extensibility
(defn js->command [js]
  {:pre [(keyword? js)]
   :post [(not (nil? %))]}
  (let [cmd (command-table js)]
    (case js
      :phantom [cmd (runner-path! :phantom "unit-test.js") 
                (runner-path! :phantom-shim "phantomjs-shims.js")]
      :slimer [cmd (runner-path! :slimer "unit-test.js") ]
      :rhino [cmd "-opt" "-1" (runner-path! :rhino "rhino.js")]
      :node [cmd (runner-path! :node "node-runner.js")])))

;; ====================================================================== 
;; Compiler options

(def valid-optimizations #{:none :simple :whitespace :advanced})

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
  [js-env script-path]
  {:pre [(valid-js-env? js-env)]}
  (try
    (let [r (apply sh (conj (js->command js-env) script-path))]
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
