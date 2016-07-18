(ns doo.utils
  "Taken from leiningen.core.utils"
  (:import java.io.File)
  (:require [clojure.java.io :as io]))

(defn- get-by-pattern
  "Gets a value from map m, but uses the keys as regex patterns, trying
  to match against k instead of doing an exact match."
  [m k]
  (m (first (drop-while #(nil? (re-find (re-pattern %) k))
                        (keys m)))))

(defn- get-with-pattern-fallback
  "Gets a value from map m, but if it doesn't exist, fallback
   to use get-by-pattern."
  [m k]
  (let [exact-match (m k)]
    (if (nil? exact-match)
      (get-by-pattern m k)
      exact-match)))

(def ^:private native-names
  {"Mac OS X" :macosx "Windows" :windows "Linux" :linux
   "FreeBSD"  :freebsd "OpenBSD" :openbsd
   "amd64"    :x86_64 "x86_64" :x86_64 "x86" :x86 "i386" :x86
   "arm"      :arm "SunOS" :solaris "sparc" :sparc "Darwin" :macosx})

(defn get-os
  "Returns a keyword naming the host OS."
  []
  (get-with-pattern-fallback native-names (System/getProperty "os.name")))

(defn debug-log [& args]
  (apply println "[doo]" args))

;; TODO: runner arg is not necessary
(defn runner-path!
  "Creates a temp file for the given runner resource file."
  ([base-dir runner filename]
   (runner-path! base-dir runner filename {:common? false}))
  ([base-dir runner filename {:keys [common?]}]
   (letfn [(slurp-resource [res]
             (slurp (io/resource (str base-dir res))))
           (add-common [file]
             (when common?
               (spit file (slurp-resource "common.js"))))]
     (.getAbsolutePath
      (doto (File/createTempFile (name runner) ".js")
        .deleteOnExit
        add-common
        (spit (slurp-resource filename) :append true))))))
