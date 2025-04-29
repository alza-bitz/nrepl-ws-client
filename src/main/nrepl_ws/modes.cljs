(ns nrepl-ws.modes)

(defn current-mode
  [state config]
  (get (vec (keys (:modes config))) (:mode-index @state)))

(defn next-mode-index
  [mode-index modes]
  (if (= mode-index (dec (count modes)))
    0
    (inc mode-index)))

;; TODO docs
;; eval-fn must be a fn of four args: state, config, input, opts
(defn cycle-mode-fn 
  [state config eval-fn]
  (fn []
    (let [mode-index (:mode-index @state)
          modes (vec (keys (:modes config)))
          new-mode-index (next-mode-index mode-index modes)
          {:keys [cycle-mode-eval-input]} (get-in config [:modes (get modes new-mode-index)])]
      (when cycle-mode-eval-input
        (eval-fn state config cycle-mode-eval-input {:mode :repl}))
      ;; TODO don't swap if eval returned an error
      (js/console.log "Cycling mode from" (pr-str (get modes mode-index)) "to" (pr-str (get modes new-mode-index)))
      (swap! state assoc :mode-index new-mode-index))))