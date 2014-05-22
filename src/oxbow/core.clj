(ns oxbow.core
  (:require [oxbow.checkers dead-ns ns-dependency ns-var]
            [oxbow.namespace.file :as ns-file]))

(def checkers [oxbow.checkers.dead-ns/check
               oxbow.checkers.ns-dependency/check
               oxbow.checkers.ns-var/check])

(defn check
  ([path]
   (check path {}))
  ([path opts]
   (let [analyzed-nses (map ns-file/analyze (ns-file/find-recursively path))]
     (mapcat #(% analyzed-nses opts) checkers))))
