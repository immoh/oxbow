(ns oxbow.checkers.ns-dependency
  (:require clojure.set
            clojure.string))

(defmulti find-unused-symbols (fn [{:keys [type] :as dep} used-symbols]
                                (cond
                                  (and (= :require type) (= :all (:refer dep))) :require-refer-all
                                  (and (= :require type) (:refer dep))          :require-refer-symbols
                                  (and (= :use type) (:only dep))               :use-only-symbols)))

(defmethod find-unused-symbols :require-refer-all [{required-ns :ns spec :spec} used-symbols]
  (when-not (seq (get used-symbols required-ns))
    [{:type :unused-require-refer-symbols}]))

(defn- get-unused-symbols [used-symbols dep-ns dep-symbols]
  (clojure.set/difference (set dep-symbols) (get used-symbols dep-ns)))

(defn- unused-symbol-result [type unused-symbol]
  {:type type :symbol unused-symbol})

(defmethod find-unused-symbols :require-refer-symbols [{required-ns :ns refer :refer} used-symbols]
  (map (partial unused-symbol-result :unused-require-refer-symbols) (get-unused-symbols used-symbols required-ns refer)))

(defmethod find-unused-symbols :use-only-symbols [{used-ns :ns only :only} used-symbols]
  (map (partial unused-symbol-result :unused-use-only-symbols) (get-unused-symbols used-symbols used-ns only)))

(defmethod find-unused-symbols :default [& _] nil)

(defn- find-unused-dep [{dep-ns :ns spec :spec} used-nses]
  (when-not (contains? used-nses dep-ns)
    [{:type :unused-ns-dependency}]))

(defn- var->ns-name [v]
  (-> v .-ns ns-name))

(defn- invert [m]
  (apply merge-with clojure.set/union (map (fn [[k v]] {v #{k}}) m)))

(defn- ns-to-unqualified-symbols [symbols-to-vars]
  (invert (keep (fn [[k v]] (when-not (namespace k) [k (var->ns-name v)])) symbols-to-vars)))

(defn- used-nses [symbols-to-vars]
  (set (keep var->ns-name (vals symbols-to-vars))))

(defn- format-result [ns dep result]
  (let [{:keys [line column]} (meta (:spec dep))]
    (assoc result :ns ns :spec (:spec dep) :line line :column column)))

(defn- check-ns-dep [ns used-symbols used-nses dep]
  (map (partial format-result ns dep) (or (find-unused-dep dep used-nses) (find-unused-symbols dep used-symbols))))

(defn- unqualify-class-name-symbol [s]
  (when-let [s (name s)]
    (when-let [class-name (last (clojure.string/split (name s) #"\."))]
      (symbol class-name))))

(defn- class->symbol [c]
  (when c
    (symbol (.getName c))))

(defn- check-import [ns symbols-to-classes {:keys [class] :as dep}]
  (when-not (= class (class->symbol (get symbols-to-classes (unqualify-class-name-symbol class))))
    (format-result ns dep {:type :unused-import :class class})))

(defn- check-ns [{:keys [ns resolved-symbols deps]}]
  (let [symbols-to-vars (into {} (filter (comp var? val) resolved-symbols))
        symbols-to-classes (into {} (filter (comp class? val) resolved-symbols))]
    (concat (mapcat (partial check-ns-dep ns (ns-to-unqualified-symbols symbols-to-vars) (used-nses symbols-to-vars))
                    (filter (comp #{:require :use} :type) deps))
            (keep (partial check-import ns symbols-to-classes)
                  (filter #(= :import (:type %)) deps)))))

(defn check
  ([analyzed-nses]
   (check analyzed-nses {}))
  ([analyzed-nses opts]
   (mapcat check-ns analyzed-nses)))
