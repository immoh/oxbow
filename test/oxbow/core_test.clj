(ns oxbow.core-test
  (:use midje.sweet)
  (:require [oxbow.core :as core]))

(def check-test-project (memoize (fn [] (core/check "test-resources/test-project"))))

(fact "Namespaces that are not referenced from anywhere are reported as dead"
  (check-test-project) => (contains {:type :dead-ns :name 'test-project.deps.b}))

(fact "Namespaces imported with :require  are not reported as dead"
  (check-test-project) =not=> (contains {:type :dead-ns :name 'test-project.deps.a}))

(fact "Namespaces imported with :use are not reported as dead"
  (check-test-project) =not=> (contains {:type :dead-ns :name 'test-project.deps.c}))
