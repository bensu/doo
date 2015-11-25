(ns doo.shell
  "Rewrite of clojure.java.shell to have access to the output stream."
  (:require [clojure.java.io :as io]
            [doo.utils :as utils])
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

(defn exec* [base-dir command-arr]
  (if base-dir
    (.exec (Runtime/getRuntime) command-arr nil base-dir)
    (.exec (Runtime/getRuntime) command-arr)))

(defn exec! [cmd path]
  (let [windows? (= :windows (utils/get-os))
        windows-cmd (when windows? ["cmd" "/c"])
        base-dir (when path (.getParentFile (File. path)))]
    (->> cmd
         (concat windows-cmd)
         ^"[Ljava.lang.String;" (into-array String)
         (exec* base-dir))))

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
     (Thread. (fn []
                (println msg)
                (.destroy process))))))

(defn sh
  "Rewrite of clojure.java.shell/sh that writes output to console,
   as it happens by default."
  ([cmd] (sh cmd {:verbose true}))
  ([cmd opts]
   (let [proc (exec! cmd (:base-dir opts))
         {:keys [out err]} (capture-process! proc opts)
         exit-code (.waitFor proc)]
     {:exit exit-code :out @out :err @err})))
