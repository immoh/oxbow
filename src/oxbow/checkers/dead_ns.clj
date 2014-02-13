(ns oxbow.checkers.dead-ns
  (:require [oxbow.tools.namespace :as ns-tool]))

(defn- parse-ns-from-ns-decl [decl]
  (second decl))

(defn- deps-from-ns-spec [{:keys [uses requires]}]
  (map :ns (concat uses requires)))

(defn- create-deps-map [ns-specs]
  (zipmap
    (map :ns ns-specs)
    (map deps-from-ns-spec ns-specs)))

(defn- find-dead-namespaces [namespaces-to-deps]
  (clojure.set/difference
    (set (keys namespaces-to-deps))
    (set (mapcat val namespaces-to-deps))))

(defn- format-result [ns]
  {:type :dead-ns :name ns})

(defn check [files]
  (->> files
       (map ns-tool/analyze)
       (create-deps-map)
       (find-dead-namespaces)
       (map format-result)))
