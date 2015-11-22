# End to end testing example

Create a `clojure.test` test that compiles a runner that selects end-to-end style test and uses `doo/run-script` to run
the resulting JavaScript:

```clojure
(ns my-app.end-to-end-test
  (:require [clojure.test :refer :all]
            [doo.core :as doo]
            [cljs.build.api :as cljs]
            ;; These will be used in the following steps
            [com.stuartsierra.component :as component]
            [my-app.system :as system]
            [ring.middleware.cors :refer [wrap-cors]]))

(deftest end-to-end-suite
  (let [compiler-opts {:main 'my-app.e2e-runner
                       :output-to "out/test.js"
                       :output-dir "out"
                       :asset-path "out"
                       :optimizations :none}]
    (binding [*print-length* nil]
      (cljs/build (apply cljs/inputs ["src/cljs" "test/cljs" "test/cljs-app-config"]) compiler-opts))
    (-> (doo/run-script :phantom compiler-opts {})
        :exit
        zero?
        is)))
```

Add a fixture that starts & stops the app (example below uses Stuart Sierra's [component](https://github.com/stuartsierra/component)):

```clojure
(use-fixtures :each
  (fn [f]
    (let [system (-> (system/new-system {:http {:port 3333}
                                         :app {:middleware [(fn [handler]
                                                              (wrap-cors handler
                                                                         :access-control-allow-origin [#".*"]
                                                                         :access-control-allow-methods [:get :put :post :delete]))]}})
                     (assoc :nrepl {}) ;; Mock unnecessary dependencies before start
                     component/start)]
      ;; Perform post start setup here, like adding a test user to a database
      (f) ;; run the test
      (component/stop system)))) ;; Finally stop the app
```

The code above uses [Ring CORS](https://github.com/r0man/ring-cors) in order to allow AJAX calls made by the runner
which runs the test from a local file. Although the example uses
[Duct components](https://github.com/weavejester/duct/wiki/Components), idea is to pass in CORS middleware at testing time.

Provide base url for the app under test in `test/cljs_app_config/my_app/config.cljs` (use a different base url at
production time by providing one in production sources e.g. `src/cljs_app_config/my_app/config.cljs`):

```clojure
(ns my-app.config)

(def base "http://localhost:3333")
```

The runner might look something like this:

```clojure
(ns my-app.e2e-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [my-app.e2e-test]))

(doo-tests 'my-app.e2e-test)
```

The app under test can then access the server, maybe something like this (using [cljs-react-test](https://github.com/bensu/cljs-react-test)):

```clojure
(ns my-app.main
  (:require [reagent.core :as r]
            [ajax.core :refer [GET]]
            [my-app.config]))

(def state (r/atom :loading))

(defn main-view []
  [:div
   (condp = @state
     :loading "Loading..."
     :started "...")])

(defn render-app [target]
  (r/render [main-view] target)
  (GET (str config/base "/data") {:handler (fn [data] (reset! state :started))}))

...

(ns my-app.e2e-test
  (:require [cljs.test :refer-macros [async deftest is testing use-fixtures]]
            [reagent.core :as reagent]
            [my-app.main :as main]
            [cljs-react-test.simulate :as sim]
            [cljs-react-test.utils :as tu]))

(def container (atom nil))

(use-fixtures :each
  {:before #(async done
              (reset! container (tu/new-container!))
              (done))
   :after #(tu/unmount! @container)})

(deftest start-the-app
  (testing "First view is loading"
    (is (= :loading @main/state)))
  (async done
    (main/render-app @container)
    (.setTimeout js/window
                 (fn []
                   (testing "Then the app is started"
                     (is (= :started @main/state)))
                     (done))
                 1000)))

```
Notice that since the test framework boots up the app, we can access and verify it's state (applies to smaller
components too).
