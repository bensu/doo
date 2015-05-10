(ns lein-doo.runner
  (:require [cljs.test :as tt]
            [doo.runner :as doo :refer-macros [run-tests]]
            [lein-doo.core-test]))

(defn run []
  (run-tests 'lein-doo.core-test))
