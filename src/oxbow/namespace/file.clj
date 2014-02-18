(ns oxbow.namespace.file
  (:require [clojure.java.io :as io]
            [oxbow.namespace.declaration :as ns-decl])
  (:import [java.io PushbackReader]))

(defn- read-next [rdr & {:keys [eof-error? eof-value]}]
  (read rdr eof-error? eof-value))

(defn- read-forms [file]
  (with-open [rdr (PushbackReader. (io/reader file))]
    (doall
      (take-while
        #(not= % :eof)
        (repeatedly #(read-next rdr :eof-error? false :eof-value :eof))))))

(defn analyze [file]
  (let [[ns-form & forms] (drop-while (complement (partial ns-decl/is-ns-decl?)) (read-forms file))]
    (merge
      (ns-decl/analyze ns-form)
      {:forms forms})))

(defn- clojure-file? [file]
  (and
    (.isFile file)
    (re-matches #".+.clj.?" (.getName file))))

(defn find-recursively [path]
  (->> (io/file path)
       (file-seq)
       (filter clojure-file?)
       (sort-by (memfn getAbsolutePath))))
