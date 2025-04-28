(ns nrepl-ws.modes)

(defn current-mode
  [state config]
  (get (vec (keys (:modes config))) (:mode-index @state)))

(defn next-mode-index
  [mode-index modes]
  (if (= mode-index (dec (count modes)))
    0
    (inc mode-index)))

(defn toggle-mode-fn 
  [state config eval-fn]
  (fn []
    (let [mode-index (:mode-index @state)
          modes (vec (keys (:modes config)))
          new-mode-index (next-mode-index mode-index modes)
          {:keys [toggle-eval-message]} (get-in config [:modes (get modes new-mode-index)])]
      (when toggle-eval-message
        (eval-fn state config toggle-eval-message {:mode :repl}))
      ;; TODO don't swap if eval returned an error
      (js/console.log "Switching mode from" (pr-str (get modes mode-index)) "to" (pr-str (get modes new-mode-index)))
      (swap! state assoc :mode-index new-mode-index))))