(require 'cljs.build.api)

(defrecord SourcePaths [paths]
  cljs.closure/Compilable
  (-compile [_ opts]
    (mapcat #(cljs.closure/-compile % opts) paths)))

(cljs.build.api/build
  (SourcePaths. ["src"])
  {:output-dir "/home/carlos/OpenSource/cljs-test/target/cljsbuild-compiler-0",
   :main "minimal-env.test"
   :verbose true,
   :output-to "resources/public/js/testable.js",
   :optimizations :none,
   :warnings true,
   :externs ["closure-js/externs"],
   :libs ["closure-js/libs"],
   :pretty-print true})
