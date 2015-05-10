(ns doo.core
  (:refer-clojure :exclude (set-print-fn!)))

(defn test-completed []
  nil)

(enable-console-print!)

(defn ^:export set-print-fn! [f]
  (set! cljs.core.*print-fn* f))

(comment
  (defmacro run-tests [& namespaces]
    `(cljs.test/run-block
       (concat ~@(cljs.test/run-tests-block ~namespaces)
         ;; How do I pass an argument?
         [(fn []
            (doo.core/tests-completed))]))))
