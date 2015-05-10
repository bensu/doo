(ns doo.runner)

(defmacro doo-tests [& namespaces]
  `(doo.runner/set-run! 
     (fn [] (cljs.test/run-block
             (concat (cljs.test/run-tests-block ~@namespaces)
               ;; How do I pass an argument?
               [(fn []
                  (if (ifn? doo.runner/*on-testing-complete-fn*)
                    (doo.runner/*on-testing-complete-fn*)))])))))
