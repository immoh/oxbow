(ns oxbow.namespace.file
  (:require [clojure.java.io :as io]
            [oxbow.namespace [declaration :as ns-decl]
                             [body :as ns-body]])
  (:import [java.io PushbackReader]))

(defn- read* [rdr & {:keys [eof-error? eof-value]}]
  (read rdr eof-error? eof-value))

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
    (with-open [rdr (PushbackReader. (io/reader file))]
      (let [ns-form (read-ns-form rdr)]
        (eval ns-form)
        (let [forms (read-forms rdr)]
          (dorun (map eval forms))
          (merge
            {:ns-form ns-form
             :forms   forms}
            (ns-decl/analyze ns-form)
            (ns-body/analyze forms)))))))

(defn- clojure-file? [file]
  (and
    (.isFile file)
    (re-matches #".+.clj.?" (.getName file))))

(defn find-recursively [path]
  (->> (io/file path)
       (file-seq)
       (filter clojure-file?)
       (sort-by (memfn getAbsolutePath))))
