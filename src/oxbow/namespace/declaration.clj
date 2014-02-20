(ns oxbow.namespace.declaration)

(defn- is-decl-of-type? [type form]
  (and (sequential? form)
       (= type (first form))))

(defn- parse-ns [ns-decl]
  (second ns-decl))

(defn- is-prefix-spec? [spec]
  (and
    (sequential? spec)
    (second spec)
    (not (keyword? (second spec)))))

(defn- concat-to-ns-symbol [prefix suffix]
  (symbol (str prefix "." suffix)))

(defn- sequentify [v]
  (if (sequential? v) v [v]))

(defn- create-spec-map [spec & {:keys [ns-transformer original-spec] :or {ns-transformer identity original-spec spec}}]
  (let [[ns & {:as opts}] (sequentify spec)]
    (merge {:ns (ns-transformer ns) :spec original-spec} opts)))

(defn- analyze-libspec [spec]
  (if (is-prefix-spec? spec)
    (let [[prefix & suffix-specs] spec]
      (map #(create-spec-map % :ns-transformer (partial concat-to-ns-symbol prefix) :original-spec spec) suffix-specs))
    [(create-spec-map spec)]))

(defn- parse-deps-of-type [type ns-decl]
  (->> ns-decl
       (filter (partial is-decl-of-type? type))
       (mapcat rest)
       (mapcat analyze-libspec)))

(defn is-ns-decl? [form]
  (is-decl-of-type? 'ns form))

(defn analyze [ns-decl]
  {:ns       (parse-ns ns-decl)
   :uses     (parse-deps-of-type :use ns-decl)
   :requires (parse-deps-of-type :require ns-decl)})
