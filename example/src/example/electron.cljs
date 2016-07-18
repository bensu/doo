(ns example.electron)

(def electron (js/require "electron"))
(def remote (.-remote electron ))
(def app (.-app remote))

(defn get-current-window
  []
  (.getCurrentWindow remote))

(defn get-window-size
  "Helper function to get Electron window size."
  [window]
  (let [[width height] (array-seq (.getSize window))]
    {:width width
     :height height}))

(defn set-window-size!
  "Helper function to set Electron window size."
  [window {:keys [width height]}]
  (.setSize window width height))
