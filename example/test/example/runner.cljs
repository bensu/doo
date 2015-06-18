(ns example.runner 
  (:require [cljs.test :as test]
            [doo.runner :refer-macros [doo-tests]]
            [example.core-test]))

(doo-tests 'example.core-test)
