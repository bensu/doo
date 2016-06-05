(ns example.electron-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [clojure.string :refer [split]]
            [example.electron :as electron]))

(deftest electron-app-path
  (is (= (last (split (.getAppPath electron/app) #"/"))
         "runner.electron")))

(deftest electron-window-get-size
  (let [window (electron/get-current-window)
        size (electron/get-window-size window)]
    (is (> (:width size) 0)
        "The Electron window width should be more than zero.")
    (is (> (:height size) 0)
        "The Electron window height should be more than zero.")))

(deftest electron-window-set-size
  (let [window (electron/get-current-window)
        original-size (electron/get-window-size window)
        new-size {:width (rand-int 500)
                  :height (rand-int 500)}]
    ;; resize window to a random size
    (electron/set-window-size! window new-size)
    (is (= new-size (electron/get-window-size window))
        "The Electron window should be resized to new-size.")

    ;; resize window back to original size
    (electron/set-window-size! window original-size)
    (is (= original-size (electron/get-window-size window))
        "The Electron window should be resized back to original-size.")))
