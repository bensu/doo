(ns doo.notifier
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]))

(def last-run (atom nil))

(defn- escape [message]
  (str/replace message "[" "\\["))

(defn- notify [title-postfix message]
  (try
    (shell/sh "terminal-notifier" "-message" (escape message) "-title" (str "doo - " (escape title-postfix)))
    (catch Exception ex
      (println "Problem communicating with notification center, please make sure you installed terminal-notifier (e.g. using 'brew install terminal-notifier'), exception:" (.getMessage ex)))))

(defn get-assertion-string [s]
  (when s
    (re-find #"Ran \d+ tests containing.+" s)))

(defn notify-title [s]
  (when s
    (condp re-find s
      #"0 failures, 0 errors" "Ok"
      #"0 errors" "Failed"
      #"0 failures" "Errors"
      s)))

(defn handle-notifications [out opts]
  (when (:notify opts)
    (let [old-status (notify-title @last-run)
          new (reset! last-run (get-assertion-string @out))
          new-status (notify-title new)
          changed (not= new-status old-status)]
      (when (or changed (= :always (:notify opts)))
        (notify new-status new)))))
