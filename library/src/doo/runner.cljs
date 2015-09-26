(ns doo.runner
  (:refer-clojure :exclude (run! set-print-fn!))
  (:require [cljs.test :refer [successful?]]
            [goog.object :as gobj]
            [jx.reporter.karma :as karma :include-macros true]))
            
;; ====================================================================== 
;; Printing

(enable-console-print!)

(defn ^:export set-print-fn! [f]
  (set! cljs.core.*print-fn* f))

;; ====================================================================== 
;; Node

(defn node? []
  (exists? js/process))

;; ====================================================================== 
;; Karma Helpers

(defn karma? []
  (or (and (exists? js/window) (exists? (gobj/get js/window "__karma__")))
      (and (exists? js/global) (exists? (gobj/get js/global "__karma__")))))

(defmethod cljs.test/report [:jx.reporter.karma/karma :begin-test-ns] [m]
  (println "Testing" (name (:ns m))))

;; ====================================================================== 
;; Start Testing

(def ^:dynamic *run-fn* nil)

;; Karma starts the runner with arguments
(defn ^:export run! [a]
  (*run-fn* a))

(defn set-entry-point!
  "Sets the function to be run when starting the script"
  [f]
  {:pre [(ifn? f)]}
  (if (node?)
    (set! *main-cli-fn* f)
    (set! *run-fn* f)))

;; ====================================================================== 
;; Finish Testing 

(def ^:dynamic *exit-fn* nil)

(defn ^:export set-exit-point!
  "Sets the fn to be called when exiting the script.
   It should take one bool argument: successful?"
  [f]
  {:pre [(ifn? f)]}
  (set! *exit-fn* f))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (let [success? (successful? m)]
    (if (node?)
      (.exit js/process (if success? 0 1))
      (*exit-fn* success?))))
