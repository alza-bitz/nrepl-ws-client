(ns nrepl-ws.view
  (:require
   ["@codemirror/commands" :as commands]
   ["@codemirror/language" :as language]
   ["@codemirror/state" :as state]
   ["@codemirror/view" :as view]
   [applied-science.js-interop :as j]
   [nextjournal.clojure-mode :as cm-clj]
   [nextjournal.clojure-mode.extensions.eval-region :as eval-region]
   [nrepl-ws.modes :as modes]
   [reagent.core :as r]))

(j/defn eval-at-cursor [eval-fn ^:js {:keys [state]}]
  (some-> (eval-region/cursor-node-string state)
          eval-fn {:single-form? true})
  true)

(j/defn eval-top-level [eval-fn ^:js {:keys [state]}]
  (some-> (eval-region/top-level-string state)
          eval-fn {:single-form? true})
  true)

(j/defn eval-cell [eval-fn ^:js {:keys [state]}]
  (-> (.-doc state)
      (str)
      eval-fn)
  true)

(defn eval-extension [eval-fn {:keys [modifier]}]
  (.of view/keymap
       (j/lit
        [{:key "Alt-Enter"
          :run (partial eval-cell eval-fn)}
         {:key (str modifier "-Enter")
          :shift (partial eval-top-level eval-fn)
          :run (partial eval-at-cursor eval-fn)}])))

(defn input-comp [input eval-fn]
  (r/with-let [editor-view (atom nil)
               editor-mount (fn [node]
                              (when node
                                (let [extensions [(commands/history)
                                                  (language/syntaxHighlighting language/defaultHighlightStyle)
                                                  (view/drawSelection)
                                                  (.. state/EditorState -allowMultipleSelections (of true))
                                                  cm-clj/default-extensions
                                                  (.of view/keymap cm-clj/complete-keymap)
                                                  (.of view/keymap commands/historyKeymap)
                                                  (eval-region/extension {:modifier "Ctrl"})
                                                  (eval-extension eval-fn {:modifier "Ctrl"})
                                                  view/EditorView.lineWrapping
                                                  (.of view/EditorView.updateListener
                                                       (fn [^view/ViewUpdate view-update]
                                                         (when (.-docChanged view-update)
                                                           (reset! input (.toString (.-doc (.-state view-update)))))))]
                                      editor-state (state/EditorState.create
                                                    #js {:doc @input
                                                         :extensions (clj->js extensions)})
                                      editor (view/EditorView. #js {:state editor-state
                                                                    :parent node})]
                                  (reset! editor-view editor))))]
    [:div
     {:ref editor-mount}]
    (finally
      (when @editor-view
        (.destroy @editor-view)))))

(defn eval-button [eval-fn]
  [:button
   {:on-click eval-fn}
   "Eval"])

(defn cycle-mode-button [toggle-mode-fn]
  [:button
   {:on-click toggle-mode-fn}
   "Cycle Mode"])

(defn output-comp [output]
  (r/with-let [editor-view (atom nil)
               editor-mount (fn [node]
                              (when node
                                (let [extensions [(language/syntaxHighlighting language/defaultHighlightStyle)
                                                  cm-clj/default-extensions
                                                  (.. state/EditorState -readOnly (of true))
                                                  view/EditorView.lineWrapping]
                                      editor-state (state/EditorState.create
                                                    #js {:doc @output
                                                         :extensions (clj->js extensions)})
                                      editor (view/EditorView. #js {:state editor-state
                                                                    :parent node})]

                                  (reset! editor-view editor))))

               ;; Add a watch to update editor content when output changes
               _ (add-watch output :editor-updater
                            (fn [_ _ _ new-value]
                              (when @editor-view
                                (let [current-state (.-state @editor-view)
                                      transaction (.update current-state
                                                           #js {:changes
                                                                #js {:from 0
                                                                     :to (.-length (.-doc current-state))
                                                                     :insert new-value}})]
                                  (.dispatch @editor-view transaction)))))]
    [:div
     {:ref editor-mount}]
    (finally
      ;; Remove the watch when component unmounts
      (remove-watch output :editor-updater)
      (when @editor-view
        (.destroy @editor-view)))))

;; TODO docs
;; eval-fn must be a fn of four args: state, config, input, opts
;; cycle-mode-fn must be a fn of zero args
(defn view [state config eval-fn cycle-mode-fn]
  (let [input (r/atom "(+ 1 2 3)")
        repl-output (get-in config [:modes :repl :output])
        clay-hiccup-output (get-in config [:modes :clay-hiccup :output])]
    (fn []
      (let [mode (modes/current-mode state config)]
        [:div
         [:h3 "nREPL Websocket Client"]
         [:div {:style {:margin "10px"}}
          "[alt + enter]: eval all. [ctrl + enter]: eval form at cursor. [ctrl + shift + enter]: eval top-level form at cursor."]
         [:div {:class "container"}
          [:div {:class "row"}
           [:h4 "Editor"]
           [:div {:class "component"}
            [input-comp input (partial eval-fn state config)]]]
          [:div {:class "row"}
           [:h4 "Result"]
           [:div
            [:div {:class "component"
                   :hidden (not= mode :repl)}
             [output-comp repl-output]]
            [:div {:class "component"
                   :hidden (not= mode :clay-hiccup)}
             @clay-hiccup-output]
            [:div {:class "component"
                   :style {:overflow "hidden"}
                   :hidden (not= mode :clay)}
             [:iframe {:src "http://localhost:7890"
                       :style {:border "none"
                               :width "100%"
                               :height "100%"}}]]]]]
         [:div
          [eval-button (fn [] (eval-fn state config @input))]
          [cycle-mode-button cycle-mode-fn]
          "current mode: " mode]]))))