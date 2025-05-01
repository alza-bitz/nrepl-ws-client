(ns nrepl-ws.main
  (:require
   [clay.readers :as readers]
   [cljs.reader :refer [read-string]]
   [goog.events :refer [listen]]
   [nrepl-ws.modes :as modes]
   [nrepl-ws.nrepl :as nrepl]
   [nrepl-ws.view :as view]
   [reagent.core :as r]
   [reagent.dom :as dom])
  (:import
   [goog.events EventType]))

;; Use defonce to preserve state across hot reloads
(def state (r/atom {:stream nil ;; should probably be a promise
                    }))

(defn fix-fragment [react-fragment]
  (when (vector? react-fragment)
    (if (keyword? (first react-fragment))
      react-fragment
      (vec (cons :div react-fragment)))))

(def config
  ;; TODO docs
  ;; eval-message-fn must be a fn of two args: input, single-form?
  ;; output-fn must be a fn of one arg: eval-result
  {:modes {:repl {:cycle-mode-eval-input "(require '(scicloj.clay.v2 prepare)) (scicloj.clay.v2.prepare/add-preparer! :kind/plotly #'scicloj.clay.v2.item/plotly)"
                  :eval-message-fn nrepl/eval-message
                  :output-fn str
                  :output (r/atom "")}
           :clay-hiccup {:cycle-mode-eval-input "(require '(clay readers item)) (require '(scicloj.clay.v2 prepare)) (scicloj.clay.v2.prepare/add-preparer! :kind/plotly #'clay.item/react-js-plotly)"
                         :eval-message-fn nrepl/eval-message-clay-make-hiccup
                         :output-fn (comp fix-fragment (partial read-string {:readers {'nrepl-ws/plotly readers/plotly}}))
                         :output (r/atom "")}
           :clay {:cycle-mode-eval-input "(require '(scicloj.clay.v2 prepare)) (scicloj.clay.v2.prepare/add-preparer! :kind/plotly #'scicloj.clay.v2.item/plotly)"
                  :eval-message-fn nrepl/eval-message-clay-make}}})

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
  ;; TODO load config, validate it and then merge with state
  ;; config validation rules:
  ;; at least one mode is provided (the default)
  ;; mode required keys: eval-message-fn
  ;; mode optional keys: cycle-mode-eval-input, output, output-fn (note: default mode requires keys output, output-fn)
  (swap! state assoc :mode-index 0)
  (setup-unload-listener)
  (nrepl/connect! state config)
  (dom/render [view/view state config nrepl/eval! (modes/cycle-mode-fn state config nrepl/eval!)]
              (.getElementById js/document "main")))