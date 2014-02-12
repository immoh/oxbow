(ns oxbow.checkers.unused-require
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.find :as ns-find]
            [oxbow.tools.namespace :as oxbow-ns]))

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

(defn check [path]
  (mapcat
    (comp find-unused-requires oxbow-ns/analyze)
    (ns-find/find-clojure-sources-in-dir (io/file path))))
