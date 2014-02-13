(ns oxbow.tools.namespace
  (:require [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(defn- read-next [rdr & {:keys [eof-error? eof-value]}]
  (read rdr eof-error? eof-value))

(defn- read-forms [file]
  (with-open [rdr (PushbackReader. (io/reader file))]
    (doall
      (take-while
        #(not= % :eof)
        (repeatedly #(read-next rdr :eof-error? false :eof-value :eof))))))

(defn- is-ns-decl? [form]
  (and (sequential? form)
       (= 'ns (first form))))

(defn is-require-decl? [form]
  (and (sequential? form)
       (= :require (first form))))

(defn- parse-ns [ns-decl]
  (second ns-decl))

(defn- extract-ns [spec]
  (if (sequential? spec)
    (first spec)
    spec))

(defn- extract-alias [spec]
  (if (sequential? spec)
    (second (drop-while #(not= % :as) spec))
    spec))

(defn- create-spec-map [spec]
  {:spec spec :ns (extract-ns spec) :alias (extract-alias spec)})

(defn- parse-requires [ns-decl]
  (->> ns-decl
       (filter is-require-decl?)
       (mapcat rest)
       (map create-spec-map)))

(defn analyze [file]
  (let [[ns-form & forms] (drop-while (complement is-ns-decl?) (read-forms file))]
    {:ns       (parse-ns ns-form)
     :requires (parse-requires ns-form)
     :forms    forms}))
