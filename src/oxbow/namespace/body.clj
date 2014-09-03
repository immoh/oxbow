(ns oxbow.namespace.body
  (:require [clojure.set]
            [riddley.walk :as walk]
            [riddley.compiler :as compiler]))

(defn- local? [sym]
  (get (compiler/locals) sym))

(defn- ns-name? [sym]
  (re-matches #"([^/\.]+\.)+[^/\.]+" (str sym)))

(defn- resolve-and-store [result-atom sym]
  (when (symbol? sym)
    (when-not (or (local? sym) (ns-name? sym))
      (let [resolved (resolve sym)]
        (when (var? resolved)
          (swap! result-atom assoc-in [:symbols-to-vars sym] resolved))))))

(defn- store-bindings-from-env [result-atom]
  (swap! result-atom update-in [:bindings-to-symbols] merge (clojure.set/map-invert (compiler/locals))))

(defn- store-used-binding [result-atom symbol]
  (swap! result-atom update-in [:used-bindings] conj (get (compiler/locals) symbol)))

(defmulti handle-special-form (fn [result-atom form] (first form)))

(defmethod handle-special-form 'case* [result-atom [_ generated-sym & _]]
  (store-used-binding result-atom generated-sym))

(defmethod handle-special-form :default [& _])

(defn- with-meta= [x y]
  (and (= x y)
       (= (meta x) (meta y))))

(defn- distinct-with-meta [coll]
  (reduce (fn [coll x]
            (if (some (partial with-meta= x) coll)
              coll
              (conj coll x)))
          (take 1 coll)
          (rest coll)))

(defn- unused-locals [{:keys [bindings-to-symbols used-bindings]}]
  (-> (apply dissoc bindings-to-symbols (conj used-bindings :riddley.compiler/analyze-failure))
      vals
      distinct-with-meta
      seq))

(defn analyze-form [form]
  (let [result (atom {:symbols-to-vars {}
                      :bindings-to-symbols {}
                      :used-bindings #{}})]
    (walk/walk-exprs (fn [form]
                       (store-bindings-from-env result)
                       (when (symbol? form)
                         (store-used-binding result form)
                         (resolve-and-store result form))
                       (when (and (seq? form) (special-symbol? (first form)))
                         (handle-special-form result form))
                       false)
                     (constantly nil)
                     (fn [first-of-form]
                       (when (symbol? first-of-form)
                         (resolve-and-store result first-of-form))
                       false)
                     form)
    {:symbols-to-vars (:symbols-to-vars @result)
     :unused-locals   (unused-locals @result)}))

(defn analyze [forms]
  (let [analyzed-forms (map analyze-form forms)]
    {:symbols-to-vars (apply merge (map :symbols-to-vars analyzed-forms))
     :unused-locals   (apply concat (map :unused-locals analyzed-forms))}))
