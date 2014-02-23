(ns oxbow.namespace.file
  (:require [clojure.java.io :as io]
            [oxbow.namespace.declaration :as ns-decl])
  (:import [java.io PushbackReader]))

(defn- read* [rdr & {:keys [eof-error? eof-value]}]
  (read rdr eof-error? eof-value))

(defn- read-next [file rdr]
  (try
    (read* rdr :eof-error? false :eof-value :eof)
    (catch RuntimeException e
      (println (format "Error while reading %s: %s" (.getAbsolutePath file) (.getMessage e)))
      :error)))

(defn- read-forms [file]
  (with-open [rdr (PushbackReader. (io/reader file))]
    (doall
      (take-while
        (complement #{:eof :error})
        (repeatedly #(read-next file rdr))))))

(defn analyze [file]
  (let [[ns-form & forms] (drop-while (complement ns-decl/is-ns-decl?) (read-forms file))]
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
