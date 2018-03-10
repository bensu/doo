(ns test.doo.core
  (:require [clojure.test :refer [deftest are is testing]]
            [cljs.build.api :as cljs]
            [doo.core :as doo]))

(deftest compiler-opts
  (are [msg js-env opts] (is (thrown? java.lang.AssertionError
                               (doo/assert-compiler-opts js-env opts)) msg)
       ":output-to can't be nil"
       :node {:main 'example.runner
              :optimizations :none}
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
    (are [js-env] (not (doo/valid-js-env? js-env {}))
         :spidermonkey :browser :browsers :v8 :d8 :something-else)
    (are [js-env] (doo/valid-js-env? js-env {:custom-chrome {:name "..."}})
      :rhino :nashorn :slimer :phantom :node
      :chrome :safari :firefox :opera :ie :electron
      :karma-phantom :karma-slimer :chrome-headless :firefox-headless
      :custom-chrome))
  (testing "We can resolve aliases"
    (are [alias js-envs] (= (doo/resolve-alias alias {} {}) js-envs)
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
      (are [alias js-envs] (= (doo/resolve-alias alias alias-map {}) js-envs)
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
  (testing "we resolve custom karma launchers"
    (let [alias-map {:all-chrome [:chrome :chrome-no-security]}
          karma-launchers {:chrome-no-security {:plugin "karma-chrome-launcher" :name "Chrome_no_security"}}]
      (are [alias js-envs] (= (doo/resolve-alias alias alias-map karma-launchers) js-envs)
        :all-chrome [:chrome :chrome-no-security]
        :chrome-no-security [:chrome-no-security])))
  (testing "we warn against circular dependencies"
    (is (thrown-with-msg? java.lang.Exception #"circular"
          (doo/resolve-alias :all {:browsers [:chrome :engines]
                                   :engines [:rhino :nashorn :browsers]
                                   :all [:browsers :engines]} {})))))

(deftest resolve-path
  (testing "When given a js-env, it gets the correct path"
    (testing "with the defaults"
      (are [js-env path] (= path (first (doo/command-table js-env {})))
           :slimer "slimerjs"
           :phantom "phantomjs"
           :rhino "rhino"
           :nashorn "jjs"
           :node "node"
           :karma "karma")
      (testing "unless we don't have it"
        (is (thrown? java.lang.AssertionError
              (doo/command-table :unknown {})))))
    (testing "when passing options"
      (let [opts {:paths {:karma "./node_modules/karma/bin/karma"}}]
        (are [js-env path] (= path (first (doo/command-table js-env opts)))
             :slimer "slimerjs"
             :karma "./node_modules/karma/bin/karma")))))

(defn doo-ok? [doo-output]
  (zero? (:exit doo-output)))

(deftest integration
  (testing "We can compile a cljs project and run the script"
    (let [doo-opts {:verbose false
                    :paths {:slimer "../example/node_modules/.bin/slimerjs"}}
          compiler-opts {:output-to "out/testable.js"
                         :output-dir "out"
                         :main 'example.runner
                         :optimizations :none}
          srcs (cljs/inputs "../example/src" "../example/test")]
      (doseq [[opts envs] [[{} [:phantom :slimer :chrome :firefox :electron
                                :karma-phantom :karma-slimer]]
                           [{:target :nodejs} [:node]]
                           [{:optimizations :whitespace} [:rhino :nashorn :phantom :slimer
                                                          :chrome :firefox :electron
                                                          :karma-phantom :karma-slimer]]
                           [{:optimizations :simple :target :nodejs} [:node]]
                           [{:optimizations :advanced :target :nodejs} [:node]]
                           [{:optimizations :advanced} [:phantom :slimer :rhino :nashorn
                                                        :chrome :firefox :electron
                                                        :karma-phantom :karma-slimer]]]]
        (let [compiler-opts' (merge compiler-opts opts)]
          (cljs/build srcs compiler-opts')
          (doseq [env envs]
            (is (doo-ok? (doo/run-script env compiler-opts' doo-opts)))))))))

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
