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
 
(defn mode? [arg]
  (contains? #{"auto" "once"} arg))

(defn parse [args]
  (let [[js-env build-id & xs] (remove mode? args)]
    (assert (empty? xs)
      (str "We couldn't parse " xs " as a mode, only auto or once are supported"))
    {:alias (keyword (or js-env "default"))
     :build (or build-id :default)
     :mode (keyword (or (first (filter mode? args)) "auto"))}))

(defn args->js-envs [{cli-alias :alias} {alias-map :alias}]
  (doo.core/resolve-alias cli-alias alias-map))

(deftest parse-cli
  (testing "lein-doo properly parses the cli arguments"
    (are [args opts] (= opts (parse args))
         [] {:build :default
             :alias :default
             :mode :auto}
         ["chrome"] {:build :default 
                     :alias :chrome
                     :mode :auto}
         ["chrome" "none-test"] {:build "none-test" 
                                 :alias :chrome
                                 :mode :auto}
         ["chrome" "once"] {:build :default 
                            :alias :chrome
                            :mode :once}
         ["chrome" "none-test" "once"] {:build "none-test" 
                                        :alias :chrome
                                        :mode :once})
    (are [args] (is (thrown? java.lang.AssertionError (parse args)))
         ["chrome" "none-test" "autoo"]
         ["chrome" "none-test" "auto" "advanced-test"])))

(deftest parse-js-envs 
  (testing "We can get the js-envs from the cli"
    (let [opts {:build "test"
                :alias {:default [:firefox]}}]
      (are [args js-envs] (= js-envs (args->js-envs (parse args) opts))
           [] [:firefox] 
           ["chrome"] [:chrome] 
           ["chrome" "none-test"] [:chrome] 
           ["chrome" "once"] [:chrome] 
           ["chrome" "none-test" "once"] [:chrome]))))

(def default-build
  {:compiler {:optimizations :none
              :output-to "out/doo-testable.js"}})

(defn args->build [{cli-build :build} builds {opts-build :build}]
  {:post [(contains? % :source-paths)
          (not (empty? (:source-paths %)))]}
  (let [build (if (or (nil? cli-build)
                      (= :default cli-build))
                opts-build
                cli-build)]
    (if (or (nil? build) (map? build))
      (merge default-build build)
      (doo/find-by-id builds build))))

(deftest parse-build
  (testing "We can get the right build from the cli, opts, and project"
    (let [builds [{:id "test"
                   :source-paths ["src" "test"]
                   :compiler {:optimizations :simple
                              :output-to "out/testable.js"}}
                  {:id "advanced"
                   :source-paths ["src" "test"]
                   :compiler {:optimizations :advanced
                              :output-to "out/testable.js"}}]
          string-opts {:build "test" 
                       :alias {:default [:firefox]}}
          empty-opts {}
          map-opts {:build {:source-paths ["src" "test"]
                            :compiler {:optimizations :whitespace
                                       :output-to "out/testable.js"}}}]
      (are [opts args opt-level] (= opt-level (-> (parse args)
                                                (args->build builds opts)
                                                :compiler
                                                :optimizations))
           string-opts [] :simple 
           string-opts ["chrome"] :simple 
           string-opts ["chrome" "advanced"] :advanced 
           string-opts ["chrome" "once"]  :simple 
           string-opts ["chrome" "test" "once"] :simple
           map-opts [] :whitespace
           map-opts ["chrome"] :whitespace
           map-opts ["chrome" "advanced"] :advanced 
           map-opts ["chrome" "once"]  :whitespace
           map-opts ["chrome" "test" "once"] :simple
           empty-opts ["chrome" "advanced"] :advanced 
           empty-opts ["chrome" "test" "once"] :simple)
      (are [args] (is (thrown? java.lang.AssertionError
                        (args->build (parse args) builds empty-opts)))
           []
           ["chrome"]
           ["chrome" "once"]))))
