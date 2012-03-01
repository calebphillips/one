(ns one.sample.api
  "The server side of the sample application. Provides a simple API for
  updating an in-memory database."
  (:use [compojure.core :only (defroutes POST GET)]))

(defonce ^:private next-id (atom 0))

(defonce ^:dynamic *database* (atom #{}))

(defonce ^:dynamic *task-list* (atom []))

(defmulti remote
  "Multimethod to handle incoming API calls. Implementations are
  selected based on the :fn key in the data sent by the client.
  Implementation are called with whatever data struture the client
  sends (which will already have been read into a Clojure value) and
  can return any Clojure value. The value the implementation returns
  will be serialized to a string before being sent back to the client."
  :fn)

(defmethod remote :default [data]
  {:status :error :message "Unknown endpoint."})

(defmethod remote :add-name [data]
  (let [n (-> data :args :name)
        response {:exists (contains? @*database* n)}]
    (swap! *database* conj n)
    response))

(defmethod remote :add-task [data]
  (let [t (-> data :args :task)]
    (swap! *task-list* conj t)
    {}))

(defmethod remote :list-tasks [data]
  {:task-list @*task-list*})

(defroutes remote-routes
  (POST "/remote" {{data "data"} :params}
        (pr-str
         (remote
          (binding [*read-eval* false]
            (read-string data)))))
  (GET "/remote" {{data "data"} :params}
       (pr-str
        (remote
         (binding [*read-eval* false]
           (read-string data))))))
