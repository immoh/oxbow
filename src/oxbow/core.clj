(ns oxbow.core
  (:require [oxbow.checkers dead-ns unused-require unused-require-refer]
            [oxbow.namespace.file :as ns-file]))

(def checkers [oxbow.checkers.dead-ns/check
               oxbow.checkers.unused-require/check
               oxbow.checkers.unused-require-refer/check])

(defn check [path]
  (let [ns-infos (map ns-file/analyze (ns-file/find-recursively path))]
    (mapcat #(% ns-infos) checkers)))
