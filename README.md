# doo 

A library and Leiningen plugin to run `cljs.test` in many JS environments.

>  ...and I would have gotten away with it, too, if it wasn't for you meddling kids.

[![Clojars Project](http://clojars.org/lein-doo/latest-version.svg)](http://clojars.org/lein-doo)

Versions from `[0.1.1-SNAPSHOT]` onwards need
`[org.clojure/clojurescript "0.0-3308"]`. 

## Usage

### Plugin

    lein doo {js-env} {build-id}

    lein doo {js-env} {build-id} {watch-mode}

* `js-env` can be any `slimer`, `phantom`, or `rhino`. In the future it
is planned to support `node`, `V8`, `jscore`, and others.
* `watch-mode` (optional): either `auto` (default) or `once` which
  exits with 0 if the tests were successful and 1 if they failed.
* `build-id` is one of your `cljsbuild` profiles. For example `test` from:

```clj
:cljsbuild {:test {:source-paths ["src" "test"]
    			   :compiler {:output-to "resources/public/js/testable.js"
                              :main 'your-project.runner
                              :optimizations :whitespace}}}
```

Notice that `:main` is set to the namespace `your-project.runner`
where you define which test namespaces you want to run, using:

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

To run a JS file in your preferred runner you can directly call
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
    "The function you want to run"
    []
    ;; what you want to do
    (catch-the-ghost!)
    ;; and then exit when you are done
    (run/*exit-fn*))
    
(run/set-entry-point! main)
```

You should not change `doo.runner/*exit-fn*` since it is used by the
script runner to know when to exit.

## Setting up Environments

This is the hardest part and `doo` doesn't do it for you (yet?). Right
now if you want to run
[`slimer`](http://docs.slimerjs.org/current/installation.html) and [`phantom`](http://phantomjs.org/download.html) you need to install them
so that these commands work on the command line:

    phantomjs -v

    slimerjs -v

    rhino -help

In the future I plan to allow for customized commands in case you want to
run something like `/path/to/slimer/v1/slimerjs` instead of `slimerjs`.

> Note: Do not install slimerjs with homebrew unless you know what you
> are doing. There are
> [reports](https://groups.google.com/forum/#!topic/clojurescript/4EF-NAzu-kM)
> of it not working with ClojureScript when installed that way because
> of dated versions.

> Remember that Rhino doesn't come with a DOM so you can't call the
> window or document objects. It is meant to test functions and
> business logic not rendering.

## Travis CI
To run on [travis](https://travis-ci.org/) there is a sample `.travis.yml` file in the example project: [example/.travis.yml](example/.travis.yml)

(Currently only tested with PhantomJS.)

## Changes

* `0.1.1-SNAPSHOT` adds the option for `once` and returns an UNIX exit
code reflecting if the tests failed, to be used in CI builds. It also
**requires [org.clojure/clojurescript "0.0-3308"]** or newer.

## License

Most code in this project is a repackaging of
[cemerick/clojurescript.test](https://github.com/cemerick/clojurescript.test),
therefore most of the credit goes to Chas Emerick and contributors to
that project.

Copyright © 2015 Sebastian Bensusan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
