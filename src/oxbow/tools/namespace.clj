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

(defn- is-decl-of-type? [type form]
  (and (sequential? form)
       (= type (first form))))

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

(defn- parse-deps-of-type [type ns-decl]
  (->> ns-decl
       (filter (partial is-decl-of-type? type))
       (mapcat rest)
       (map create-spec-map)))

(defn analyze [file]
  (let [[ns-form & forms] (drop-while (complement (partial is-decl-of-type? 'ns)) (read-forms file))]
    {:ns       (parse-ns ns-form)
     :uses     (parse-deps-of-type :use ns-form)
     :requires (parse-deps-of-type :require ns-form)
     :forms    forms}))
