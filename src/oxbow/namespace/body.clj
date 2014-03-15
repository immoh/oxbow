(ns oxbow.namespace.body
  (:require [riddley.walk :as walk]
            [riddley.compiler :as compiler]))

(defn- get-ns-symbol [sym]
  (let [resolved (resolve sym)]
    (when (instance? clojure.lang.Var resolved)
      (some-> resolved .-ns .getName))))

(defn- local? [sym]
  ((compiler/locals) sym))

(defn- get-resolved-symbols [form]
  (let [resolved-symbols (atom {})]
    (walk/walk-exprs symbol?
                     (fn [sym]
                       (when-not (or (local? sym) (namespace sym))
                         (swap! resolved-symbols assoc sym (get-ns-symbol sym))))
                     form)
    @resolved-symbols))

(defn analyze [forms]
  {:resolved-symbols (apply merge (map get-resolved-symbols forms))})
