(ns example.failing-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [async deftest is testing]]
            [cljs.core.async :as async :refer [chan put! <!]]
            [example.core :as core]))

(deftest adding
         (is (= 3 (core/adder 1 1))))

(deftest async-test
         (async done
                (let [a 1]
                  (js/setTimeout (fn []
                                   (is (= 1 a))
                                   (done))
                                 100)
                  (is (= 2 a)))))

(deftest csp-test
         (async done
                (let [val 1
                      ch (chan)]
                  (go (let [a (<! ch)]
                        (is (= a :not-a))
                        (done)))
                  (put! ch val))))
