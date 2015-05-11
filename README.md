# doo 

>  ...and I would have gotten away with it, too, if it wasn't for you meddling kids.

A library and Leiningen plugin to run `cljs.test` in many JS environments.

## Usage

### Plugin

    lein doo {js-env} {build-id}

* `js-env` can be any of `slimer` or `phantom`. In the future it
is planned to support `node`, `rhino`, `V8`, `jscore`, and others.
* `build-id` is one of your `cljsbuild` profiles. For example `test` from:

```clj
:cljsbuild {:test {:source-paths ["src" "test"]
    			   :compiler {:output-to "resources/public/js/testable.js"
                              :main 'your-project.runner
                              :optimizations :whitespace}}}
```

Notice that `:main` is set to the namespace `your-project.runner`
where you define which test namespaces you want to run using:

```clj
(ns your-project.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [your-project.core-test]
              [your-project.util-test]))

(doo-tests 'your-project.core-test
           'your-project.util-test)
```

`doo.runner/doo-tests` works just like `cljs.test/run-tests` but it places hooks
around the tests to know when to start them and finish them. Since it
is a macro that will be calling said namespaces, you need to require
them in `your-project.runner` even if you don't call any of their functions.

Then you can run:

    lein doo slimer test

which starts an ClojureScript autobuilder for the `test` profile and
runs `slimerjs` on it when it's done. The build profiles must set
`:optimizations` to **anything but `:none`** (we need all the JavaScript
to be in one file).

### Library

To run a Js file in many environments you can directly call
`doo.core/run-script` from Clojure:

```clj
(require '[doo.core :as doo])

(doo/run-script :phantom {:output-to "path/to/the/file"})
```

If you don't want to run test and run your own functions you need to
handle the entry and exit points yourself from ClojureScript:

```clj
(require '[doo.runner :as run])

(defn main
    "The fn you want to run"
    []
    ;; what you want to do
    (catch-the-ghost!)
    ;; and then exit when you are done
    (run/*exit-fn*))
    
(run/set-entry-point! main)
```

You should not change `doo.runner/*exit-fn*` since it is used by the
script runner to know when to exit.

## License

Most code in this project is a repackaging of
[cemerick/clojurescript.test](https://github.com/cemerick/clojurescript.test),
therefore most of the credit goes to Chas Emerick and contributors to
that project.

Copyright © 2015 Sebastian Bensusan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
