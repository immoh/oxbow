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

(defn- unused-locals [{:keys [bindings-to-symbols used-bindings]}]
  (->> (apply dissoc bindings-to-symbols (conj used-bindings :riddley.compiler/analyze-failure))
       vals
       (remove #(-> % meta ::skip-unused))
       seq))

(defn- add-nil-body-at-index [n form]
  (concat form
          (when-not (seq (drop n form))
            [nil])))

(defmulti ^:private ensure-binding-form-body-not-empty first)

(defmethod ensure-binding-form-body-not-empty 'fn* [form]
  (let [[prelude remainder] (split-with (complement sequential?) form)
        remainder (if (vector? (first remainder))
                    (list remainder)
                    remainder)]
    (concat prelude (map (partial add-nil-body-at-index 1) remainder))))

(defmethod ensure-binding-form-body-not-empty 'reify* [[_ classes & methods]]
  (list* 'reify* classes (map (partial add-nil-body-at-index 2) methods)))

(defmethod ensure-binding-form-body-not-empty 'let*    [form] (add-nil-body-at-index 2 form))
(defmethod ensure-binding-form-body-not-empty 'letfn*  [form] (add-nil-body-at-index 2 form))
(defmethod ensure-binding-form-body-not-empty 'loop*   [form] (add-nil-body-at-index 2 form))
(defmethod ensure-binding-form-body-not-empty 'catch   [form] (add-nil-body-at-index 3 form))
(defmethod ensure-binding-form-body-not-empty :default [form] form)

(defn- mark-fn-name-skip-unused [[fn name & body]]
  (list* fn (vary-meta name assoc ::skip-unused true) body))

(defmulti ^:private mark-locals-skip-unused first)

(defmethod mark-locals-skip-unused 'letfn* [[_ bindings & body]]
  (list* 'letfn*
         (vec (mapcat (fn [[k v]] [k (mark-fn-name-skip-unused v)]) (partition-all 2 bindings)))
         body))

(defmethod mark-locals-skip-unused  :default [form] form)

(defn- transform-form [form]
  (-> form
      ensure-binding-form-body-not-empty
      mark-locals-skip-unused))

(defn collection-information [result form]
  (store-bindings-from-env result)
  (when (symbol? form)
    (store-used-binding result form)
    (resolve-and-store result form)))

(defn walk-exprs
  ([result form]
   (walk-exprs result form false))
  ([result form special-form?]
    (walk/walk-exprs (fn [form]
                       (let [seq-not-transformed? (and (seq? form)
                                                       (not special-form?)
                                                       (not (-> form meta ::transformed)))]
                         (if seq-not-transformed?
                           (compiler/with-lexical-scoping
                             (walk-exprs result
                                         (with-meta (transform-form form) (merge (meta form) {::transformed true}))
                                         special-form?))
                           (collection-information result form))
                         (when (and (seq? form)
                                    (= 'quote (first form)))
                           (walk-exprs result (rest form) true))
                         seq-not-transformed?))
                     (constantly nil)
                     (fn [first-of-form]
                       (when (symbol? first-of-form)
                         (resolve-and-store result first-of-form))
                       special-form?)
                     form)))

(defn analyze-form [form]
  (let [result (atom {:symbols-to-vars {}
                      :bindings-to-symbols {}
                      :used-bindings #{}})]
    (try
      (walk-exprs result form)
      (catch Throwable t
        (throw (ex-info "Failed to analyze form" {:type ::analyze-failure :form form} t))))
    {:symbols-to-vars (:symbols-to-vars @result)
     :unused-locals   (unused-locals @result)}))

(defn analyze [forms]
  (let [analyzed-forms (map analyze-form forms)]
    {:symbols-to-vars (apply merge (map :symbols-to-vars analyzed-forms))
     :unused-locals   (apply concat (map :unused-locals analyzed-forms))}))
