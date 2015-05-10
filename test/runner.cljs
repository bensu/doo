(ns test.runner
  (:refer-clojure :exclude (set-print-fn!)) 
  (:require [cljs.test :as tt]
            [doo.core-test]))

(enable-console-print!)

(defn runner [] 
  (tt/run-tests
    (tt/empty-env ::tt/default)
    'doo.core-test))

(defn ^:export set-print-fn! [f]
  (set! cljs.core.*print-fn* f))
