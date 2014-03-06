(ns oxbow.checkers.unused-require-refer
  (:require clojure.set))

(defmulti find-unused-refers (fn [ns used-symbols {:keys [refer]}] refer))

(defmethod find-unused-refers nil [& _] nil)

(defmethod find-unused-refers :all [ns used-symbols {required-ns :ns spec :spec}]
  (when-not (seq (get used-symbols required-ns))
    {:type :unused-require-refer-all :ns ns :spec spec}))

(defmethod find-unused-refers :default [ns used-symbols {required-ns :ns refer :refer spec :spec}]
  (when-let [unused (clojure.set/difference (set refer) (get used-symbols required-ns))]
    {:type :unused-require-refer-symbols :ns ns :spec spec :symbols (seq unused)}))

(defn- invert [m]
  (apply merge-with clojure.set/union (map (fn [[k v]] {v #{k}}) m)))

(defn- find-unused-refers-for-ns [{:keys [ns resolved-symbols requires]}]
  (keep (partial find-unused-refers ns (invert resolved-symbols)) requires))

(defn check [ns-infos]
  (mapcat find-unused-refers-for-ns ns-infos))
