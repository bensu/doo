# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## Unreleased

### Added
- New runner: Headless Firefox (`firefox-headless`, [#160](https://github.com/bensu/doo/pull/160))
- Add support for arbitrary Karma configuration. ([#108](https://github.com/bensu/doo/pull/108), [#43](https://github.com/bensu/doo/issues/43), [#171](https://github.com/bensu/doo/pull/171))
- Add support for custom Karma launchers. ([#108](https://github.com/bensu/doo/pull/108))

### Changed
- Quoted values for `:main` in `:compiler-opts` are deprecated. Use a plain value instead. ([`0f6bc87`](https://github.com/bensu/doo/commit/0f6bc8764dacff1d7ebae954e85722ecf9c680e3))
  - Quoted values will continue to work for now, but doo will print a deprecation warning if you use them.
  - Example: if you have `{:main 'my-project.runner}`, change it to `{:main myproject.runner}`.

### Fixed
- Make ClojureScript compiler warnings about `:preloads` go away. ([#163](https://github.com/bensu/doo/issues/163))
- Make Karma warnings about matchers go away. ([#153](https://github.com/bensu/doo/pull/153))
- Fix the need to quote symbols in Doo build configuration. ([#133](https://github.com/bensu/doo/issues/133), [#172](https://github.com/bensu/doo/pull/172))

## [0.1.8] - 2017-10-02

### Added
- New runner: Headless Chrome (`chrome-headless`, [#136](https://github.com/bensu/doo/pull/136)) 

### Changed
- Improve the error messages when an exception is thrown outside the tests. ([#151](https://github.com/bensu/doo/issues/151))

### Fixed
- Made doo exit correctly with ClojureScript 1.9.854 and later. ([#141](https://github.com/bensu/doo/pull/141))
- Added support for Leiningen 2.7's managed dependencies. ([#143](https://github.com/bensu/doo/pull/143))

## [0.1.7] - 2016-07-10

### Changed
- Wait 1sec after starting the Karma server so that the tests are run right after starting auto
- Fixed `lein doo` with no arguments
- Drop `experimental` for Karma

### Added
- Karma PhantomJS, Karma SlimerJS, and Karma Electron as new experimental runners
- `:debug` option to `doo.core/run-script`.
- Support for Chrome Canary from Karma.
- Add AppVeyor CI.
- `:exec-dir` option to `doo.core/run-script`.

### Fixed
- Fixed a problem with empty :alias map in configuration. ([#113](https://github.com/bensu/doo/issues/113))

## [0.1.6] - 2015-12-05

### Changed
- **BREAKING CHANGE:** changes the default `:karma` path to
    `"karma"` to use the CLI tool. If you were using the local
    installation, add `{:path {:karma "./node_modules/karma/bin/karma"}}`
    to your `doo` config in `project.clj`.
- Removes `cljsbuild` as a dependency.
- Swaps `selmer` for `data.json`.
- Removes limitations around absolute and relative paths for
  `doo.core/run-script` and the `compiler-options`.
- In auto mode, run karma as a server to avoid starting/stopping the
  browsers.
- Print the output of the script as it comes.

### Added
- `:debug` option to `doo.core/run-script`.
- `:verbose` option to `doo.core/run-script`.
- Optional regex argument to `doo-all-tests` to mirror
  `run-all-test`'s behavior.
- `nashorn` runner.
- Option to pass command line arguments to runners through `:paths`.
- Option to pass the Default Builds under `:doo {:build "build-id"}`
  in `project.clj`.
- Option to pass `'example.runner`, `"example.runner"`, or
  `example.runner` to `:main`.

## [0.1.5] - 2015-10-09

### Added
- Karma with `chrome`, `firefox`, `safari`, `opera`, and `ie` as
  runners.
- Custom `:paths` for the runners.
- Custom `:alias` to group runners.

### Changed
- Deletes the `browser` alias and replaces it with
  `headless` for `slimer` and `phantom`
- **BREAKING CHANGE** signature for `doo.core/run-script`.

## [0.1.4] - 2015-08-11

### Added
- Allows `:optimizations :none` for all platforms except for `rhino`.
- `browsers` alias to the plugin.

### Changed
- `valid-compiler-options?`'s signature to take `js-env`.

### Fixed
- Remove many superflous compiler option requirements.

## [0.1.3-SNAPSHOT] - 2015-07-26

### Added
- Support for absolute paths in the runners
- Allows projects to use node dependencies through `lein-npm`.
  Requires `node => 0.12`.

## [0.1.2-SNAPSHOT] - 2015-07-13

### Addded
- `node` support

### Changed
- `doo.core/run-script`'s signature.

## [0.1.1-SNAPSHOT] - 2015-07-05

### Added
- Option to run the plugin `once`

### Fixed
- Returns an UNIX exit code reflecting if the tests failed.

### Changed
- **BREAKING CHANGE**: requires `[org.clojure/clojurescript "0.0-3308"]` or newer.
