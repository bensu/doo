(ns doo.runner
  (:require [cljs.test]
            [cljs.analyzer.api :as ana-api]))

(defn count-tests [namespaces]
  (->> namespaces
    (map (fn [ns]
           (count (filter (fn [[_ v]] (:test v)) (ana-api/ns-interns ns)))))
    (reduce +)))

(defmacro doo-all-tests
  "Runs all tests in all namespaces; prints results.
  Optional argument is a regular expression; only namespaces with
  names matching the regular expression (with re-matches) will be
  tested."
  ([] `(doo-all-tests nil))
  ([re]
   `(doo.runner/set-entry-point!
     (if (doo.runner/karma?)
       (fn [tc#]
         (jx.reporter.karma/start tc# ~(count-tests (ana-api/all-ns)))
         (cljs.test/run-all-tests ~re
                                  (cljs.test/empty-env :jx.reporter.karma/karma)))
       (fn []
         (cljs.test/run-all-tests ~re))))))

(defmacro doo-tests [& namespaces]
  "Runs all tests in the given namespaces; prints results.
  Defaults to current namespace if none given. Does not return a meaningful
  value due to the possiblity of asynchronous execution. To detect test
  completion add a :end-run-tests method case to the cljs.test/report
  multimethod."
  `(doo.runner/set-entry-point!
     (if (doo.runner/karma?)
       (fn [tc#]
         (jx.reporter.karma/start tc# ~(count-tests (map second namespaces)))
         (cljs.test/run-tests
           (cljs.test/empty-env :jx.reporter.karma/karma)
           ~@namespaces))
       (fn []
         (cljs.test/run-tests ~@namespaces)))))
