(ns ^{:doc "Contains client-side state, validators for input fields
 and functions which react to changes made to the input fields."}
  one.sample.model
  (:require [one.dispatch :as dispatch]))

(def ^{:doc
       "An atom containing a map which is the application's current state.
        The only state is :init right now, but hopefully :log-in and :preferences
        may come later"}
  state (atom {}))

(add-watch state :state-change-key
           (fn [k r o n]
             (dispatch/fire :state-change n)))

(def ^{:private true
       :doc "An atom containing the state of the task form and
             each of its fields."}
  task-form (atom {}))

(add-watch task-form :form-change-key
           (fn [k r o n]
             (dispatch/fire :form-change {:old o :new n})))

(def ^{:doc "An atom containing the list of tasks, kept in sync
             with the list on the server by the controller."}
  task-list (atom {:state :init :list []}))

(defn- find-added-tasks [old new]
  (filter (fn [[id tasks]] (= 1 (count tasks)))
          (group-by :id (concat old new))))

(defn- find-toggled-tasks [old new]
  (filter (fn [[id tasks]]
            (not (apply = (map :complete tasks))))
          (group-by :id (concat old new))))

(defn- task-toggled [[id [old new]]]
  [:task-toggled {:id id :complete (:complete new)}])

(defn- task-added [[id [task]]]
  [:task-added task])

(defn tl-change->events
  "Returns a vector of tuples of events (with accompanying data) that should
   be fired based on the type of change(s) that were made to the task list.

   For example: [[:task-added {:id 123 :description \"Do this\" :complete false}]]"
  [old new]
  (let [load? (= [:init :loaded] (map :state [old new]))
        added (find-added-tasks (:list old) (:list new))
        toggled (find-toggled-tasks (:list old) (:list new))]
    (cond
     load? [[:tasks-loaded (:list new)]]
     (seq added) (map task-added added)
     (seq toggled) (map task-toggled toggled)
     :else [])))

(add-watch task-list :task-list-change-key
           (fn [k r o n]
             (doseq [[evt-id data] (tl-change->events o n)]
               (dispatch/fire evt-id data))))

(defmulti ^:private new-status
  "Determine the new :status of a form input field based on:
    1) its preivous status
    2) what just happended to it (value was changed, got focus, lost focus...)
    3) The current state of its value (empty, error, valid)"
  (fn [& args] (vec args)))

(defmethod new-status [:empty :focus :empty] [p e f]
  {:status :editing})

(defmethod new-status [:editing :finished :empty] [p e f]
  {:status :empty})

(defmethod new-status [:editing :change :empty] [p e f]
  {:status :editing})

(defmethod new-status [:editing :change :error] [p e f]
  {:status :editing})

(defmethod new-status [:editing :change :valid] [p e f]
  {:status :editing-valid})

(defmethod new-status [:editing :finished :error] [p e f]
  {:status :error})

(defmethod new-status [:editing-valid :change :error] [p e f]
  {:status :editing})

(defmethod new-status [:editing-valid :change :valid] [p e f]
  {:status :editing-valid})

(defmethod new-status [:editing-valid :change :empty] [p e f]
  {:status :editing})

(defmethod new-status [:editing-valid :finished :valid] [p e f]
  {:status :valid})

(defmethod new-status [:error :focus :error] [p e f]
  {:status :editing-error})

(defmethod new-status [:editing-error :change :error] [p e f]
  {:status :editing-error})

(defmethod new-status [:editing-error :finished :error] [p e f]
  {:status :error})

(defmethod new-status [:editing-error :change :valid] [p e f]
  {:status :editing-valid})

(defmethod new-status [:editing-error :change :empty] [p e f]
  {:status :editing-error})

(defmethod new-status [:editing-error :finished :empty] [p e f]
  {:status :empty})

(defmethod new-status [:valid :focus :valid] [p e f]
  {:status :editing-valid})

(defmethod new-status [:valid :finished :empty] [p e f]
  {:status :empty})

(defmethod new-status :default [p e f]
  {:status p})

(defmulti ^:private validate
  "Accepts a form id and a value and returns a map
  with `:value`, `:status`, and `:error` keys. Status will be set to
  either `:valid` or `:error`. If there was an error, then there will be
  an error message associated with the `:error` key."
  (fn [id _] id))

(defmethod validate "task-input" [_ v]
  (cond (= (count v) 0) :empty
        (= (count v) 1) :error
        :else :valid))

(defn- form-status
  "Calculates the status of the whole form based on the status of each
  field. Retuns `:finished` or `:editing`."
  [m]
  (if (every? #(or (= % :valid) (= % :editing-valid)) (map :status (vals (:fields m))))
    :finished
    :editing))

(defn- set-field-value
  "Accepts a field-id and value. Validates the field and updates the
  greeting form atom."
  [field-id type value]
  (swap! task-form
         (fn [old]
           (let [field-status (assoc (new-status (-> old :fields field-id :status)
                                                 type
                                                 (validate field-id value))
                                :value value)
                 new (assoc-in old [:fields field-id] field-status)]
             (assoc new :status (form-status new))))))

(defn- set-editing
  "Update the form state for a given field to indicate that the form
  is still being edited."
  [id]
  (swap! task-form
         (fn [old]
           (let [field-map (-> old :fields id)
                 status (or (:status field-map) :empty)
                 field-status (new-status status
                                          :focus
                                          status)]
             (-> old
                 (assoc-in [:fields id] (assoc field-status :value (:value field-map)))
                 (assoc :status (form-status old)))))))

(dispatch/react-to (fn [e] (= (first e) :field-finished))
                   (fn [[_ id] value]
                     (set-field-value id :finished value)))

(dispatch/react-to (fn [e] (= (first e) :field-changed))
                   (fn [[_ id] value]
                     (set-field-value id :change value)))

(dispatch/react-to (fn [e] (= (first e) :editing-field))
                   (fn [[_ id] _]
                     (set-editing id)))

(defn- new-task [d]
  {:description d :complete false})

(defn- toggle-complete [t]
  (assoc t :complete (not (:complete t))))

(dispatch/react-to #{:form-submit}
                   (fn [t d]
                     (let [form-data @task-form]
                       (when (= (:status form-data) :finished)
                         (dispatch/fire :add-task
                                        {:task (-> form-data
                                                   :fields
                                                   "task-input"
                                                   :value
                                                   new-task)})))))

(defn- find-task-by-id [id]
  (first (filter #(= (:id %) id) (:list @task-list))))

(dispatch/react-to #{:task-clicked}
                   (fn [_ id]
                     (let [t (find-task-by-id id)]
                       (dispatch/fire :update-task
                                      {:old t :new (toggle-complete t)}))))
