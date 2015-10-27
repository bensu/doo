# doo

[![Circle CI](https://circleci.com/gh/bensu/doo.svg?style=svg)](https://circleci.com/gh/bensu/doo)

A library and Leiningen plugin to run `cljs.test` in many JS environments.

>  ...and I would have gotten away with it, too, if it wasn't for you meddling kids.

`[lein-doo 0.1.4]`

Versions from `[0.1.1-SNAPSHOT]` onwards need
`[org.clojure/clojurescript "0.0-3308"]`. 

## Usage

### Plugin

    lein doo {js-env} {build-id}

    lein doo {js-env} {build-id} {watch-mode}

* `js-env` can be any `slimer`, `phantom`, `node`, or `rhino`. In the future it
is planned to support `V8`, `jscore`, and others. It can also be the alias
`browsers` which resolves to `slimer` and `phantom` (both get executed).
* `watch-mode` (optional): either `auto` (default) or `once` which
  exits with 0 if the tests were successful and 1 if they failed.
* `build-id` is one of your `cljsbuild` profiles. For example `test` from:

```clj
:cljsbuild {:test {:source-paths ["src" "test"]
    			   :compiler {:output-to "resources/public/js/testable.js"
                              :main 'your-project.runner
                              :optimizations :none}}}
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
them in `your-project.runner` even if you don't call any of their
functions. You can also call `(doo.runner/doo-all-tests)` which wraps
`cljs.test/run-all-tests` to run tests in all loaded namespaces.

Then you can run:

    lein doo slimer test

which starts an ClojureScript autobuilder for the `test` profile and
runs `slimerjs` on it when it's done.

### Library

To run a JS file in your preferred runner you can directly call
`doo.core/run-script` from Clojure:

```clj
(require '[doo.core :as doo])

(doo/run-script :phantom "/path/to/the/file")
```

In general, the path to the file should be absolute.

## Setting up Environments

This is the hardest part and `doo` doesn't do it for you (yet?). Right
now if you want to run
[`slimer`](http://docs.slimerjs.org/current/installation.html),
[`phantom`](http://phantomjs.org/download.html), or [`node`](https://github.com/joyent/node/wiki/Installation) you need to install them
so that these commands work on the command line:

    phantomjs -v

    slimerjs -v

    node -v

    rhino -help

In the future I plan to allow for customized commands in case you want to
run something like `/path/to/slimer/v1/slimerjs` instead of `slimerjs`.

> Remember that Rhino and Node don't come with a DOM so you can't call the
> window or document objects. They are meant to test functions and
> business logic, not rendering.

### Slimer & Phantom

When using `slimer` and `phantom` with `:none` make sure your
`:output-dir` is either unspecified or an absolute path. `doo` will
bark otherwise.

If you want to run both, use `lein doo browsers {build-id} {watch-mode}`.

Do not install Slimer with homebrew unless you know what you
are doing. There are
[reports](https://groups.google.com/forum/#!topic/clojurescript/4EF-NAzu-kM)
of it not working with ClojureScript when installed that way because
of dated versions.

### Node

`*main-cli-fn*` is not needed (but can be used), since `doo`
initializes the tests. `:output-dir` is needed whenever you are using `:none`.
`:hashbang false` and `:target :nodejs` are always needed.

```clj
:node-test {:source-paths ["src" "test"]
            :compiler {:output-to "target/testable.js"
                       :output-dir "target"
                       :main 'example.runner
                       :optimizations :none
                       :hashbang false
                       :target :nodejs}}
```

## Travis CI

To run on [travis](https://travis-ci.org/) there is a sample `.travis.yml` file in the example project: [example/.travis.yml](example/.travis.yml)

(Currently only tested with PhantomJS.)

## Changes

* `0.1.4-SNAPSHOT` allows `:optimizations :none` for all platforms but
  `rhino`, changes `valid-compiler-options?`'s signature to take
  `js-env`, adds the `browsers` alias, and changes many of the
  compiler requirements.
* `0.1.3-SNAPSHOT` adds support for absolute paths in the runners and
  allows projects to use node dependencies through `lein-npm`.
  Requires `node => 0.12`.
* `0.1.2-SNAPSHOT` adds `node` support and changed `run-script`'s interface.
* `0.1.1-SNAPSHOT` adds the option for `once` and returns an UNIX exit
code reflecting if the tests failed, to be used in CI builds. It also
**requires [org.clojure/clojurescript "0.0-3308"]** or newer.

## License

Most code in this project is a repackaging of
[cemerick/clojurescript.test](https://github.com/cemerick/clojurescript.test),
therefore most of the credit goes to Chas Emerick and contributors to
that project.

Copyright © 2015 Sebastian Bensusan and Contributors.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
