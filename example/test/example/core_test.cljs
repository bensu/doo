(ns example.core-test
  (:require [cljs.test :refer-macros [async deftest is testing]]
            [example.core :as core]))

(deftest adding
  (is (= 2 (core/adder 1 1))))

(deftest async-test
  (async done
    (let [a 1]
      (js/setTimeout (fn []
                       (is (= 1 a))
                       (done))
        100)
      (is (= 1 a)))))
