(ns oxbow.core
  (:require [oxbow.checkers.dead-ns :as dead-ns]
            [oxbow.checkers.unused-require :as unused-require]
            [oxbow.namespace.file :as ns-file]))

(def checkers [dead-ns/check
               unused-require/check])

(defn check [path]
  (let [ns-infos (map ns-file/analyze (ns-file/find-recursively path))]
    (mapcat #(% ns-infos) checkers)))
