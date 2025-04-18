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
                    }))

(defn fix-fragment [react-fragment]
  (when (vector? react-fragment)
    (if (keyword? (first react-fragment))
      react-fragment
      (vec (cons :div react-fragment)))))

;; :plotly #'scicloj.clay.v2.item/plotly

(def config
  ;; TODO docs
  ;; eval-message-fn must be a fn of two args
  ;; output-fn must be a fn of one arg
  {:modes {:repl {:eval-message-fn nrepl/eval-message
                  :toggle-eval-message "(require '(scicloj.clay.v2 prepare)) (scicloj.clay.v2.prepare/add-preparer! :kind/plotly #'scicloj.clay.v2.item/plotly)"
                  :output-fn str
                  :output (r/atom "")}
           :kindly {:eval-message-fn nrepl/eval-message-clay-make-hiccup
                    :toggle-eval-message "(require '(clay readers item)) (require '(scicloj.clay.v2 prepare)) (scicloj.clay.v2.prepare/add-preparer! :kind/plotly #'clay.item/react-js-plotly)"
                    :output-fn (comp fix-fragment (partial read-string {:readers {'nrepl-ws/plotly readers/plotly}}))
                    :output (r/atom "")}
           :iframe {:eval-message-fn nrepl/eval-message-clay-make
                    :toggle-eval-message "(require '(scicloj.clay.v2 prepare)) (scicloj.clay.v2.prepare/add-preparer! :kind/plotly #'scicloj.clay.v2.item/plotly)"}}})

(defn next-mode 
  [n modes]
  (if (= n (dec (count modes)))
    0
    (inc n)))

(defn toggle-mode-fn [state config]
  (fn []
    (let [current-mode (:mode @state)
          modes (vec (keys (:modes config)))
          new-mode (next-mode current-mode modes)
          {:keys [toggle-eval-message]} (get-in config [:modes (get modes new-mode)])]
      (when toggle-eval-message
        (nrepl/eval! state config {:input-str toggle-eval-message} :repl))
      ;; TODO don't swap if eval returned an error
      (js/console.log "Switching mode from" (pr-str (get modes current-mode)) "to" (pr-str (get modes new-mode))) 
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
    ;; TODO load config and merge with state
  (swap! state assoc :mode 0)
  (setup-unload-listener)
  (nrepl/connect! state config)
  (dom/render [view/view state config nrepl/eval! (toggle-mode-fn state config)]
              (.getElementById js/document "main")))