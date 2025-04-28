(ns nrepl-ws.modes-test
  (:require [cljs.test :refer [deftest is testing]]
            [nrepl-ws.modes :refer [current-mode next-mode-index]]))

(deftest current-mode-test
  (testing "state in default mode"
    (let [state (atom {:mode-index 0})
          config {:modes {:default-mode {}
                          :other-mode {}}}] 
      (is (= :default-mode (current-mode state config)))))
  (testing "state in other mode"
    (let [state (atom {:mode-index 1})
          config {:modes {:default-mode {}
                          :other-mode {}}}]
      (is (= :other-mode (current-mode state config))))))

(deftest next-mode-test
  (testing "next mode"
    (let [modes [:mode-1 :mode-2 :mode-3]]
      (is (= 1 (next-mode-index 0 modes)))
      (is (= 2 (next-mode-index 1 modes)))))
  (testing "next mode wrap"
    (let [modes [:mode-1 :mode-2 :mode-3]]
      (is (= 0 (next-mode-index 2 modes))))))