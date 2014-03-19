(ns oxbow.namespace.body
  (:require [riddley.walk :as walk]
            [riddley.compiler :as compiler]))

(defn- resolve-ns-name [sym]
  (let [resolved (resolve sym)]
    (when (instance? clojure.lang.Var resolved)
      (some-> resolved .-ns ns-name))))

(defn- local? [sym]
  ((compiler/locals) sym))

(defn- qualified? [sym]
  (re-find #"[\./]" (str sym)))

(defn- resolve-and-store [res-atom sym]
  (when-not (or (local? sym) (qualified? sym))
    (when-let [sym-ns-name (resolve-ns-name sym)]
      (swap! res-atom assoc sym sym-ns-name))))  

(defn- get-resolved-symbols [form]
  (when-not (= 'defmacro (first form))
    (let [resolved-symbols (atom {})]
      (walk/walk-exprs symbol?
                       (fn [sym] (resolve-and-store resolved-symbols sym) sym)
                       (fn [first-of-form]
                         (when (symbol? first-of-form)
                           (resolve-and-store resolved-symbols first-of-form))
                         false)
                       form)
      @resolved-symbols)))

(defn analyze [forms]
  {:resolved-symbols (apply merge (map get-resolved-symbols forms))})
