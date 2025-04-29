(ns nrepl-ws.modes-test
  (:require [cljs.test :refer [deftest is testing]]
            [nrepl-ws.modes :as modes]))

(deftest current-mode-test
  (testing "state in default mode"
    (let [state (atom {:mode-index 0})
          config {:modes {:default-mode {}
                          :other-mode {}}}] 
      (is (= :default-mode (modes/current-mode state config)))))
  (testing "state in other mode"
    (let [state (atom {:mode-index 1})
          config {:modes {:default-mode {}
                          :other-mode {}}}]
      (is (= :other-mode (modes/current-mode state config))))))

(deftest default-mode-test
  (testing "default mode"
    (let [config {:modes {:default-mode {}
                          :other-mode {}}}]
      (is (= :default-mode (modes/default-mode config))))))

(deftest next-mode-test
  (testing "next mode"
    (let [modes [:mode-1 :mode-2 :mode-3]]
      (is (= 1 (modes/next-mode-index 0 modes)))
      (is (= 2 (modes/next-mode-index 1 modes)))))
  (testing "next mode wrap"
    (let [modes [:mode-1 :mode-2 :mode-3]]
      (is (= 0 (modes/next-mode-index 2 modes))))))

(deftest cycle-mode-test
  (testing "cycle mode"
    (let [state (atom {:mode-index 0})
          config {:modes {:mode-1 {}
                          :mode-2 {}
                          :mode-3 {}}}
          eval-fn nil
          toggle-mode-fn (modes/cycle-mode-fn state config eval-fn)]
      (toggle-mode-fn)
      (is (= 1 (:mode-index @state)))
      (toggle-mode-fn)
      (is (= 2 (:mode-index @state)))))
  (testing "cycle mode wrap"
    (let [state (atom {:mode-index 2})
          config {:modes {:mode-1 {}
                          :mode-2 {}
                          :mode-3 {}}}
          eval-fn nil
          toggle-mode-fn (modes/cycle-mode-fn state config eval-fn)]
      (toggle-mode-fn)
      (is (= 0 (:mode-index @state))))))