(ns test.runner
  (:require [cljs.test :as tt]
            [doo.core-test]))

(defn runner [] 
  (tt/run-tests
    (tt/empty-env ::tt/default)
    'doo.core-test))
