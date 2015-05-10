(ns lein-doo.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [lein-doo.core-test]))

(doo-tests 'lein-doo.core-test)
