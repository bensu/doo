(ns example.failing-runner
  (:require [cljs.test :as test]
            [doo.runner :refer-macros [doo-all-tests doo-tests]]
            [example.failing-test]))

(doo-tests 'example.failing-test)
