(ns clay.readers
  (:require
   #?(:cljs ["react-plotly.js" :default Plot])
   #?(:clj [clojure.core :refer [print-method]])))

(deftype Plotly []
  Object
  (toString [_]
    (str "#" 'nrepl-ws/plotly " PlotlyComponent")))

#?(:clj
   (defmethod print-method Plotly
     [v w]
     (.write w (str v))))

(defn plotly [_]
  #?(:cljs Plot
     :clj (Plotly.)))