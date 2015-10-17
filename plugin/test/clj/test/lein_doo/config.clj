(ns test.lein-doo.config
  (:require [clojure.test :refer :all]
            [leiningen.doo :as doo]))

(deftest correct-main
  (testing "lein-doo can handle three types for main"
    (letfn [(find-dev [project]
              (doo/find-by-id (get-in project [:cljsbuild :builds]) "dev"))]
      (doseq [main '('lein.core "lein.core" lein.core)]
        (is (= "dev" (-> {:cljsbuild {:builds [{:id "dev"
                                                :compiler {:main main}}]}}
                       doo/correct-builds
                       find-dev
                       :id)))
        (is (= "dev" (-> {:cljsbuild {:builds {:dev {:compiler {:main main}}}}}
                       doo/correct-builds
                       find-dev
                       :id)))))))

(deftest args->cli
  (testing "lein-doo properly doo/args->clis the cli arguments"
    (are [args opts] (= opts (doo/args->cli args))
         [] {:build :default
             :alias :default
             :watch-mode :auto}
         ["chrome"] {:build :default 
                     :alias :chrome
                     :watch-mode :auto}
         ["chrome" "none-test"] {:build "none-test" 
                                 :alias :chrome
                                 :watch-mode :auto}
         ["chrome" "once"] {:build :default 
                            :alias :chrome
                            :watch-mode :once}
         ["chrome" "none-test" "once"] {:build "none-test" 
                                        :alias :chrome
                                        :watch-mode :once})
    (are [args] (is (thrown? java.lang.AssertionError (doo/args->cli args)))
         ["chrome" "none-test" "autoo"]
         ["chrome" "none-test" "auto" "advanced-test"])))

(deftest cli->js-envs 
  (testing "We can get the js-envs from the cli"
    (let [opts {:build "test"
                :alias {:default [:firefox]}}]
      (are [args js-envs] (= js-envs (doo/cli->js-envs (doo/args->cli args) opts))
           [] [:firefox] 
           ["chrome"] [:chrome] 
           ["chrome" "none-test"] [:chrome] 
           ["chrome" "once"] [:chrome] 
           ["chrome" "none-test" "once"] [:chrome]))))

(deftest cli->build 
  (testing "We can get the right build from the cli, opts, and project"
    (let [builds [{:id "test"
                   :source-paths ["src" "test"]
                   :compiler {:optimizations :simple
                              :output-to "out/testable.js"}}
                  {:id "production"
                   :source-paths ["src" "test"]
                   :compiler {:optimizations :advanced
                              :output-to "out/testable.js"}}]
          project {:source-paths ["src" "src/main/clj"]
                   :test-paths ["test" "src/test/clj"]
                   :cljsbuild {:builds builds}}
          string-opts {:build "test" 
                       :alias {:default [:firefox]}}
          empty-opts {}]
      (are [opts args opt-level] (= opt-level (-> (doo/args->cli args)
                                                (doo/cli->build project opts)
                                                :compiler
                                                :optimizations))
           string-opts [] :simple 
           string-opts ["chrome"] :simple 
           string-opts ["chrome" "production"] :advanced 
           string-opts ["chrome" "once"]  :simple 
           string-opts ["chrome" "test" "once"] :simple
           empty-opts ["chrome" "production"] :advanced 
           empty-opts ["chrome" "test" "once"] :simple)
      (are [args] (is (thrown? java.lang.AssertionError
                        (doo/cli->build (doo/args->cli args) project empty-opts)))
           []
           ["chrome"]
           ["chrome" "once"]))))
