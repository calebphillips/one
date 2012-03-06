(ns ^{:doc "Respond to user actions by updating local and remote
  application state."}
  one.sample.controller
  (:use [one.browser.remote :only (request)]
        [one.sample.model :only (state task-list)])
  (:require [cljs.reader :as reader]
            [clojure.browser.event :as event]
            [one.dispatch :as dispatch]
            [goog.uri.utils :as uri]))

(defmulti action
  "Accepts a map containing information about an action to perform.

  Actions may cause state changes on the client or the server. This
  function dispatches on the value of the `:type` key and currently
  supports `:init`, `:form`, and `:greeting` actions.

  The `:init` action will initialize the appliation's state. "
  :type)

(defn host
  "Get the name of the host which served this script."
  []
  (uri/getHost (.toString window.location ())))

(defn remote
  [method f data on-success]
  (let [data-str (str "data=" (pr-str {:fn f :args data}))
        query-str (when (= "GET" method) (str "?" data-str))
        post-data (when (= "POST" method) data-str)]
    (request f (str (host) "/remote" query-str)
             :method method
             :on-success #(on-success (reader/read-string (:body %)))
             :on-error #(swap! state assoc :error "Error communicating with server.")
             :content post-data)))

(def r-post (partial remote "POST"))
(def r-get  (partial remote "GET"))

(defmethod action :init [_]
  (reset! state {:state :init})
  (r-get :list-tasks {} #(reset! task-list (:task-list %))))

(defmethod action :add-task [{task :task}]
  (r-post :add-task {:task task} #(swap! task-list conj %)))

(dispatch/react-to #{:init :add-task}
                   (fn [t d] (action (assoc d :type t))))
