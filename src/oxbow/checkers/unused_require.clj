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

(defn- find-unused-use [{used-ns :ns} used-nses]
  (when-not (contains? used-nses used-ns)
    {:type :unused-use}))

(defn- var->ns-name [v]
  (-> v .-ns ns-name))

(defn- invert [m]
  (apply merge-with clojure.set/union (map (fn [[k v]] {v #{k}}) m)))

(defn- ns-to-unqualified-symbols [symbols-to-vars]
  (invert (keep (fn [[k v]] (when-not (namespace k) [k (var->ns-name v)])) symbols-to-vars)))

(defn- used-nses [symbols-to-vars]
  (set (keep var->ns-name (vals symbols-to-vars))))

(defmulti check-ns-dep (fn [ns used-symbols used-nses dep] (:type dep)))

(defmethod check-ns-dep :require [ns used-symbols used-nses require]
  (when-let [result (or
                     (find-unused-require require used-nses)
                     (find-unused-refer require used-symbols))]
    (assoc result :ns ns :spec (:spec require))))

(defmethod check-ns-dep :use [ns used-symbols used-nses use]
  (when-let [result (find-unused-use use used-nses)]
    (assoc result :ns ns :spec (:spec use))))

(defn- check-ns [{:keys [ns symbols-to-vars deps]}]
  (keep (partial check-ns-dep ns (ns-to-unqualified-symbols symbols-to-vars) (used-nses symbols-to-vars)) deps))

(defn check [analyzed-nses]
  (mapcat check-ns analyzed-nses))
