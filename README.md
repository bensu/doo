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

* `js-env` can be any `chrome`, `firefox`, `safari`, `opera`,
`slimer`, `phantom`, `node`, or `rhino`. In the future it 
is planned to support `V8`, `jscore`, and others. It can also be an
alias: either user defined or built-in (see the Alias section) 
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

### Boot

`doo` is packaged as a Boot task in [boot-cljs-test](https://github.com/crisptrutski/boot-cljs-test).

### Library

To run a JS file in your preferred runner you can directly call
`doo.core/run-script` from Clojure:

```clj
(require '[doo.core :as doo])

(doo/run-script :phantom {:output-to "/path/to/the/file"})
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

    karma -v

In the future I plan to allow for customized commands in case you want to
run something like `/path/to/slimer/v1/slimerjs` instead of `slimerjs`.

> Remember that Rhino and Node don't come with a DOM so you can't call the
> window or document objects. They are meant to test functions and
> business logic, not rendering.

### Karma

#### Installation

[Karma](http://karma-runner.github.io/0.13/index.html)
is a comprehensive JavaScript test runner. It uses
[plugins](http://karma-runner.github.io/0.13/dev/plugins.html) to
extend functionality. We are interested in several "launcher" plugins
which start a browser on command. You might **want** any of:

    - karma-chrome-launcher
    - karma-firefox-launcher
    - karma-safari-launcher
    - karma-opera-launcher
    - karma-ie-launcher 

Alternatively, if you don't want `doo` to launch the browsers for you,
you can always launch them yourself and navigate to
[http://localhost:9876](http://localhost:9876) 

We also need to properly report `cljs.test` results inside Karma.
We'll **need** a "framework" plugin:

    - karma-cljs-test

Karma and its plugins are installed with `npm`, globally
with `npm install -g karma`, or local to
the project with `npm install karma`. These alternatives can't be
combined: you can't install `karma` globally and then install
`karma-chrome-launcher` locally. I recommend the local option since
you will probably get sucked into `npm` sooner rather than later
(if you are not already there). For local installation run:

    npm install karma karma-cljs-test --save-dev

and then install any of the launchers you'll use:

    npm install karma-chrome-launcher karma-firefox-launcher --save-dev
    npm install karma-safari-launcher karma-opera-launcher
    npm install karma-ie-launcher --save-dev

The `--save-dev` option informs `npm` that you only need the packages
during development and not when packaging artifacts.

The installation will generate both a `package.json`, specifying what was
installed and a `node-modules` folder with all the
installed modules. It is recommended to add `node-modules` to your
`.gitignore`. Leave `package.json` in git (even though it is
generated) so that other developers in your projects can run all your
install commands with: `npm install` (which reads from `package.json`). 

If you are using `lein-npm`, follow their [instructions](https://github.com/RyanMcG/lein-npm).

For global installation, run the same commands but add the `-g` option
as in `npm install -g karma`. Global installation will allow you to
use karma in all of your projects. The problem is that it won't be
explicitly configured in your project that karma is used for testing,
which makes it harder for new contributors to setup.

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
