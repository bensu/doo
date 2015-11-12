(ns test.doo.core
  (:require [clojure.test :refer [deftest are is testing]]
            [cljs.build.api :as cljs]
            [doo.core :as doo]))

(deftest compiler-opts
  (are [msg js-env opts] (is (thrown? java.lang.AssertionError
                               (doo/assert-compiler-opts js-env opts)) msg)
       ":rhino doesn't support :none"
       :rhino {:output-to "target/testable.js"
               :main 'example.runner
               :optimizations :none}
       ":nashorn doesn't support :none"
       :nashorn {:output-to "target/testable.js"
               :main 'example.runner
               :optimizations :none}
       "karma needs :output-dir"
       :chrome {:output-to "target/testable.js"
                :main 'example.runner
                :optimizations :none}
       ":node needs :target :nodejs"
       :node {:output-to "target/testable.js"
              :main 'example.runner
              :optimizations :none})
  (are [js-env opts] (doo/assert-compiler-opts js-env opts)
       :node {:output-to "target/testable.js"
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
         :rhino :nashorn :slimer :phantom :node :chrome :safari :firefox :opera :ie))
  (testing "We can resolve aliases"
    (are [alias js-envs] (= (doo/resolve-alias alias {}) js-envs)
         :phantom [:phantom]
         :slimer [:slimer]
         :node [:node]
         :rhino [:rhino]
         :nashorn [:nashorn]
         :headless [:slimer :phantom]
         :not-an-alias [])
    (let [alias-map {:browsers [:chrome :firefox]
                     :engines [:rhino :nashorn]
                     :all [:browsers :engines]}]
      (are [alias js-envs] (= (doo/resolve-alias alias alias-map) js-envs)
           :browsers [:chrome :firefox]
           :engines [:rhino :nashorn]
           :all [:chrome :firefox :rhino :nashorn]
           :phantom [:phantom]
           :slimer [:slimer]
           :node [:node]
           :rhino [:rhino]
           :nashorn [:nashorn]
           :headless [:slimer :phantom]
           :not-an-alias [])))
  (testing "we warn against circular dependencies"
    (is (thrown-with-msg? java.lang.Exception #"circular" 
          (doo/resolve-alias :all {:browsers [:chrome :engines]
                                   :engines [:rhino :nashorn :browsers]
                                   :all [:browsers :engines]})))))

(deftest resolve-path
  (testing "When given a js-env, it gets the correct path"
    (testing "with the defaults"
      (are [js-env path] (= path (first (doo/command-table js-env {})))
           :slimer "slimerjs"
           :phantom "phantomjs"
           :rhino "rhino"
           :nashorn "jrunscript"
           :node "node"
           :karma "./node_modules/karma/bin/karma")
      (testing "unless we don't have it"
        (is (thrown? java.lang.AssertionError
              (doo/command-table :unknown {})))))
    (testing "when passing options"
      (are [js-env path] (= path (first (doo/command-table js-env
                                          {:paths {:karma "karma"}})))
           :slimer "slimerjs"
           :karma "karma"))))

(defn doo-ok? [doo-output]
  (zero? (:exit doo-output)))

(deftest integration
  (testing "We can compile a cljs project"
    (let [doo-opts {:verbose false 
                    :paths {:karma "karma"}}
          compiler-opts {:output-to "out/testable.js"
                         :output-dir "out"
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
           {:optimizations :whitespace} [:rhino :nashorn :phantom :chrome :firefox]
           {:optimizations :simple :target :nodejs} [:node]
           {:optimizations :advanced :target :nodejs} [:node]
           {:optimizations :advanced} [:phantom :rhino :nashorn :chrome :firefox]))))

(deftest paths-with-options 
  (testing "We can pass paths with options"
    (let [phantom-cmd "phantomjs --ignore-ssl-errors=true --web-security=false"
          doo-opts {:verbose false 
                    :paths {:phantom phantom-cmd}}
          compiler-opts {:output-to "out/testable.js"
                         :main 'example.runner}
          srcs (cljs/inputs "../example/src" "../example/test")]
      (cljs/build srcs compiler-opts)
      (is (doo-ok? (doo/run-script :phantom compiler-opts doo-opts)))
      (testing "but there are problems with bad options"
        (is (not (doo-ok? (->> {:phantom "phantomjs --bad-opts=asdfa"}
                            (assoc doo-opts :paths)
                            (doo/run-script :phantom compiler-opts)))))))))
