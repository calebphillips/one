(ns one.sample.test.model
  (:use [clojure.test]
        [one.test :only (cljs-eval)]
        [clojure.java.browse :only (browse-url)]
        [cljs.repl :only (-setup -tear-down)]
        [cljs.repl.browser :only (repl-env)]
        [one.test :only (*eval-env*)]
        [one.sample.dev-server :only (run-server)]))

;; Would like to be able to run these against rhino instead of browser
(defn setup
  "Start the development server and connect to the browser so that
  ClojureScript code can be evaluated from tests."
  [f]
  (let [server (run-server)
        eval-env (repl-env)]
    (-setup eval-env)
    (browse-url "http://localhost:8080/development")
    (binding [*eval-env* eval-env]
      (f))
    (-tear-down eval-env)
    (.stop server)))

(use-fixtures :once setup)


(deftest test-tl-change->events

  (is (= [[:tasks-loaded []]]
         (cljs-eval one.sample.model
                    (tl-change->events {:state :init :list []}
                                       {:state :loaded :list []}))))
  (is (= [[:tasks-loaded [{:id 1 :description "A"} {:id 2 :description "B"}]]]
         (cljs-eval one.sample.model
                    (tl-change->events {:state :init :list []}
                                       {:state :loaded
                                        :list [{:id 1 :description "A"} {:id 2 :description "B"}]}))))
  (is (= [[:task-added {:id 1 :description "Do this"}]]
         (cljs-eval one.sample.model
                    (tl-change->events {:state :loaded :list []}
                                       {:state :loaded :list [{:id 1 :description "Do this"}]}))))
  (is (= [[:task-toggled {:id 1 :complete true}]]
         (cljs-eval one.sample.model
                    (tl-change->events {:state :loaded :list [{:id 1 :complete false}]}
                                       {:state :loaded :list [{:id 1 :complete true}]}))))
  (is (= []
         (cljs-eval one.sample.model
                    (tl-change->events {:state :loaded :list []}
                                       {:state :loaded :list []})))))