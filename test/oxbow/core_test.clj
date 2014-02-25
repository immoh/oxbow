(ns oxbow.core-test
  (:use midje.sweet)
  (:require [oxbow.core :as core]))

(def check-test-project (memoize (fn [] (core/check "test-resources/test_project"))))

;; :dead-ns

(fact "Namespaces that are not referenced from anywhere are reported as dead"
  (check-test-project) => (contains {:type :dead-ns :name 'test-project.deps.b}))

(fact "Namespaces imported with :require  are not reported as dead"
  (check-test-project) =not=> (contains {:type :dead-ns :name 'test-project.deps.a}))

(fact "Namespaces imported with :use are not reported as dead"
  (check-test-project) =not=> (contains {:type :dead-ns :name 'test-project.deps.c}))

;; :unused-require

(fact "Unused require is reported"
  (check-test-project) => (contains {:type :unused-require :ns 'test-project.core :spec '[test-project.deps.d :as d]}))

(fact "Used require is not reported"
  (check-test-project) =not=> (contains {:type :unused-require :ns 'test-project.core :spec '[test-project.deps.a :as a]}))

;; unused-require-refer-symbols

(fact "Unused referred symbols from require are reported"
 (check-test-project) => (contains {:type :unused-require-refer-symbols
                                    :ns   'test-project.core
                                    :spec '[test-project.deps.e :refer [my-function other-function]]
                                    :symbols '(my-function)}))
