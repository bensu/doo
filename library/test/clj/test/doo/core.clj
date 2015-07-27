(ns test.doo.core
  (:require [clojure.test :refer [deftest are is]]
            [doo.core :as doo]))

(defn prepare [opts]
  (cond-> opts
    (nil? (:output-dir opts)) (assoc :output-dir "/tmp/target")))

(deftest compiler-opts
  (are [js-env opts] (is (thrown? java.lang.AssertionError
                           (doo/assert-compiler-opts js-env (prepare opts))))
       ;; :rhino doesn't support :none
       :rhino {:output-to "target/testable.js"
               :main 'example.runner
               :optimizations :none}
       ;; :none needs :output-dir
       :node {:output-to "target/testable.js"
              :main 'example.runner
              :hashbang false
              :optimizations :none
              :target :nodejs}
       ;; :node nees :hasbang false
       :node {:output-to "target/testable.js"
              :main 'example.runner
              :optimizations :none
              :target :nodejs}
       ;; :node needs :target :nodejs
       :node {:output-to "target/testable.js"
              :main 'example.runner
              :hashbang false
              :optimizations :none}
       ;; :ouputdir not for the rest for none
       :phantom {:output-to "target/testable.js"
                 :output-dir "target/none"
                 :main 'example.runner
                 :optimizations :none}
       :slimer {:output-to "target/testable.js"
                :output-dir "target/none"
                :main 'example.runner
                :optimizations :none})
  (are [js-env opts] (is (doo/assert-compiler-opts js-env (prepare opts)))
       :node {:output-to "target/testable.js"
              :output-dir "target/none"
              :main 'example.runner
              :hashbang false
              :optimizations :none
              :target :nodejs}
       :node {:output-to "target/testable.js"
              :main 'example.runner
              :hashbang false
              :target :nodejs
              :optimizations :simple}
       :phantom {:output-to "target/testable.js"
                 :main 'example.runner
                 :optimizations :none}
       :slimer {:output-to "target/testable.js"
                :main 'example.runner
                :optimizations :none}))
