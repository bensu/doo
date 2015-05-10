(require 'cljs.build.api)

(cljs.build.api/build
  ["src"]
  {:output-dir "/home/carlos/OpenSource/cljs-test/target/cljsbuild-compiler-0",
   :main 'minimal-env.test,
   :verbose true,
   :output-to "resources/public/js/testable.js",
   :optimizations :none,
   :warnings true,
   :externs ["closure-js/externs"],
   :libs ["closure-js/libs"],
   :pretty-print true})
