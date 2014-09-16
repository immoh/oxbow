(ns test-project.core
  (:require [test-project.deps.a :as a]
            [test-project.deps.d :as d]
            [test-project.deps.e :refer [my-function other-function]]
            [test-project.deps.f :as f :refer :all]
            [test-project.deps.g :refer :all]
            [test-project.deps.h :refer :all]
            [slingshot.slingshot :refer [try+]])
  (:use [test-project.deps.c :only [main]]
        [test-project.deps.i]
        [test-project.deps.j :only [configuration j-function]]
        [test-project.deps.k :rename {main k-main}]))

(defn- square [x]
  (* x x))

(defn- square-sum [& xs]
  (apply + (map square xs)))

(defn execute [& _]
  (a/foo)
  (other-function)
  (f/f-function)
  (g-function)
  (i-function)
  (j-function)
  (k-main))

(defn plus [x y]
  (let [y 5]
    (+ x y)))

(defmacro with-exception-printing [& body]
  `(try
     (do ~@body)
     (catch Exception e#
       (println "Exception!"))))

(defn divide-by-zero [x]
  (with-exception-printing (/ x 0)))

(defn watch-fn [_key _ref old new]
  (println old "->" new))

(defprotocol Foo)

(defn slingshot-test []
  (try+
    (/ 5 0)
    (catch [:type ::division-by-zero] {}
      (println "Division by zero!"))))

(def proxy-objext (proxy [Object] []))

(doseq [x nil])
