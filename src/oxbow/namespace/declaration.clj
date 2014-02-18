(ns oxbow.namespace.declaration)

(defn- is-decl-of-type? [type form]
  (and (sequential? form)
       (= type (first form))))

(defn- parse-ns [ns-decl]
  (second ns-decl))

(defn- extract-ns [spec]
  (if (sequential? spec)
    (first spec)
    spec))

(defn- extract-alias [spec]
  (if (sequential? spec)
    (second (drop-while #(not= % :as) spec))
    spec))

(defn- create-spec-map [spec]
  {:spec spec :ns (extract-ns spec) :alias (extract-alias spec)})

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
