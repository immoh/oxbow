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

(defn- add-nil-body-at-index [n form]
  (concat form
          (when-not (seq (drop n form))
            [nil])))

(defn- ensure-fn-bodies-not-empty [form]
  (let [[prelude remainder] (split-with (complement sequential?) form)
        remainder (if (vector? (first remainder))
                    (list remainder)
                    remainder)]
    (concat prelude (map (partial add-nil-body-at-index 1) remainder))))

(defn- ensure-reify-bodies-not-empty [[_ classes & methods]]
  (list* 'reify* classes (map (partial add-nil-body-at-index 2) methods)))

(def ^:private ensure-body-not-empty-fns
  {'let*   (partial add-nil-body-at-index 2)
   'loop*  (partial add-nil-body-at-index 2)
   'catch  (partial add-nil-body-at-index 3)
   'fn*    ensure-fn-bodies-not-empty
   'reify* ensure-reify-bodies-not-empty})

(defn- binding-form? [form]
  (and (seq? form) (ensure-body-not-empty-fns (first form))))

(defn collection-information [result form]
  (store-bindings-from-env result)
  (when (symbol? form)
    (store-used-binding result form)
    (resolve-and-store result form))
  (when (and (seq? form) (special-symbol? (first form)))
    (handle-special-form result form)))

(defn walk-exprs [result form]
  (walk/walk-exprs (fn [form]
                     (let [handle-empty-bodies? (and (binding-form? form)
                                                     (not (-> form meta ::empty-bodies-handled)))]
                       (if handle-empty-bodies?
                         (compiler/with-lexical-scoping
                           (walk-exprs result (with-meta ((ensure-body-not-empty-fns (first form)) form)
                                                         (merge (meta form) {::empty-bodies-handled true}))))
                         (collection-information result form))
                       handle-empty-bodies?))
                   (constantly nil)
                   (fn [first-of-form]
                     (when (symbol? first-of-form)
                       (resolve-and-store result first-of-form))
                     false)
                   form))

(defn analyze-form [form]
  (let [result (atom {:symbols-to-vars {}
                      :bindings-to-symbols {}
                      :used-bindings #{}})]
    (walk-exprs result form)
    {:symbols-to-vars (:symbols-to-vars @result)
     :unused-locals   (unused-locals @result)}))

(defn analyze [forms]
  (let [analyzed-forms (map analyze-form forms)]
    {:symbols-to-vars (apply merge (map :symbols-to-vars analyzed-forms))
     :unused-locals   (apply concat (map :unused-locals analyzed-forms))}))
