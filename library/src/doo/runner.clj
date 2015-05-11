(ns doo.runner)

(defmacro doo-tests [& namespaces]
  `(doo.runner/set-entry-point! 
     (fn [] (cljs.test/run-block
             (concat (cljs.test/run-tests-block ~@namespaces)
               ;; How do I pass an argument?
               [(fn []
                  (if (ifn? doo.runner/*exit-fn*)
                    (doo.runner/*exit-fn*)))])))))
