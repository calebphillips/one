(ns one.sample.snippets
  "Macros for including HTML snippets in the ClojureScript application
  at compile time."
  (:use [one.templates :only (render)])
  (:require [net.cgrand.enlive-html :as html]))

;; Return the content for the tasks view, with any example
;; tasks removed
(html/defsnippet tasks "tasks.html" [:div#tasks-view] []
  [:ul#task-list] (html/content ""))

(defn- snippet [file id]
  (html/select (html/html-resource file) id))

(defmacro snippets
  "Expands to a map of HTML snippets which are extracted from the
  design templates."
  []
  (reduce (fn [m [k v]] (assoc m k (render v))) {} 
          {:tasks (tasks)
           :task (snippet "tasks.html" [:li.not-completed])
           :completed-task (snippet "tasks.html" [:li.completed]) }))
