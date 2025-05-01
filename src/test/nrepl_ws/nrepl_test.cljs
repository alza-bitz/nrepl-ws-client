(ns nrepl-ws.nrepl-test
  (:require
   [cljs.test :refer [async deftest is]]
   [clojure.core.async :refer [<! >! chan go go-loop timeout]]
   [nrepl-ws.nrepl :as nrepl]))

(deftest eval-test-with-eval-message
  (async done
         (let [state (atom {:mode-index 0
                            :stream {:in (chan)
                                     :out (chan)}})
               output (atom "")
               config {:modes {:mode-0 {:eval-message-fn nrepl/eval-message
                                        :output-fn str
                                        :output output}}}]
           (go-loop []
             (when-let [msg (<! (get-in @state [:stream :out]))]
               (is (= msg (nrepl/eval-message "input-to-eval" false)))
               (>! (get-in @state [:stream :in]) [{:value "output-from-eval"}])
               (recur)))
           (nrepl/eval! state config "input-to-eval")
           (go
             (<! (timeout 1000))
             (is (= "output-from-eval" @output))
             (done)))))

(deftest eval-with-eval-message-and-opts-single-form
  (async done 
         (let [state (atom {:mode-index 0
                            :stream {:in (chan)
                                     :out (chan)}})
               output (atom "")
               config {:modes {:mode-0 {:eval-message-fn nrepl/eval-message
                                        :output-fn str
                                        :output output}}}]
           (go-loop []
             (when-let [msg (<! (get-in @state [:stream :out]))]
               (is (= msg (nrepl/eval-message "input-to-eval" false)))
               (>! (get-in @state [:stream :in]) [{:value "output-from-eval"}])
               (recur)))
           (nrepl/eval! state config "input-to-eval" {:single-form? true})
           (go
             (<! (timeout 1000))
             (is (= "output-from-eval" @output))
             (done)))))

(deftest eval-with-eval-message-and-opts-mode
  (async done
         (let [state (atom {:mode-index 1
                            :stream {:in (chan)
                                     :out (chan)}})
               output-0 (atom "")
               output-1 (atom "")
               config {:modes {:mode-0 {:eval-message-fn nrepl/eval-message
                                        :output-fn str
                                        :output output-0}
                               :mode-1 {:eval-message-fn nrepl/eval-message
                                        :output-fn str
                                        :output output-1}}}]
           (go-loop []
             (when-let [msg (<! (get-in @state [:stream :out]))]
               (is (= msg (nrepl/eval-message "input-to-eval" false)))
               (>! (get-in @state [:stream :in]) [{:value "output-from-eval"}])
               (recur)))
           (nrepl/eval! state config "input-to-eval" {:mode :mode-0})
           (go
             (<! (timeout 1000))
             (is (= "output-from-eval" @output-0))
             (is (= "" @output-1))
             (done)))))

(deftest eval-with-eval-message-and-error
  (async done 
         (let [state (atom {:mode-index 0
                            :stream {:in (chan)
                                     :out (chan)}})
               output (atom "")
               config {:modes {:mode-0 {:eval-message-fn nrepl/eval-message
                                        :output-fn str
                                        :output output}}}]
           (go-loop []
             (when-let [msg (<! (get-in @state [:stream :out]))]
               (is (= msg (nrepl/eval-message "input-to-eval" false)))
               (>! (get-in @state [:stream :in]) [{:err "error-from-eval"}])
               (recur)))
           (nrepl/eval! state config "input-to-eval")
           (go
             (<! (timeout 1000))
             (is (= "error-from-eval" @output))
             (done)))))