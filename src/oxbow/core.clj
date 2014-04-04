(ns oxbow.core
  (:require [oxbow.checkers dead-ns unused-require]
            [oxbow.namespace.file :as ns-file]))

(def checkers [oxbow.checkers.dead-ns/check
               oxbow.checkers.unused-require/check])

(defn check [path]
  (let [analyzed-nses (map ns-file/analyze (ns-file/find-recursively path))]
    (mapcat #(% analyzed-nses) checkers)))
