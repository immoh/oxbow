(ns oxbow.namespace.declaration)

(defn- is-decl-of-type? [type form]
  (and (sequential? form)
       (= type (first form))))

(defn- parse-ns [ns-decl]
  (second ns-decl))

(defn- sequentify [v]
  (if (sequential? v) v [v]))

(defn- create-spec-map [spec]
  (let [[ns & {:as opts}] (sequentify spec)]
    (merge {:ns ns :spec spec} opts)))

(defn- parse-deps-of-type [type ns-decl]
  (->> ns-decl
       (filter (partial is-decl-of-type? type))
       (mapcat rest)
       (map create-spec-map)))

(defn is-ns-decl? [form]
  (is-decl-of-type? 'ns form))

(defn analyze [ns-decl]
  {:ns       (parse-ns ns-decl)
   :uses     (parse-deps-of-type :use ns-decl)
   :requires (parse-deps-of-type :require ns-decl)})
