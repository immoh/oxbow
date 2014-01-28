(ns oxbow.core-test
  (:use midje.sweet)
  (:require [oxbow.core :as core]))

(defn- check-test-project []
  (core/check "test-resources/test-project"))

(fact "Namespaces that are not referenced from anywhere are reported as dead"
  (check-test-project) => (contains {:type :dead-ns :name 'test-project.deps.b}))

(fact "Referenced namespaces are not reported as dead"
  (check-test-project) =not=> (contains {:type :dead-ns :name 'test-project.deps.a}))
