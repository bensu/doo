(ns example.runner 
  (:require [cljs.test :as test]
            [doo.runner :refer-macros [doo-all-tests doo-tests]]))

(doo-all-tests)
