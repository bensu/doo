(ns lein-doo.config
  (:require [clojure.test :refer :all]
            [leiningen.doo :as doo]))

(deftest correct-main
  (testing "doo can handle three types for main"
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
