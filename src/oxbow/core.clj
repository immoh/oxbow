(ns oxbow.core
  (:require [oxbow.checkers.dead-ns :as dead-ns]))

(def checkers [dead-ns/check])

(defn check [path]
  (mapcat #(% path) checkers))
