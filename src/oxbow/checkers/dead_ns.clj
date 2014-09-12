(ns oxbow.checkers.dead-ns
  (:require clojure.set
            [oxbow.options :as options]))

(defn- deps-from-ns-spec [{:keys [deps]}]
  (map :ns deps))

(defn- create-deps-map [ns-specs]
  (zipmap
    (map :ns ns-specs)
    (map deps-from-ns-spec ns-specs)))

(defn- find-dead-namespaces [opts namespaces-to-deps]
  (clojure.set/difference
    (set (remove (partial options/api-namespace? opts) (keys namespaces-to-deps)))
    (set (mapcat val namespaces-to-deps))))

(defn- format-result [ns]
  {:type :dead-ns :name ns})

(defn check
  ([analysed-nses]
   (check analysed-nses {}))
  ([analyzed-nses opts]
   (->> analyzed-nses
        (create-deps-map)
        (find-dead-namespaces opts)
        (map format-result))))
