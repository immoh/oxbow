(ns oxbow.checkers.unused-require)

(defn- parse-namespaces-from-symbols [forms]
  (->> forms
       (mapcat (partial tree-seq sequential? identity))
       (filter symbol?)
       (keep namespace)
       (set)))

(defn is-require-used? [used-namespaces {:keys [ns as]}]
  (used-namespaces (str (or as ns))))

(defn format-result [ns {:keys [spec]}]
  {:type :unused-require :ns ns :spec spec})

(defn- find-unused-requires [{:keys [ns requires forms]}]
  (let [used-namespaces (parse-namespaces-from-symbols forms)]
    (some->> requires
             (remove (partial is-require-used? used-namespaces))
             seq
             (map (partial format-result ns)))))

(defn check [ns-infos]
  (mapcat find-unused-requires ns-infos))
