(ns example.runner
  (:require [cljs.test :as test]
            [doo.runner :refer-macros [doo-all-tests doo-tests]]
            [example.core-test]))

(doo-tests 'example.core-test)
