(ns oxbow.checkers.unused-require
  (:require [oxbow.tools.namespace :as ns-tool]))

(defn- parse-namespaces-from-symbols [forms]
  (->> forms
       (mapcat (partial tree-seq sequential? identity))
       (filter symbol?)
       (keep namespace)
       (set)))

(defn format-result [ns {:keys [spec]}]
  {:type :unused-require :ns ns :spec spec})

(defn- find-unused-requires [{:keys [ns requires forms]}]
  (let [used-namespaces (parse-namespaces-from-symbols forms)]
    (some->> requires
             (remove (comp used-namespaces str :alias))
             seq
             (map (partial format-result ns)))))

(defn check [files]
  (mapcat (comp find-unused-requires ns-tool/analyze) files))
