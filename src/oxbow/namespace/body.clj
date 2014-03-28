(ns oxbow.namespace.body
  (:require [riddley.walk :as walk]
            [riddley.compiler :as compiler]))

(defn- local? [sym]
  ((compiler/locals) sym))

(defn- resolve-and-store [res-atom sym]
  (when-not (local? sym)
    (let [resolved (resolve sym)]
      (when (var? resolved)
        (swap! res-atom assoc sym resolved)))))  

(defn- symbols-to-vars [form]
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
  {:symbols-to-vars (apply merge (map symbols-to-vars forms))})
