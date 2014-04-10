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

(defn- create-spec-map [type spec & {:keys [ns-transformer original-spec] :or {ns-transformer identity original-spec spec}}]
  (let [[ns & {:as opts}] (sequentify spec)]
    (merge {:type type :ns (ns-transformer ns) :spec original-spec} opts)))

(defn- analyze-libspec [[type spec]]
  (if (is-prefix-spec? spec)
    (let [[prefix & suffix-specs] spec]
      (map #(create-spec-map type % :ns-transformer (partial concat-to-ns-symbol prefix) :original-spec spec) suffix-specs))
    [(create-spec-map type spec)]))

(defn- expand-libspec [[ type & specs]]
  (map (partial list type) specs))

(defn- parse-deps [ns-decl]
  (->> ns-decl
       (filter #(or (is-decl-of-type? :require %) (is-decl-of-type? :use %)))
       (mapcat expand-libspec)
       (mapcat analyze-libspec)))

(defn is-ns-decl? [form]
  (is-decl-of-type? 'ns form))

(defn analyze [ns-decl]
  {:ns   (parse-ns ns-decl)
   :deps (parse-deps ns-decl)})
