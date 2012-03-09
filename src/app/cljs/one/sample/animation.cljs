(ns ^{:doc "Defines animations which are used in the sample
  application."}
  one.sample.animation
  (:use [one.core :only (start)]
        [one.browser.animation :only (play bind)]
        [domina :only (by-id set-html! set-styles! destroy-children! single-node
                             add-class! remove-class!)]
        [domina.xpath :only (xpath)])
  (:require [goog.dom.forms :as gforms]
            [goog.style :as style]))

(def ^:private
  form-in {:effect :fade :start 0 :end 1 :time 800})

(def task-form "//div[@id='task-form']")
(def task-label "//label[@id='task-input-label']/span")

(defn initialize-task-views
  [task-html]
  (let [content (xpath "//div[@id='content']")]
    (destroy-children! content)
    (set-html! content task-html)
    ;; Required for IE8 to work correctly
    (style/setOpacity (single-node (xpath task-label)) 1)
    (set-styles! (by-id "task-button") {:opacity "0.2" :disabled true})
    (play "//div[@id='content']" form-in {:after #(.focus (by-id "task-input") ())})))

(def fade-in {:effect :fade :end 1 :time 400})
(def fade-out {:effect :fade :end 0 :time 400})

(def white [255 255 255])
(def smoky [235 235 235])
(defn show-new-task [id]
  (start (bind (by-id id)
               [(assoc fade-in :time 400)
                {:effect :bg-color :start white :end smoky :time 400}]
               {:effect :bg-color :start smoky :end white :time 600})))

(defn fade-task-out [id]
  (let [li (by-id id)]
    (play li (assoc fade-out :end 0.4 :time 200))
    (remove-class! li "not-completed")
    (add-class! li "completed")))

(defn fade-task-in [id]
  (let [li (by-id id)]
    (play li fade-in)
    (remove-class! li "completed")
    (add-class! li "not-completed")))

(defn disable-button
  "Accepts an element id for a button and disables it. Fades the
  button to 0.2 opacity."
  [id]
  (let [button (by-id id)]
    (gforms/setDisabled button true)
    (play button (assoc fade-in :end 0.2))))

(defn enable-button
  "Accepts an element id for a button and enables it. Fades the button
  to an opactiy of 1."
  [id]
  (let [button (by-id id)]
    (gforms/setDisabled button false)
    (play button fade-in)))

