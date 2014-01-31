(ns oxbow.core
  (:require [oxbow.checkers.dead-ns :as dead-ns]
            [oxbow.checkers.unused-require :as unused-require]))

(def checkers [dead-ns/check
               unused-require/check])

(defn check [path]
  (mapcat #(% path) checkers))
