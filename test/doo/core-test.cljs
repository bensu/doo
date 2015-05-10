(ns doo.core-test
  (:require [cljs.test :refer-macros [async deftest is testing]]))

(enable-console-print!)

(deftest sync-test 
  (is (= 2 1)))

;; Only works in browsers because it uses setTimeout
(deftest async-test
  (async done
    (is (= 1 1))
    (let [a 1]
      (js/setTimeout (fn []
                       (is (= a 2))
                       (done))
        500))))
