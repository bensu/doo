(ns doo.core
  (:import java.io.File)
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]))

;; JS Environments
;; ===============

;; Inside this ns all js-envs are keywords.
(def js-envs #{:phantom :slimer :node})

(defn valid-js-env? [js-env]
  {:pre [(keyword? js-env)]}
  (contains? js-envs js-env))

(defn assert-js-env [js-env]
  (assert (valid-js-env? js-env)
    (str "The js-env should be one of: "
      (clojure.string/join ", " (map name js-envs))
      " and we got: " js-env)))


;; Runners
;; =======

(def base-dir "runners/")

;; The runner keyword is not necessary
(defn runner-path! [runner filename]
  "Creates a temp file for the given runner resource file"
  (let [full-path (str base-dir filename)
        runner-path (.getAbsolutePath
                     (doto (File/createTempFile (name runner) ".js")
                       (.deleteOnExit)
                       (#(io/copy (slurp (io/resource full-path)) %))))]
    runner-path))

(defn get-resource [rs]
  (.getPath (io/resource (str base-dir rs))))

(defn js->command [js]
  {:pre [(keyword? js)]
   :post [(not (nil? %))]}
  (case js
    :phantom ["phantomjs" (runner-path! :phantom "unit-test.js") 
              (runner-path! :phantom-shim "phantomjs-shims.js")]
    :slimer ["slimerjs" (runner-path! :slimer "unit-test.js") ]
    :node "node"))

;; Compiler options
;; ===============

(def valid-optimizations #{:simple :whitespace :advanced})

(defn valid-complier-opts? [opts]
  {:pre [(map? opts)]}
  (contains? valid-optimizations (:optimizations opts)))

(defn assert-compiler-opts [opts]
  (assert (valid-complier-opts? opts)
    (str ":optmimizations should be one of: "
      (clojure.string/join ", " (map str valid-optimizations))
      ". It currently is " (:optimizations opts))))

;; bash
;; ====

(defn run-script [js-env compiler-opts]
  {:pre [(valid-js-env? js-env)]}
  (println (conj (js->command js-env)
                      (:output-to compiler-opts)))
  (let [r (apply sh (conj (js->command js-env)
                      (:output-to compiler-opts)))]
    (println (:out r))))
