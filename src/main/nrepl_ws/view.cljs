(ns nrepl-ws.view
  (:require
   [applied-science.js-interop :as j]
   [nextjournal.clojure-mode :as cm-clj]
   [nextjournal.clojure-mode.extensions.eval-region :as eval-region]
   ["@codemirror/commands" :as commands]
   ["@codemirror/language" :as language]
   ["@codemirror/state" :as state]
   ["@codemirror/view" :as view]
   [reagent.core :as r]))

(j/defn eval-at-cursor [eval-fn ^:js {:keys [state]}]
  (some->> (eval-region/cursor-node-string state)
           eval-fn)
  true)

(j/defn eval-top-level [eval-fn ^:js {:keys [state]}]
  (some->> (eval-region/top-level-string state)
           eval-fn)
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
    [:div.input-comp
     {:ref editor-mount
      :class "component"}]
    (finally
      (when @editor-view
        (.destroy @editor-view)))))

(defn button [eval-fn]
  [:button
   {:on-click eval-fn}
   "Eval"])

(defn mode-toggle-button [state mode-toggle-fn]
  [:button
   {:on-click mode-toggle-fn}
   (str "Switch to " (if (= (:mode @state) :repl) "Clay" "Repl"))])

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
    [:div.output-comp
     {:ref editor-mount
      :class "component"}]
    (finally
      ;; Remove the watch when component unmounts
      (remove-watch output :editor-updater)
      (when @editor-view
        (.destroy @editor-view)))))

(defn view [state config eval-fn mode-toggle-fn]
  (let [input (r/atom "(+ 1 2 3)")
        output (get-in config [:modes :repl :output])
        kindly-output (get-in config [:modes :kindly :output])]
    (fn []
      [:div
       [:h2 "nREPL Websocket Client"]
       [:div {:class "container"}
        [:div {:class "row"}
         [:h3 "Repl"]
         [input-comp input (fn [input-str]
                             (eval-fn state config {:input-str input-str}))]
         [:div
          [button (fn [_] (eval-fn state config {:input-ref input}))]
          [mode-toggle-button state mode-toggle-fn]]
         [output-comp output]]
        [:div {:class "row"}
         [:h3 "Clay"]
         [:div {:class "component"
                :style {:height "490px"}} @kindly-output]]]])))