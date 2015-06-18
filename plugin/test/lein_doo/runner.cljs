(ns lein-doo.runner
  (:require [cljs.test :as test]
            [doo.runner :refer-macros [doo-tests]]
            [lein-doo.core-test]))

(doo-tests 'lein-doo.core-test)
