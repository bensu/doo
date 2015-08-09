(ns doo.runner
  (:require [cljs.test]
            [cljs.analyzer.api]))

(defmacro count-tests [namespaces]
  `(+ ~@(map (fn [ns]
               (count (filter #(:test (nth % 1) false)
                        (cljs.analyzer.api/ns-publics (second ns)))))
          namespaces)))

(defmacro doo-all-tests []
  `(doo.runner/set-entry-point!
     (fn [] (cljs.test/run-all-tests))))

(defmacro doo-tests [& namespaces]
  `(doo.runner/set-entry-point!
     (if (doo.runner/karma?)
       (fn [tc#]
         (jx.reporter.karma/start tc# (doo.runner/count-tests ~namespaces))
         (cljs.test/run-tests
           (cljs.test/empty-env :jx.reporter.karma/karma)
           ~@namespaces))
       (fn []
         (cljs.test/run-tests ~@namespaces)))))
