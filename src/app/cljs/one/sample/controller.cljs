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
  supports only `:init` action.

  Actions to be added in the future  might include :list, :log-in and
  :preferences

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
  (r-get :list-tasks {} #(reset! task-list {:state :loaded :list (:task-list %)})))

(defmethod action :add-task [{task :task}]
  (r-post :add-task {:task task}
          #(swap! task-list
                  (fn [old-state]
                    (update-in old-state [:list] conj %)))))

;; Not working - events are getting sent to this incorrectly, I believe.
(defmethod action :update-task [{:keys [old new]}]
  (r-post :update-task {:task new}
          #(swap! task-list
                  (fn [old-state]
                    (update-in old-state
                               [:list]
                               (fn [ls] (replace {old %} ls)))))))

(dispatch/react-to #{:init :add-task :update-task}
                   (fn [t d] (action (assoc d :type t))))
