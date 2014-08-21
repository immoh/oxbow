(ns oxbow.namespace.body
  (:require [clojure.set]
            [riddley.walk :as walk]
            [riddley.compiler :as compiler]))

(defn- local? [sym]
  ((compiler/locals) sym))

(defn- ns-name? [sym]
  (re-matches #"([^/\.]+\.)+[^/\.]+" (str sym)))

(defn- resolve-and-store [res-atom sym]
  (when (symbol? sym)                                       ; riddley leaks vars
    (when-not (or (local? sym) (ns-name? sym))
      (let [resolved (resolve sym)]
        (when (var? resolved)
          (swap! res-atom assoc sym resolved))))))

(defn- symbols-to-vars [form]
  (let [resolved-symbols (atom {})]
    (walk/walk-exprs symbol?
                     (fn [sym] (resolve-and-store resolved-symbols sym) sym)
                     (fn [first-of-form]
                       (when (symbol? first-of-form)
                         (resolve-and-store resolved-symbols first-of-form))
                       false)
                     form)
    @resolved-symbols))

(defn- get-unused-locals [form]
  (let [all-bindings-to-symbols (atom {})
        used-bindings (atom #{})]
    (walk/walk-exprs (fn [form]
                       (swap! all-bindings-to-symbols merge (clojure.set/map-invert (compiler/locals)))
                       (when (symbol? form)
                         (swap! used-bindings conj (get (compiler/locals) form)))
                       false)
                     identity
                     form)
    (vals (apply dissoc @all-bindings-to-symbols @used-bindings))))

(defn analyze [forms]
  {:symbols-to-vars (apply merge (map symbols-to-vars forms))
   :unused-locals   (mapcat get-unused-locals forms)})
