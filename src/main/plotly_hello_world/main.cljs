(ns plotly-hello-world.main
  (:require
   [cljs.reader :refer [read-string]]
   [reagent.core :as r]
   [reagent.dom :as dom]
   [reagent.dom.client :as dom-client]
   ["react-plotly.js" :as react-plotly :default Plot]
   [nrepl-ws.main :as nrepl-ws]
   [clay.readers :as readers]))

(def plotly-getting-started-cljs
  [:> Plot
   {:data [{:x [1 2 3 4 5], :y [1 2 4 8 16]}]
    :layout {:margin {:t 0}}}])

(def plotly-getting-started-from-clay
  [:div ;; added by fix-fragment
   [:script {:type "text/javascript", :src ".clay_files/plotly0.js"}]
   [:div {:style {:height "400px", :width "100%"}}
    [:script "Plotly.newPlot(document.currentScript.parentElement, [{x: [1, 2, 3, 4, 5], y: [1, 2, 4, 8, 16] }], {margin: { t: 0 } } );"]]])

(def plotly-getting-started-from-clay-noscript-idea
  [:div ;; added by fix-fragment
   [:div
    {:style {:height "400px", :width "100%"}}
    [:> Plot
     {:data [{:x [1 2 3 4 5], :y [1 2 4 8 16]}]
      :layout {:margin {:t 0}}}]]])

;; me attempting to create an instance of PlotlyComponent with the supplied data (doesn't work)
(comment
  (Plot. {:data [{:x [1 2 3 4 5], :y [1 2 4 8 16]}]
          :layout {:margin {:t 0}}}))

;; WTF is Plot..
(comment
  (type Plot))

;; me attempting to create an instance of PlotlyComponent with the supplied data (works but don't understand why)
(comment
  [:> Plot
   {:data [{:x [1 2 3 4 5], :y [1 2 4 8 16]}]
    :layout {:margin {:t 0}}}])

(def plotly-getting-started-from-clay-noscript-actual
  (let [msg-value "[[:div {:style {:height \"400px\", :width \"100%\"}} [:> Plot {:data [{:x [1 2 3 4 5], :y [1 2 4 8 16]}] :layout {:margin {:t 0}}}]]]"]
    ((comp nrepl-ws/fix-fragment read-string) msg-value)))

(comment
  (read-string "[:> Plot {:data [{:x [1 2 3 4 5], :y [1 2 4 8 16]}] :layout {:margin {:t 0}}}]"))

(comment
  (read-string {:default tagged-literal}
               "#nrepl-ws/plotly PlotlyComponent"))

(comment
  (read-string {:readers {'nrepl-ws/plotly readers/plotly}}
               "#nrepl-ws/plotly PlotlyComponent"))

(comment
  (read-string
   {:readers {'nrepl-ws/plotly readers/plotly}}
   "[:> #nrepl-ws/plotly PlotlyComponent {:data [{:x [1 2 3 4 5], :y [1 2 4 8 16]}] :layout {:margin {:t 0}}}]"))

(def plotly-getting-started-from-clay-noscript-actual-using-readers 
  (let [output-fn (comp nrepl-ws/fix-fragment (partial read-string {:readers {'nrepl-ws/plotly readers/plotly}}))]
   (output-fn 
   "[[:div {:style {:height \"400px\", :width \"100%\"}} [:> #nrepl-ws/plotly PlotlyComponent {:data [{:x [1 2 3 4 5], :y [1 2 4 8 16]}] :layout {:margin {:t 0}}}]]]")))

(comment
  (type (read-string "#inst \"2018-03-28T10:48:00.000\"")))

(defn view []
  (fn []
    [:div
     [:h1 "Plotly Hello World"]
     [:div {:id "component"} plotly-getting-started-from-clay-noscript-actual-using-readers]]))

(defn ^:dev/after-load start []
  (js/console.log "Starting/restarting application")
  (dom/render [view] (.getElementById js/document "main")))