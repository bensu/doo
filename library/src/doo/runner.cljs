(ns doo.runner
  (:refer-clojure :exclude (run! set-print-fn!))
  (:require [cljs.test :refer [successful?]]))

;; Printing
;; ========

(enable-console-print!)

(defn ^:export set-print-fn! [f]
  (set! cljs.core.*print-fn* f))

;; Start Testing
;; =============

(def ^:export run! nil)

(defn set-entry-point!
  "Sets the function to be run when starting the script"
  [f]
  {:pre [(ifn? f)]}
  (set! run! f))

;; Finish Testing 
;; ==============

(def ^:dynamic *exit-fn* nil)

(defn ^:export set-exit-point!
  "Sets the fn to be called when exiting the script.
   It should take one bool argument: successful?"
  [f]
  {:pre [(ifn? f)]}
  (set! *exit-fn* f))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (*exit-fn* (successful? m)))
