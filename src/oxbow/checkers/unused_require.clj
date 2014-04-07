(ns oxbow.checkers.unused-require
  (:require clojure.set))

(defmulti find-unused-refer (fn [{:keys [refer]} used-symbols] refer))

(defmethod find-unused-refer nil [& _] nil)

(defmethod find-unused-refer :all [{required-ns :ns} used-symbols]
  (when-not (seq (get used-symbols required-ns))
    {:type :unused-require-refer-all}))

(defmethod find-unused-refer :default [{required-ns :ns refer :refer} used-symbols]
  (when-let [unused (clojure.set/difference (set refer) (get used-symbols required-ns))]
    {:type :unused-require-refer-symbols :symbols (seq unused)}))

(defn- find-unused-require [{required-ns :ns} used-nses]
  (when-not (contains? used-nses required-ns)
    {:type :unused-require}))

(defn- var->ns-name [v]
  (-> v .-ns ns-name))

(defn- invert [m]
  (apply merge-with clojure.set/union (map (fn [[k v]] {v #{k}}) m)))

(defn- ns-to-unqualified-symbols [symbols-to-vars]
  (invert (keep (fn [[k v]] (when-not (namespace k) [k (var->ns-name v)])) symbols-to-vars)))

(defn- used-nses [symbols-to-vars]
  (set (keep var->ns-name (vals symbols-to-vars))))

(defn- check-ns-require [ns used-symbols used-nses require]
  (when-let [result (or
                     (find-unused-require require used-nses)
                     (find-unused-refer require used-symbols))]
    (assoc result :ns ns :spec (:spec require))))

(defn- check-ns [{:keys [ns symbols-to-vars requires]}]
  (keep (partial check-ns-require ns (ns-to-unqualified-symbols symbols-to-vars) (used-nses symbols-to-vars)) requires))

(defn check [analyzed-nses]
  (mapcat check-ns analyzed-nses))
