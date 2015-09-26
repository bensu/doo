(ns lein-doo.config
  (:require [clojure.test :refer :all]
            [leiningen.doo :as doo]))

(deftest correct-main
  (testing "doo can handle three types for main"
    (doseq [main '('lein.core "lein.core" lein.core)]
      (is (doo/correct-builds {:cljsbuild {:builds {:dev {:compiler {:main main}}}}})))))
