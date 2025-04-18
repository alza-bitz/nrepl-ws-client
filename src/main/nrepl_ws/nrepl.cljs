(ns nrepl-ws.nrepl
  (:require
   [clojure.core.async :refer [<! >! chan go]]
   [clojure.string :as str]
   [clojure.walk :refer [keywordize-keys]]
   [goog.string :as gstring]
   [goog.string.format]
   [haslett.client :as ws]
   [haslett.format :as fmt]
   [nrepl-ws.transducers :refer [partition-when]]))

(def eval-op {:op "eval"})

(defn eval-message [code _]
  (js/console.log "eval-message" code)
  (assoc eval-op :code code))

(defn eval-message-clay-make-hiccup [code single-form?]
  (js/console.log "eval-message-clay-make-hiccup" code single-form?)
  (if single-form?
    (eval-message (gstring/format "(require '(scicloj.clay.v2 api)) (scicloj.clay.v2.api/make-hiccup {:single-form (quote %s)})" code) single-form?)
    (let [code-escaped (str/replace code "\"" "\\\"")] 
      (eval-message (gstring/format 
                     "(spit \"/tmp/nrepl_ws_client.clj\" \"%s\") (require '(scicloj.clay.v2 api)) (scicloj.clay.v2.api/make-hiccup {:source-path \"/tmp/nrepl_ws_client.clj\"})" 
                     code-escaped) single-form?))))

(defn eval-message-clay-make [code single-form?]
  (js/console.log "eval-message-clay-make" code single-form?)
  (if single-form?
    (eval-message (gstring/format "(require '(scicloj.clay.v2 api)) (scicloj.clay.v2.api/make! {:single-form (quote %s)})" code) single-form?)
    (let [code-escaped (str/replace code "\"" "\\\"")]
     (eval-message (gstring/format 
                    "(spit \"/tmp/nrepl_ws_client.clj\" \"%s\") (require '(scicloj.clay.v2 api)) (scicloj.clay.v2.api/make! {:source-path \"/tmp/nrepl_ws_client.clj\"})" 
                    code-escaped) single-form?))))

(defn eval!
  ([state config input-opts]
   (eval! state config input-opts (get (vec (keys (:modes config))) (:mode @state))))
  ([state config {:keys [input-ref input-str]} current-mode]
   (js/console.log "eval! state:" (pr-str @state) "current mode:" (pr-str current-mode))
   (let [stream (:stream @state)
         [input single-form?] (cond input-ref [@input-ref false]
                                    input-str [input-str true])
         {:keys [eval-message-fn]} (get-in config [:modes current-mode])
         message (eval-message-fn input single-form?)]
     (go
       (if (>! (:out stream) message)
         (do
           (js/console.log "sent message: " (pr-str message))
           (if-let [msgs (<! (:in stream))]
             (do
               (js/console.log "received messages" (pr-str msgs))
               (let [val-msgs (filter #(or (:value %) (:err %)) msgs)
                     val-msg (last val-msgs)
                     error? (contains? val-msg :err)
                     output-mode (cond error? :repl
                                      ;;  TODO change condition to "output or output-fn missing?"
                                       (= :iframe current-mode) :repl
                                       :else current-mode)
                     {:keys [output output-fn]} (get-in config [:modes output-mode])]
                 (when (and output output-fn)
                   (reset! output (if error? (output-fn (:err val-msg)) (output-fn (:value val-msg)))))))
             (js/console.log "in channel closed!")))
         (js/console.log "out channel closed!"))))))

(defn connect! [state config]
  (go
    (when-not (:stream @state)
      (js/console.log "Opening WebSocket connection")
      (let [stream (<! (ws/connect "ws://localhost:7888"
                                   {:format fmt/json
                                    :in (chan 10 (comp (map keywordize-keys) (partition-when #(= ["done"] (:status %)))))}))]
        (js/console.log "Websocket connection opened" (pr-str stream))
        (swap! state assoc :stream stream)))))

(defn close! [state]
  (when-let [stream (:stream @state)]
    (js/console.log "Closing WebSocket connection")
    (ws/close stream)
    (swap! state assoc :stream nil)))