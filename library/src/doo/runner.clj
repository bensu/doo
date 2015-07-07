(ns doo.runner
  (:require [cljs.test]))

(defmacro doo-tests [& namespaces]
  `(doo.runner/set-entry-point!
     (fn [] (cljs.test/run-tests ~@namespaces))))

(defmacro doo-all-tests []
  `(doo.runner/set-entry-point!
     (fn [] (cljs.test/run-all-tests))))
