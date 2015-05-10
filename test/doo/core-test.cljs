(ns doo.core-test
  (:require [cljs.test :refer-macros [async deftest is testing]]))

(enable-console-print!)

(deftest basic
  (is (= 1 2)))
