(ns oxbow.core
  (:require [oxbow.checkers dead-ns ns-dependency ns-var unused-local]
            [oxbow.namespace.file :as ns-file]
            [oxbow.options :as options]))

(def checkers [oxbow.checkers.dead-ns/check
               oxbow.checkers.ns-dependency/check
               oxbow.checkers.ns-var/check
               oxbow.checkers.unused-local/check])

(defn check
  ([paths]
   (check paths {}))
  ([paths opts]
   (let [analyzed-nses (map ns-file/analyze (ns-file/find-recursively paths))]
     (mapcat #(% analyzed-nses (options/parse opts)) checkers))))
