(ns oxbow.checkers.ns-dependency
  (:require clojure.set))

(defmulti find-unused-symbols (fn [{:keys [type] :as dep} used-symbols]
                                (cond
                                  (and (= :require type) (= :all (:refer dep))) :require-refer-all
                                  (and (= :require type) (:refer dep))          :require-refer-symbols
                                  (and (= :use type) (:only dep))               :use-only-symbols)))

(defmethod find-unused-symbols :require-refer-all [{required-ns :ns} used-symbols]
  (when-not (seq (get used-symbols required-ns))
    {:type :unused-require-refer-symbols}))

(defn- get-unused-symbols [used-symbols dep-ns dep-symbols]
  (clojure.set/difference (set dep-symbols) (get used-symbols dep-ns)))

(defmethod find-unused-symbols :require-refer-symbols [{required-ns :ns refer :refer} used-symbols]
  (when-let [unused (seq (get-unused-symbols used-symbols required-ns refer))]
    {:type :unused-require-refer-symbols :symbols unused}))

(defmethod find-unused-symbols :use-only-symbols [{used-ns :ns only :only} used-symbols]
  (when-let [unused (seq (get-unused-symbols used-symbols used-ns only))]
    {:type :unused-use-only-symbols :symbols unused}))

(defmethod find-unused-symbols :default [& _] nil)

(defn- find-unused-dep [{dep-ns :ns} used-nses]
  (when-not (contains? used-nses dep-ns)
    {:type :unused-ns-dependency}))

(defn- var->ns-name [v]
  (-> v .-ns ns-name))

(defn- invert [m]
  (apply merge-with clojure.set/union (map (fn [[k v]] {v #{k}}) m)))

(defn- ns-to-unqualified-symbols [symbols-to-vars]
  (invert (keep (fn [[k v]] (when-not (namespace k) [k (var->ns-name v)])) symbols-to-vars)))

(defn- used-nses [symbols-to-vars]
  (set (keep var->ns-name (vals symbols-to-vars))))

(defn check-ns-dep [ns used-symbols used-nses dep]
  (when-let [result (or (find-unused-dep dep used-nses) (find-unused-symbols dep used-symbols))]
    (let [{:keys [line column]} (meta (:spec dep))]
      (assoc result :ns ns :spec (:spec dep) :line line :column column))))

(defn- check-ns [{:keys [ns symbols-to-vars deps]}]
  (keep (partial check-ns-dep ns (ns-to-unqualified-symbols symbols-to-vars) (used-nses symbols-to-vars)) deps))

(defn check
  ([analyzed-nses]
   (check analyzed-nses {}))
  ([analyzed-nses opts]
   (mapcat check-ns analyzed-nses)))
