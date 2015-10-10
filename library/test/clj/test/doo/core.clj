(ns test.doo.core
  (:require [clojure.test :refer [deftest are is testing]]
            [cljs.build.api :as cljs]
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
              :optimizations :none
              :target :nodejs}
       ;; :node needs :target :nodejs
       :node {:output-to "target/testable.js"
              :main 'example.runner
              :target :nodejs
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
              :optimizations :none
              :target :nodejs}
       :node {:output-to "target/testable.js"
              :main 'example.runner
              :target :nodejs
              :optimizations :simple}
       :phantom {:output-to "target/testable.js"
                 :main 'example.runner
                 :optimizations :none}
       :slimer {:output-to "target/testable.js"
                :main 'example.runner
                :optimizations :none}))

(deftest js-env
  (testing "We know which js-envs we support"
    (are [js-env] (not (doo/valid-js-env? js-env))
         :spidermonkey :browser :browsers :v8 :d8 :something-else)
    (are [js-env] (doo/valid-js-env? js-env)
         :rhino :slimer :phantom :node :chrome :safari :firefox :opera :ie))
  (testing "We can resolve aliases"
    (are [alias js-envs] (= (doo/resolve-alias alias {}) js-envs)
         :phantom [:phantom]
         :slimer [:slimer]
         :node [:node]
         :rhino [:rhino]
         :headless [:slimer :phantom]
         :not-an-alias [])
    (let [alias-map {:browsers [:chrome :firefox]
                     :engines [:rhino]
                     :all [:browsers :engines]}]
      (are [alias js-envs] (= (doo/resolve-alias alias alias-map) js-envs)
           :browsers [:chrome :firefox]
           :engines [:rhino]
           :all [:chrome :firefox :rhino]
           :phantom [:phantom]
           :slimer [:slimer]
           :node [:node]
           :rhino [:rhino]
           :headless [:slimer :phantom]
           :not-an-alias [])))
  (testing "we warn against circular dependencies"
    (is (thrown-with-msg? java.lang.Exception #"circular" 
          (doo/resolve-alias :all {:browsers [:chrome :engines]
                                   :engines [:rhino :browsers]
                                   :all [:browsers :engines]})))))

(deftest resolve-path
  (testing "When given a js-env, it gets the correct path"
    (testing "with the defaults"
      (are [js-env path] (= path (doo/command-table js-env {}))
           :slimer "slimerjs"
           :phantom "phantomjs"
           :rhino "rhino"
           :node "node"
           :karma "./node_modules/karma/bin/karma")
      (is (thrown? java.lang.AssertionError
            (doo/command-table :unknown {}))))
    (testing "when passing options"
      (are [js-env path] (= path (doo/command-table js-env
                                   {:paths {:karma "karma"}}))
           :slimer "slimerjs"
           :karma "karma"))))

(defn doo-ok? [doo-output]
  (zero? (:exit doo-output)))

(deftest integration
  (testing "We can compile a cljs project"
    (let [doo-opts {:silent? true 
                    :paths {:karma "karma"}}
          compiler-opts {:output-to "out/testable.js"
                         :main 'example.runner
                         :optimizations :none}
          srcs (cljs/inputs "../example/src" "../example/test")]
      (are [opts envs] (let [compiler-opts' (merge compiler-opts opts)]
                         (cljs/build srcs compiler-opts')
                         (->> envs
                           (mapv (fn [env]
                                   (-> env
                                     (doo/run-script compiler-opts' doo-opts)
                                     doo-ok?)))
                           (every? true?)))
           {} [:phantom :chrome :firefox]
           {:target :nodejs} [:node] 
           {:optimizations :whitespace} [:rhino :phantom :chrome :firefox]
           {:optimizations :simple :target :nodejs} [:node]
           {:optimizations :advanced :target :nodejs} [:node]
           {:optimizations :advanced} [:phantom :rhino :chrome :firefox]))))
