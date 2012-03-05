(ns ^{:doc "Render the views for the application."}
  one.sample.view
  (:use [domina :only (set-html! set-styles! styles by-id set-style!
                                 by-class value set-value! set-text! nodes single-node
                                 html append!)]
        [domina.xpath :only (xpath)]
        [one.browser.animation :only (play)])
  (:require-macros [one.sample.snippets :as snippets])
  (:require [goog.events.KeyCodes :as key-codes]
            [goog.events.KeyHandler :as key-handler]
            [clojure.browser.event :as event]
            [one.dispatch :as dispatch]
            [one.sample.animation :as fx]))

(def ^{:doc "A map which contains chunks of HTML which may be used
  when rendering views."}
  snippets (snippets/snippets))

(defmulti render-button
  "Render the submit button based on the current state of the
  form. The button is disabled while the user is editing the form and
  becomes enabled when the form is complete."
  identity)

(defmethod render-button :default [_])

(defmethod render-button [:finished :editing] [_]
  (fx/disable-button "task-button"))

(defmethod render-button [:editing :finished] [_]
  (fx/enable-button "task-button"))

(defn- add-input-event-listeners
  "Accepts a field-id and creates listeners for blur and focus events which will then fire
  `:field-changed` and `:editing-field` events."
  [field-id]
  (let [field (by-id field-id)
        keyboard (goog.events.KeyHandler. (by-id "task-form"))]
    (event/listen field
                  "blur"
                  #(dispatch/fire [:field-finished field-id] (value field)))
    (event/listen field
                  "focus"
                  #(dispatch/fire [:editing-field field-id]))
    (event/listen field
                  "keyup"
                  #(dispatch/fire [:field-changed field-id] (value field)))
    (event/listen keyboard
                  "key"
                  (fn [e] (when (= (.-keyCode e) key-codes/ENTER)
                           (do (.blur (by-id "task-input") ())
                               (dispatch/fire :form-submit)))))))

(defmulti render
  "Accepts a map which represents the current state of the application
  and renders a view based on the value of the `:state` key."
  :state)

(defmethod render :init [_]
  (fx/initialize-task-views (:tasks snippets))
  (add-input-event-listeners "task-input")
  (event/listen (by-id "task-button")
                "click"
                #(dispatch/fire :form-submit)))

(dispatch/react-to #{:state-change} (fn [_ m] (render m)))

(dispatch/react-to #{:form-change}
                   (fn [_ m]
                     (render-button [(-> m :old :status)
                                     (-> m :new :status)] )))

;; TODO Better way to add the li?
(defn render-new-tasks [tasks]
  (let [ul (by-id "task-list")]
    (doseq [t tasks]
      (append! ul (str "<li><input type='checkbox'> " t "</t>")))))

(defn reset-form []
  (set-value! (by-id "task-input") "")
  (dispatch/fire [:field-finished "task-input"] "")
  (.focus (by-id "task-input") ()))

(dispatch/react-to #{:task-list-change}
                   (fn [_ {:keys [old new]}]
                     (render-new-tasks
                      (filter #(not ((set old) %)) new))
                     (reset-form)))