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
    (let [{:keys [symbols-to-vars bindings-to-symbols used-bindings]} @result]
      {:symbols-to-vars symbols-to-vars
       :unused-locals   (vals (apply dissoc bindings-to-symbols used-bindings))})))

(defn analyze [forms]
  (let [analyzed-forms (map analyze-form forms)]
    {:symbols-to-vars (apply merge (map :symbols-to-vars analyzed-forms))
     :unused-locals   (apply concat (map :unused-locals analyzed-forms))}))
