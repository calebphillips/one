(ns one.sample.test.model
  (:use [clojure.test]
        [one.sample.api :only (*database*)]
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

(deftest test-set-editing
  (cljs-eval one.sample.model (reset! greeting-form {}))
  (is (= {:fields {"name-input" {:status :editing, :value nil}}, :status :finished}
         (cljs-eval one.sample.model
                    (set-editing "name-input")
                    (deref greeting-form)))))

(deftest test-set-field-value

  )