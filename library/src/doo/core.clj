(ns doo.core
  "Runs a Js script in any Js environment. See doo.core/run-script"
  (:import java.io.File)
  (:require [clojure.java.shell :refer [sh]]
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
      (clojure.string/join ", " (map name js-envs))
      " and we got: " js-env)))

;; ====================================================================== 
;; Runners

(def base-dir "runners/")

;; The runner keyword is not necessary
(defn runner-path! [runner filename]
  "Creates a temp file for the given runner resource file"
  (let [full-path (str base-dir filename)
        runner-path (.getAbsolutePath
                     (doto (File/createTempFile (name runner) ".js")
                       ;; (.deleteOnExit)
                       (#(io/copy (slurp (io/resource full-path)) %))))]
    runner-path))

(defn get-resource [rs]
  (.getPath (io/resource (str base-dir rs))))

;; Define in terms of multimethods to allow user extensibility
(defn js->command [js]
  {:pre [(keyword? js)]
   :post [(not (nil? %))]}
  (case js
    :phantom ["phantomjs" (runner-path! :phantom "unit-test.js") 
              (runner-path! :phantom-shim "phantomjs-shims.js")]
    :slimer ["slimerjs" (runner-path! :slimer "unit-test.js") ]
    :rhino ["rhino" "-opt" "-1" (runner-path! :rhino "rhino.js")]
    :node ["node" (runner-path! :node "node-runner.js")]))

;; ====================================================================== 
;; Compiler options

(def valid-optimizations #{:simple :whitespace :advanced})

(defn valid-complier-opts? [opts]
  {:pre [(map? opts)]}
  (contains? valid-optimizations (:optimizations opts)))

(defn assert-compiler-opts
  "Raises an exception if the compiler options are not valid.
   See valid-compiler-opts?"
  [opts]
  (assert (valid-complier-opts? opts)
    (str ":optmimizations should be one of: "
      (clojure.string/join ", " (map str valid-optimizations))
      ". It currently is " (:optimizations opts))))

;; ====================================================================== 
;; Bash

(defn run-script
  "Runs the script defined in :output-to of compiler-opts
   and runs it in the selected js-env."
  [js-env script-path]
  {:pre [(valid-js-env? js-env)]}
  (let [r (apply sh (conj (js->command js-env) script-path))]
    (println (:out r))
    r))
