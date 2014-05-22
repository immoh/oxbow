(ns oxbow.checkers.dead-ns
  (:require clojure.set))

(defn- deps-from-ns-spec [{:keys [deps]}]
  (map :ns deps))

(defn- create-deps-map [ns-specs]
  (zipmap
    (map :ns ns-specs)
    (map deps-from-ns-spec ns-specs)))

(defn- find-dead-namespaces [api-namespaces namespaces-to-deps]
  (clojure.set/difference
    (set (keys namespaces-to-deps))
    (set api-namespaces)
    (set (mapcat val namespaces-to-deps))))

(defn- format-result [ns]
  {:type :dead-ns :name ns})

(defn check
  ([analysed-nses]
   (check analysed-nses {}))
  ([analyzed-nses opts]
   (->> analyzed-nses
        (create-deps-map)
        (find-dead-namespaces (:api-namespaces opts))
        (map format-result))))

