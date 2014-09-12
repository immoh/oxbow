(ns oxbow.options)

(defn- create-matcher-fn [value]
  (cond
    (nil? value)    (constantly false)
    (= :all value)  (constantly true)
    (coll? value)   (set (map symbol value))
    :else           #{(symbol value)}))

(defn api-namespace? [opts ns-symbol]
  ((create-matcher-fn (:api-namespaces opts)) ns-symbol))
