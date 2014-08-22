(ns oxbow.namespace.file
  (:require [clojure.java.io :as io]
            [clojure.tools.reader :as reader]
            [clojure.tools.reader.reader-types :as reader-types]
            [oxbow.namespace [declaration :as ns-decl]
                             [body :as ns-body]])
  (:import [java.io PushbackReader]))

(defn- read* [rdr & {:keys [eof-error? eof-value]}]
  (reader/read rdr eof-error? eof-value))

(defn- read-next [rdr]
  (read* rdr :eof-error? false :eof-value :eof))

(defn- read-forms [rdr]
  (doall
    (take-while
      #(not= :eof %)
      (repeatedly #(read-next rdr)))))

(defn- read-ns-form [rdr]
  (let [form (read-next rdr)]
    (cond
      (= :eof form)  nil
      (ns-decl/is-ns-decl? form)  form
      :else  (recur rdr))))

(defn analyze [file]
  (println "Analyzing" (.getPath file))
  (binding [*ns* *ns*]
    (with-open [pushback-rdr (PushbackReader. (io/reader file))]
      (let [indexing-rdr (reader-types/indexing-push-back-reader pushback-rdr)
            ns-form (read-ns-form indexing-rdr)]
        (eval ns-form)
        (let [forms (read-forms indexing-rdr)]
          (dorun (map eval forms))
          (merge
            {:ns-form ns-form
             :forms   forms
             :interns (ns-interns *ns*)}
            (ns-decl/analyze ns-form)
            (ns-body/analyze forms)))))))

(defn- clojure-file? [file]
  (and
    (.isFile file)
    (re-matches #".+.clj.?" (.getName file))))

(defn find-recursively [paths]
  (->> paths
       (map io/file)
       (mapcat file-seq)
       (filter clojure-file?)
       (sort-by (memfn getAbsolutePath))))
