(set-env!
 :source-paths    #{"src"}
 :dependencies '[[org.clojure/core.async      "0.4.474"]
                 [org.clojure/clojurescript   "1.9.946"]
                 [crisptrutski/boot-cljs-test "0.3.5-SNAPSHOT"]
                 [doo                         "0.1.11"]])

(require
 '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(deftask add-tests []
  (set-env! :source-paths #(conj % "test"))
  identity)

(deftask add-failures []
  (set-env! :source-paths #(conj % "failing-tests"))
  identity)

(deftask deps [])
