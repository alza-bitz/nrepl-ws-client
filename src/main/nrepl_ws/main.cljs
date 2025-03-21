(ns nrepl-ws.main
  (:require
   [clay.readers :as readers]
   [cljs.reader :refer [read-string]]
   [goog.events :refer [listen]]
   [nrepl-ws.nrepl :as nrepl]
   [nrepl-ws.view :as view]
   [reagent.core :as r]
   [reagent.dom :as dom])
  (:import
   [goog.events EventType]))

;; Use defonce to preserve state across hot reloads
(def state (r/atom {:stream nil ;; should probably be a promise
                    :mode :repl}))

(defn fix-fragment [react-fragment]
  (when (vector? react-fragment)
    (if (keyword? (first react-fragment))
      react-fragment
      (vec (cons :div react-fragment)))))

(def config
  {:modes {:repl {:eval-message-fn nrepl/eval-message
                  :output-fn str
                  :output (r/atom "")}
           :kindly {:eval-message-fn nrepl/eval-message-clay
                    :output-fn (comp fix-fragment (partial read-string {:readers {'nrepl-ws/plotly readers/plotly}}))
                    :output (r/atom "")}}})

(defn mode-toggle-fn [state]
  (fn [] 
    (let [current-mode (:mode @state)
          new-mode (if (= current-mode :repl) :kindly :repl)]
      (js/console.log "Switching mode from " (pr-str current-mode) " to " (pr-str new-mode))
      (swap! state assoc :mode new-mode))))

;; Page unload handler
(defn setup-unload-listener []
  (listen js/window EventType.BEFOREUNLOAD
          (fn [_]
            (nrepl/close! state)
            nil)))

;; Before hot reload - clean up resources
(defn ^:dev/before-load stop []
  (js/console.log "Application reloading, cleaning up resources")
  (nrepl/close! state))

;; After hot reload - start app
(defn ^:dev/after-load start []
  (js/console.log "Starting/restarting application")
  (setup-unload-listener)
  (nrepl/connect! state config)
  (dom/render [view/view state config nrepl/eval! (mode-toggle-fn state)]
              (.getElementById js/document "main")))
