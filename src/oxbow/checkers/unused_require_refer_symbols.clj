(ns oxbow.checkers.unused-require-refer-symbols)

(defn- is-used? [resolved-symbols required-ns referred-symbol]
  (= required-ns (get resolved-symbols referred-symbol)))

(defn- filter-unused-symbols [resolved-symbols {required-ns :ns referred-symbols :refer}]
  (seq (remove (partial is-used? resolved-symbols required-ns) referred-symbols)))

(defn- find-unused-symbols-for-require [resolved-symbols {:keys [spec] :as require}]
  (when-let [unused-symbols (filter-unused-symbols resolved-symbols require)]
    {:spec spec :symbols unused-symbols}))

(defn- format-result [ns {:keys [spec symbols]}]
  {:type :unused-require-refer-symbols :ns ns :spec spec :symbols symbols})

(defn- find-unused-symbols-for-ns [{:keys [ns resolved-symbols requires]}]
  (->> (filter :refer requires)
       (keep (partial find-unused-symbols-for-require resolved-symbols))
       (map (partial format-result ns))))

(defn check [ns-infos]
  (mapcat find-unused-symbols-for-ns ns-infos))
