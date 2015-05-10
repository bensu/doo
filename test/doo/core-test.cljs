(ns doo.core-test
  (:require [cljs.test :refer-macros [async deftest is testing]]))

(enable-console-print!)

(deftest js-envs 
  (is (= 2 1)))
