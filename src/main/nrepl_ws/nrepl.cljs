(ns nrepl-ws.nrepl
  (:require
   [clojure.core.async :refer [<! >! chan go]]
   [clojure.walk :refer [keywordize-keys]]
   [goog.string :as gstring]
   [goog.string.format]
   [haslett.client :as ws]
   [haslett.format :as fmt]
   [nrepl-ws.transducers :refer [partition-when]]))

(def eval-op {:op "eval"})

(defn eval-message [code]
  (assoc eval-op :code code))

(defn eval-message-clay [code]
  (eval-message (gstring/format "(require '(scicloj.clay.v2 api)) (scicloj.clay.v2.api/make-hiccup {:single-form (quote %s)})" code)))

(defn eval! [state config {:keys [input-ref input-str]}]
  (go
    (let [input (cond input-ref @input-ref
                      input-str input-str)
          {:keys [eval-message-fn output-fn]} (get-in config [:modes (:mode @state)])
          stream (:stream @state)]
      (if (>! (:out stream) (eval-message-fn input))
        (do
          (js/console.log "sent message")
          (if-let [msgs (<! (:in stream))]
            (do
              (js/console.log "received messages" (pr-str msgs))
              (let [val-msgs (filter #(or (:value %) (:err %)) msgs)
                    val-msg (last val-msgs)
                    error? (contains? val-msg :err)
                    output (get-in config [:modes (if error? :repl (:mode @state)) :output])]
                (reset! output (if error? (str (:err val-msg)) (output-fn (:value val-msg))))))
            (js/console.log "in channel closed!")))
        (js/console.log "out channel closed!")))))

(defn connect! [state config]
  (go
    (when-not (:stream @state)
      (js/console.log "Opening WebSocket connection")
      (let [stream (<! (ws/connect "ws://localhost:7888"
                                   {:format fmt/json
                                    :in (chan 10 (comp (map keywordize-keys) (partition-when #(= ["done"] (:status %)))))}))]
        (js/console.log "Websocket connection opened" (pr-str stream))
        (swap! state assoc :stream stream)
        (eval! state config {:input-str "(require '(clay readers item)) (require '(scicloj.clay.v2 prepare)) (scicloj.clay.v2.prepare/add-preparer! :kind/plotly #'clay.item/react-js-plotly)"})))))

(defn close! [state]
  (when-let [stream (:stream @state)]
    (js/console.log "Closing WebSocket connection")
    (ws/close stream)
    (swap! state assoc :stream nil)))