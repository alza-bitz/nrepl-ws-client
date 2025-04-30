(ns nrepl-ws.nrepl-test
  (:require
   [cljs.test :refer [deftest is testing]]
   [clojure.core.async :refer [<! >! chan go go-loop timeout]]
   [nrepl-ws.nrepl :as nrepl]))

(deftest eval-test
  (testing "eval-with-eval-message"
    (let [state (atom {:mode-index 0
                       :stream {:in (chan)
                                :out (chan)}})
          output (atom "")
          config {:modes {:mode-1 {:eval-message-fn nrepl/eval-message
                                   :output-fn str
                                   :output output}}}]
      (go-loop []
        (when-let [msg (<! (get-in @state [:stream :out]))]
          (is (= msg (nrepl/eval-message "input-to-eval" false)))
          (>! (get-in @state [:stream :in]) [{:value "output-from-eval"}])
          (recur)))
      (nrepl/eval! state config "input-to-eval")
      (go
        (<! (timeout 100))
        (is (= "output-from-eval" @output)))))

  (testing "eval-with-eval-message-and-error"
    (let [state (atom {:mode-index 0
                       :stream {:in (chan)
                                :out (chan)}})
          output (atom "")
          config {:modes {:mode-1 {:eval-message-fn nrepl/eval-message
                                   :output-fn str
                                   :output output}}}]
      (go-loop []
        (when-let [msg (<! (get-in @state [:stream :out]))]
          (is (= msg (nrepl/eval-message "input-to-eval" false)))
          (>! (get-in @state [:stream :in]) [{:err "error-from-eval"}])
          (recur)))
      (nrepl/eval! state config "input-to-eval")
      (go
        (<! (timeout 100))
        (is (= "error-from-eval" @output))))))