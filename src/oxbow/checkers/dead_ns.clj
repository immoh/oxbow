(ns oxbow.checkers.dead-ns
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.find :as ns-find]
            [clojure.tools.namespace.parse :as ns-parse]))

(defn- parse-ns-from-ns-decl [decl]
  (second decl))

(defn- create-deps-map-from-ns-decls [decls]
  (zipmap
    (map parse-ns-from-ns-decl decls)
    (map ns-parse/deps-from-ns-decl decls)))

(defn- find-dead-namespaces [namespaces-to-deps]
  (clojure.set/difference
    (set (keys namespaces-to-deps))
    (set (mapcat val namespaces-to-deps))))

(defn- format-result [ns]
  {:type :dead-ns :name ns})

(defn check [path]
  (->> (io/file path)
       (ns-find/find-ns-decls-in-dir)
       (create-deps-map-from-ns-decls)
       (find-dead-namespaces)
       (map format-result)))
