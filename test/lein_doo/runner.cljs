(ns lein-doo.runner
  (:require [cljs.test :as tt]
            [doo.runner :as doo]
            [lein-doo.core-test]))

(defn run [] 
  (tt/run-tests
    (tt/empty-env ::tt/default)
    'lein-doo.core-test))

;; (defn newrunner []
;;   (doo/run-tests 'doo.core-test))
