# doo

[![Circle CI](https://circleci.com/gh/bensu/doo.svg?style=svg)](https://circleci.com/gh/bensu/doo)
[![Build status](https://ci.appveyor.com/api/projects/status/ufuprdbgn5afhudt?svg=true)](https://ci.appveyor.com/project/bensu/doo)

A library and Leiningen plugin to run `cljs.test` in many JS environments.

>  ...and I would have gotten away with it, too, if it wasn't for you meddling kids.

This README is for the latest stable release:

`[lein-doo "0.1.6"]`

To use doo you need to use `[org.clojure/clojurescript "0.0-3308"]` or
newer.

## Usage

### Plugin

All arguments are optional provided there is a corresponding default under `:doo`
in `project.clj`:

    lein doo

    lein doo {js-env}

    lein doo {js-env} {build-id}

    lein doo {js-env} {build-id} {watch-mode}

* `js-env` can be any `chrome`, `firefox`, `ie`, `safari`, `opera`,
`slimer`, `phantom`, `node`, `rhino`, or `nashorn`. In the future it
is planned to support `v8`, `jscore`, and others.
* `watch-mode` (optional): either `auto` (default) or `once` which
  exits with 0 if the tests were successful and 1 if they failed.
* `build-id` is one of your `cljsbuild` profiles. For example `test` from:

```clj
:cljsbuild
  {:builds [{:id "test"
             :source-paths ["src" "test"]
             :compiler {:output-to "resources/public/js/testable.js"
                        :main your-project.runner
                        :optimizations :none}}]}
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
Notice that `doo-tests` needs to be called in the top level and can't
be called inside a function (unless you explicitly call that function
in the top level).

Then you can run:

    lein doo slimer test

which starts an ClojureScript autobuilder for the `test` profile and
runs `slimerjs` on it when it's done.

You can also call `doo` without a `build-id` (as in `lein doo phantom`) as
long as you specify a [Default Build](#default-build) in your `project.clj`.

### Boot

`doo` is packaged as a Boot task in [boot-cljs-test](https://github.com/crisptrutski/boot-cljs-test).

### Library

To run a JavaScript file in your preferred runner you can directly call
`doo.core/run-script` from Clojure:

```clj
(require '[doo.core :as doo])

(let [doo-opts {:paths {:karma "karma"}}
      compiler-opts {:output-to "out/testable.js"
                     :optimizations :none}]
  (doo/run-script :phantom compiler-opts doo-opts))
```

You can run `doo.core/run-script` with the following arguments:

```
(run-script js-env compiler-opts)
(run-script js-env compiler-opts opts)
```

where:

* `js-env` - any of `:phantom`, `:slimer`, :`node`, `:rhino`,
  `:nashorn`, `:chrome`, `:firefox`, `:ie`, `:safari`, or `:opera`
* `compiler-opts` - the options passed to the ClojureScript when it
  compiled the script that doo should run
* `opts` - a map that can contain:
    `:verbose` - bool (default true) that determines if the scripts
    output should be printed and returned (verbose true) or only
    returned (verbose false).
    `:debug` - bool (default false) to log to standard-out internal events
             to aid debugging
    `:paths` - a map from runners (keywords) to string commands for bash.
    `:exec-dir` - a directory path (file) from where runner should be
    executed. Defaults to nil which resolves to the current dir

## Setting up Environments

This is the hardest part and `doo` doesn't do it for you (yet?). Right
now if you want to run
[`slimer`](http://docs.slimerjs.org/current/installation.html),
[`phantom`](http://phantomjs.org/download.html), [`node`](https://github.com/joyent/node/wiki/Installation)
or [nashorn](http://openjdk.java.net/projects/nashorn/) that ships with the JDK 8,
you need to install them so that these commands work on the command line:

    phantomjs -v

    slimerjs -v

    node -v

    jjs -h

    rhino -help

If you want to use a different command to run a certain runner, see
Paths.

> Remember that Rhino and Node don't come with a DOM so you can't call the
> window or document objects. They are meant to test functions and
> logic, not rendering.

### Slimer & Phantom

If you want to run both, use `lein doo headless {build-id} {watch-mode}`.

Do not install Slimer with homebrew unless you know what you
are doing. There are
[reports](https://groups.google.com/forum/#!topic/clojurescript/4EF-NAzu-kM)
of it not working with ClojureScript when installed that way because
of dated versions.

> Note: Slimer does not
> [currently](https://github.com/laurentj/slimerjs/pull/278) throw
> error exit codes when encountering an error, which makes them
> unsuitable for CI testing.

### Node

Some requirements:

* Minimum node version required: `0.12`
* `:output-dir` is needed whenever you are using `:none`.
* `:target :nodejs` is always needed.

```clj
:node-test {:source-paths ["src" "test"]
            :compiler {:output-to "target/testable.js"
                       :output-dir "target"
                       :main example.runner
                       :target :nodejs}}
```

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

Karma and its plugins are installed with `npm`. It is
[recommended](http://karma-runner.github.io/0.13/intro/installation.html)
that you install Karma and it's plugins locally in the projects directory
with `npm install karma --save-dev`. It is possible to install Karma and
its plugins globally with `npm install -g karma`, but this is not recommended.
It is not possible to run mix local and global Karma and Karma plugins.

Karma provides a [CLI tool](https://github.com/karma-runner/karma-cli)
to make running Karma simpler and to ease cross platform compatibility.
doo uses the CLI tool as the default runner, if you don't install it you will
need to configure doo.

For local installation run:

    npm install karma karma-cljs-test --save-dev

and install the Karma CLI tool globally with

    npm install -g karma-cli

then install any of the launchers you'll use:

    npm install karma-chrome-launcher karma-firefox-launcher --save-dev
    npm install karma-safari-launcher karma-opera-launcher --save-dev
    npm install karma-ie-launcher --save-dev

The `--save-dev` option informs `npm` that you only need the packages
during development and not when packaging artifacts.

The installation will generate a `node-modules` folder with all the
installed modules. It is recommended to add `node-modules` to your
`.gitignore`.

If you are using `lein-npm`, follow their
[instructions](https://github.com/RyanMcG/lein-npm).

#### Non-standard Karma configuration

If you are using a local installation and/or `node_modules` is not located
at the project root, you need to tell `doo` about it. Add this to your
`project.clj`:

```clj
:doo {:paths {:karma "path/to/node_modules/karma/bin/karma"}}

:cljsbuild { your-builds }
```

and make sure that the file `karma/bin/karma` exists inside
`node_modules`. If your `package.json` and `node_modules` folder are in the
same directory than your `project.clj`, then you should use:

```clj
:doo {:paths {:karma "./node_modules/karma/bin/karma"}}

:cljsbuild { your-builds }
```

For more info on `:paths` see Paths.

Global installation will allow you to
use karma in all of your projects. The problem is that it won't be
explicitly configured in your project that karma is used for testing,
which makes it harder for new contributors to setup.

> In some systems (e.g. Ubuntu) you might need to run all npm commands
> as root:
> 	sudo npm install karma --save-dev

### Karma Phantom and Karma Slimer (experimental)

To avoid starting a new Slimer/Phantom on every run while using `auto`, we can use
Slimer/Phantom through Karma.

Install any of the launchers you'll use:

    npm install karma-phantomjs-launcher --save-dev
    npm install karma-slimerjs-launcher --save-dev

and call

    lein doo karma-phantom test auto
    lein doo karma-slimer test auto

If you are using `once`, the regular `phantom`/`slimer` runners are recommended.

> Note: karma-slimer sometimes fails to close the running Slimer instance,
> which you need to close manually.

### Electron (experimental)

After installing [Electron](http://electron.atom.io/releases/) install the launcher with

    npm install karma-electron-launcher --save-dev

and call

    lein doo electron test

## Paths

You might want to use a different version of node, or the global
version of Karma, or any other binary to run your tests for a given
environment. You can configure that paths like so:

```clj
:doo {:paths {:node "user/local/bin/node12"
              :karma "./frontend/node_modules/karma/bin/karma"}

:cljsbuild { your-builds }
```

Paths can also be used to pass command line arguments to the runners:

```clj
:doo {:paths {:phantom "phantomjs --web-security=false"
              :slimer "slimerjs --ignore-ssl-errors=true"
              :karma "karma --port=9881 --no-colors"
              :rhino "rhino -strict"
              :node "node --trace-gc --trace-gc-verbose"}}
```

## Aliases

You might want to group runners and call
them from the command line. For example, while developing you might
only be interested in `chrome` and `firefox`, but you also want to
test with `safari` before doing a deploy:

```clj
:doo {:alias {:browsers [:chrome :firefox]
              :all [:browsers :safari]}}

:cljsbuild { my-builds }
```

Then you can use:

```sh
lein doo browsers my-build  # runs chrome and firefox

lein doo all my-build # runs chrome, firefox, and safari
```

As you can see, aliases can be recursively defined: watch for circular
dependencies or `doo` will bark.

The only built-in alias is `:headless [:phantom :slimer]`.

## Default Build

To save you one command line argument, `lein-doo` lets you specify a
default build in your `project.clj`:

```clj
:doo {:build "some-build-id"
      :paths { ... }
      :alias { ... }}

:cljsbuild
  {:builds [{:id "some-build-id"
             :source-paths ["src" "test"]
             :compiler {:output-to "out/testable.js"
                        :optimizations :none
                        :main example.runner}}]}
```

## Travis CI

To run on [travis](https://travis-ci.org/) there is a sample `.travis.yml` file in the example project: [example/.travis.yml](example/.travis.yml)

(Currently only tested with PhantomJS.)

## Developing

To run the tests for doo, you need to have installed rhino, phantomjs, slimer, chrome, node, and firefox. You will also need to run `npm install` in the `library` directory.

## License

Most code in this project is a repackaging of
[cemerick/clojurescript.test](https://github.com/cemerick/clojurescript.test),
therefore most of the credit goes to Chas Emerick and contributors to
that project.

Copyright © 2015 Sebastian Bensusan and Contributors.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
