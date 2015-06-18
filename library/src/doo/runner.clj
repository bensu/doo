(ns doo.runner
  (:require [cljs.test]))

(defmacro doo-tests [& namespaces]
  `(doo.runner/set-entry-point! 
     (fn [] (cljs.test/run-tests ~@namespaces))))
