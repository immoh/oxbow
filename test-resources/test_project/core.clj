(ns test-project.core
  (:require [test-project.deps.a :as a]
            [test-project.deps.d :as d]
            [test-project.deps.e :refer [my-function other-function]]
            [test-project.deps.f :refer :all]
            [test-project.deps.g :refer :all])
  (:use [test-project.deps.c]))

(defn foo []
  (a/foo)
  (other-function)
  (g-function))
