(ns oxbow.core
  (:require [clojure.java.io :as io]
            [oxbow.checkers.dead-ns :as dead-ns]
            [oxbow.checkers.unused-require :as unused-require]))

(def checkers [dead-ns/check
               unused-require/check])

(defn- clojure-file? [file]
  (and
    (.isFile file)
    (re-matches #".+.clj.?" (.getName file))))

(defn- find-clojure-files-recursively [path]
  (->> (io/file path)
       (file-seq)
       (filter clojure-file?)
       (sort-by (memfn getAbsolutePath))))

(defn check [path]
  (mapcat #(% (find-clojure-files-recursively path)) checkers))
