(ns example.core-test
  (:require [cljs.test :refer-macros [async deftest is testing]]
            [example.core :as core]))

(deftest adding
  (is (= 2 (core/adder 1 1))))
