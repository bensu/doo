(ns doo.runner
  (:refer-clojure :exclude (run! set-print-fn!)))

;; Printing
;; ========

(enable-console-print!)

(defn ^:export set-print-fn! [f]
  (set! cljs.core.*print-fn* f))

;; Start Testing
;; =============

(def ^:export run! nil)

(defn set-run! [f]
  {:pre [(ifn? f)]}
  (set! run! f))

;; Finish Testing 
;; ==============

(def ^:dynamic *on-testing-complete-fn* nil)

(defn ^:export set-on-testing-complete! [f]
  {:pre [(ifn? f)]}
  (set! *on-testing-complete-fn* f))


