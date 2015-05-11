(ns example.dev
  (:require [example.core :as core]))

(enable-console-print!)

(println "Development build:" (core/adder 1 1))
