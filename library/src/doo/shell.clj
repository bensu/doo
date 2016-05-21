(ns doo.shell
  "Rewrite of clojure.java.shell to have access to the output stream."
  (:require [clojure.java.io :as io]
            [doo.utils :as utils]
            [clojure.string :as str]
            [clojure.java.shell :as shell])
  (:import (java.io StringWriter BufferedReader InputStreamReader File)
           (java.nio.charset Charset)))

;; ======================================================================
;; Util

(defn flatten-cmd [cmd]
  (vec (mapcat #(cond-> % (string? %) vector) cmd)))

;; ======================================================================
;; Config

(def base-dir "runners/")

;; ======================================================================
;; Shell

(defn- stream-to-string
  ([in] (stream-to-string in (.name (Charset/defaultCharset))))
  ([in enc]
   (with-open [bout (StringWriter.)]
     (io/copy in bout :encoding enc)
     (.toString bout))))

(defn print-stream! [stream]
  (let [r (BufferedReader. (InputStreamReader. stream))]
    (with-open [out (StringWriter.)]
      (loop [line (.readLine r)]
        (when (some? line)
          (.write out line)
          (println line)
          (recur (.readLine r))))
      (.close r)
      (.toString out))))

(defn- ^"[Ljava.lang.String;" str-array [xs]
  (into-array String xs))

(defn exec! [cmd ^File exec-dir]
  (let [windows? (= :windows (utils/get-os))
        windows-cmd (when windows? ["cmd" "/c"])
        command-arr (str-array (concat windows-cmd cmd))]
    (if exec-dir
      (.exec (Runtime/getRuntime) command-arr nil exec-dir)
      (.exec (Runtime/getRuntime) command-arr))))

(defn capture-process! [process opts]
  (letfn [(capture! [stream]
            (future
              (if (:verbose opts)
                (print-stream! stream)
                (stream-to-string stream))))]
    {:out (capture! (.getInputStream process))
     :err (capture! (.getErrorStream process))}))

(defn set-cleanup!
  ([process opts]
   (set-cleanup! process opts "Shutdown Process"))
  ([process opts msg]
   (.addShutdownHook (Runtime/getRuntime)
     (Thread. ^Runnable
              (fn []
                (println msg)
                (.destroy process))))))


(def terminal-notify false)
(def always-notify false)

(defn- escape [message]
  (str/replace message "[" "\\["))

(defn- notify [title-postfix message]
  (try
    (shell/sh "terminal-notifier" "-message" (escape message) "-title" (str "AutoExpect - " (escape title-postfix)))
    (catch Exception ex
      (println "Problem communicating with notification center, please make sure you installed terminal-notifier (e.g. using 'brew install terminal-notifier'), exception:" (.getMessage ex)))))


(def last-run (atom nil))

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

(defn handle-notifications [out]
  (let [new (reset! last-run (get-assertion-string @out))
        new-status (notify-title new)
        old-status (notify-title @last-run)
        changed (not= new-status old-status)]
    (when (or always-notify changed)
      (notify new-status new))))

(defn sh
  "Rewrite of clojure.java.shell/sh that writes output to console,
   as it happens by default."
  ([cmd] (sh cmd {:verbose true}))
  ([cmd opts]
   (let [proc (exec! cmd (:exec-dir opts))
         {:keys [out err]} (capture-process! proc opts)
         exit-code (.waitFor proc)]
     (when terminal-notify
       (handle-notifications out))
     {:exit exit-code :out @out :err @err})))
