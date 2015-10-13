(ns doo.shell
  "Rewrite of clojure.java.shell to have access to the output stream."
  (:require [clojure.java.io :as io])
  (:import (java.io StringWriter BufferedReader InputStreamReader)
           (java.nio.charset Charset)))

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

(defn sh
  "Rewrite of clojure.java.shell/sh that writes output to console,
   as it happens by default."
  ([cmd] (sh cmd {:verbose true}))
  ([cmd opts]
   (let [capture! #(future
                     (if (:verbose opts)
                       (print-stream! %)
                       (stream-to-string %)))
         proc (.exec (Runtime/getRuntime) 
                ^"[Ljava.lang.String;" (into-array cmd))
         out (capture! (.getInputStream proc))
         err (capture! (.getErrorStream proc))
         exit-code (.waitFor proc)]
     {:exit exit-code :out @out :err @err})))
