(ns ^{:doc "Defines animations which are used in the sample
  application."}
  one.sample.animation
  (:use [one.core :only (start)]
        [one.browser.animation :only (bind parallel serial play play-animation)]
        [domina :only (by-id set-html! set-styles! destroy-children! append! single-node)]
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
    (play task-form form-in {:after #(.focus (by-id "task-input") ())})))

(defn label-move-up
  "Move the passed input field label above the input field. Run when
  the field gets focus and is empty."
  [label]
  (play label [{:effect :color :end "#53607b" :time 200}
               {:effect :slide :up 40 :time 200}]))

(defn label-fade-out
  "Make the passed input field label invisible. Run when the input
  field loses focus and contains a valid input value."
  [label]
  (play label {:effect :fade :end 0 :time 200}))

(def move-down [{:effect :fade :end 1 :time 200}
                {:effect :color :end "#BBC4D7" :time 200}
                {:effect :slide :down 40 :time 200}])
(def fade-in {:effect :fade :end 1 :time 400})
(def fade-out {:effect :fade :end 0 :time 400})

(defn label-move-down
  "Make the passed input field label visible and move it down into the
  input field. Run when an input field loses focus and is empty."
  [label]
  (play label move-down))

(defn disable-button
  "Accepts an element id for a button and disables it. Fades the
  button to 0.2 opacity."
  [id]
  (let [button (by-id id)]
    (gforms/setDisabled button true)
    (play button {:effect :fade :end 0.2 :time 400})))

(defn enable-button
  "Accepts an element id for a button and enables it. Fades the button
  to an opactiy of 1."
  [id]
  (let [button (by-id id)]
    (gforms/setDisabled button false)
    (play button fade-in)))

