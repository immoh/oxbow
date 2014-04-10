(ns oxbow.checkers.dead-ns
  (:require clojure.set))

(defn- deps-from-ns-spec [{:keys [deps]}]
  (map :ns deps))

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

(defn check [analyzed-nses]
  (->> analyzed-nses
       (create-deps-map)
       (find-dead-namespaces)
       (map format-result)))
