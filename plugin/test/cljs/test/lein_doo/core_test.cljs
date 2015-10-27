(ns lein-doo.core-test
  (:require [cljs.test :refer-macros [async deftest is testing]]))

(deftest sync-test
  (is (= 1 1)))

;; Only works in browsers because it uses setTimeout
(deftest async-test
  (async done
    (is (= 1 1))
    (let [a 1]
      (js/setTimeout (fn []
                       (is (= a 1))
                       (done))
        500))))
