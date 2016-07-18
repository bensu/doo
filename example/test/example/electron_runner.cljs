(ns example.electron-runner
  (:require [cljs.test :as test]
            [doo.runner :refer-macros [doo-all-tests doo-tests]]
            [example.electron-test]))

(doo-tests 'example.electron-test)
