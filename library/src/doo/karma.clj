(ns doo.karma
  (:import java.io.File)
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [doo.shell :as shell]
            [doo.utils :as utils]))

;; ======================================================================
;; Karma Clients

(defn- karma-plugin-name [name]
  (str "karma-" name "-launcher"))

(def karma-envs
  {:chrome          {:plugin (karma-plugin-name "chrome")
                     :name   "Chrome"}
   :chrome-canary   {:plugin (karma-plugin-name "chrome")
                     :name   "ChromeCanary"}
   :chrome-headless {:plugin (karma-plugin-name "chrome")
                     :name   "ChromeHeadless"}
   :firefox         {:plugin (karma-plugin-name "firefox")
                     :name   "Firefox"}
   :safari          {:plugin (karma-plugin-name "safari")
                     :name   "Safari"}
   :opera           {:plugin (karma-plugin-name "opera")
                     :name   "Opera"}
   :ie              {:plugin (karma-plugin-name "ie")
                     :name   "IE"}
   :karma-phantom   {:plugin (karma-plugin-name "phantomjs")
                     :name   "PhantomJS"}
   :karma-slimer    {:plugin (karma-plugin-name "slimerjs")
                     :name   "SlimerJS"}
   :electron        {:plugin (karma-plugin-name "electron")
                     :name   "Electron"}})

(def envs (set (keys karma-envs)))

(defn env? [js]
  (contains? envs js))

;; In Karma all paths (including config.files) are normalized to
;; absolute paths using the basePath.
;; It's important to pass it as an option and it should be the same
;; path from where the compiler was executed. Othwerwise it's extracted
;; from the karma.conf.js file passed (if it's relative the current directory
;; becomes the basePath).
;; http://karma-runner.github.io/0.10/config/configuration-file.html
;; https://github.com/karma-runner/karma/blob/master/lib/config.js#L80

;; TODO: cljs_deps.js is not being served by karma's server which
;; triggers a warning. It's not a problem because it was already
;; included, but it would be better to reproduce the server behavior
;; expected by the none shim
;; https://github.com/clojure/clojurescript/blob/master/src/main/clojure/cljs/closure.clj#L1152

(defn js-env->plugin [js-env]
  (get-in karma-envs [js-env :plugin]))

(defn js-env->browser [js-env]
  (get-in karma-envs [js-env :name]))

(defn ->karma-opts [js-envs compiler-opts]
  (letfn [(->out-dir [p]
            (str (:output-dir compiler-opts) p))]
    {"frameworks" ["cljs-test"]
     ;; basePath should be the path from where the compiler thinks the
     ;; resources will be served: :asset-path or :output-dir
     "basePath" (System/getProperty "user.dir")
     "plugins" (into ["karma-cljs-test"] (mapv js-env->plugin js-envs))
     "browsers" (mapv js-env->browser js-envs)
     ;; All this assumes that the output-dir is relative to the user.dir
     ;; base path
     ;; WARNING: the order of the files is important, don't change it.
     "files" (concat
               (when (= :none (:optimizations compiler-opts))
                 (mapv ->out-dir ["/goog/base.js" "/cljs_deps.js"]))
               [(:output-to compiler-opts)
                {"pattern" (->out-dir "/**") "included" false}
                {"pattern" (->out-dir "/**/*.js") "included" false}
                {"pattern" (->out-dir "/*.js") "included" false}])
     "autoWatch" false
     "client" {"args" ["doo.runner.run_BANG_"]}
     "singleRun" true}))

(defn write-var [writer var-name var-value]
  (.write writer (str "var " var-name " = "))
  (.write writer (with-out-str
                   (json/pprint var-value :escape-slash false)))
  (.write writer ";\n"))

(defn runner!
  "Creates a file for the given runner resource file in the users dir"
  [js-envs compiler-opts opts]
  {:pre [(some? (:output-dir compiler-opts))]}
  (let [karma-tmpl (slurp (io/resource (str shell/base-dir "karma.conf.js")))
        karma-opts (cond-> (->karma-opts js-envs compiler-opts)
                     (:install? (:karma opts)) (assoc "singleRun" false))
        f (File/createTempFile "karma_conf" ".js")]
    (.deleteOnExit f)
    (with-open [w (io/writer f)]
      (write-var w "configData" karma-opts)
      (io/copy karma-tmpl w))
    (when (:debug opts)
      (utils/debug-log "Created karma conf file:" (.getAbsolutePath f)))
    (.getAbsolutePath f)))
