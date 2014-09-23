(ns oxbow.namespace.declaration)

(defn- is-decl-of-type? [valid-types form]
  (and (sequential? form)
       (valid-types (first form))))

(defn- parse-ns [ns-decl]
  (second ns-decl))

(defn- is-prefix-spec? [type spec]
  (and
    (sequential? spec)
    (second spec)
    (or (and (#{:require :use} type)
             (not (keyword? (second spec))))
        (and (#{:import} type)
             (re-find #"\." (str (first spec)))
             (not (re-find #"\." (str (second spec))))))))

(defn- create-symbol-with-prefix [prefix suffix]
  (symbol (str prefix "." suffix)))

(defn- sequentify [v]
  (if (sequential? v) v [v]))

(defn- create-spec-map [type spec & {:keys [transformer original-spec] :or {transformer identity original-spec spec}}]
  (let [[entity & {:as opts}] (sequentify spec)
        entity-key (if (= :import type) :class :ns)]
    (merge {:type type entity-key (transformer entity) :spec original-spec}  opts)))

(defn- analyze-libspec [[type spec]]
  (if (is-prefix-spec? type spec)
    (let [[prefix & suffix-specs] spec]
      (map #(create-spec-map type % :transformer (partial create-symbol-with-prefix prefix) :original-spec spec) suffix-specs))
    [(create-spec-map type spec)]))

(defn- expand-libspec [[type & specs]]
  (map (partial list type) specs))

(defn- parse-deps [ns-decl]
  (->> ns-decl
       (filter (partial is-decl-of-type? #{:require :use :import}))
       (mapcat expand-libspec)
       (mapcat analyze-libspec)))

(defn is-ns-decl? [form]
  (is-decl-of-type? #{'ns} form))

(defn analyze [ns-decl]
  {:ns   (parse-ns ns-decl)
   :deps (parse-deps ns-decl)})
