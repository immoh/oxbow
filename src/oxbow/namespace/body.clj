(ns oxbow.namespace.body
  (:require [riddley.walk :as walk]
            [riddley.compiler :as compiler]))

(defn- resolve-ns-name [sym]
  (let [resolved (resolve sym)]
    (when (instance? clojure.lang.Var resolved)
      (some-> resolved .-ns ns-name))))

(defn- local? [sym]
  ((compiler/locals) sym))

(defn- get-resolved-symbols [form]
  (let [resolved-symbols (atom {})]
    (walk/walk-exprs symbol?
                     (fn [sym]
                       (when-not (or (local? sym) (namespace sym))
                         (swap! resolved-symbols assoc sym (resolve-ns-name sym))))
                     form)
    @resolved-symbols))

(defn analyze [forms]
  {:resolved-symbols (apply merge (map get-resolved-symbols forms))})
