(ns doo.core
  (:require [clojure.java.shell :refer [sh]]))

(def js-envs #{:phantom :slimer :node})

(defn valid-js-env? [js-env]
  (contains? js-envs (keyword js-env)))

(defn js->command [js]
  {:pre [(keyword? js)]
   :post [(not (nil? %))]}
  (get {:phantom "phantomjs"
        :slimer "slimerjs"
        :node "node"}
    js))

(def valid-optimizations #{:simple :whitespace :advanced})

(defn valid-complier-opts? [opts]
  {:pre [(map? opts)]}
  (contains? valid-optimizations (:optimizations opts)))

(defn assert-compiler-opts [opts]
  (assert (valid-complier-opts? opts)
    (str ":optmimizations should be one of: "
      (clojure.string/join ", " (map str valid-optimizations))
      ". It currently is " (:optimizations opts))))

(defn run-script [js-env compiler-opts]
  {:pre [(valid-js-env? js-env)]}
  (sh (js->command (keyword js-env)) (:output-to compiler-opts)))
