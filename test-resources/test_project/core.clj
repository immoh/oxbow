(ns test-project.core
  (:require [test-project.deps.a :as a]
            [test-project.deps.d :as d]
            [test-project.deps.e :refer [my-function other-function]])
  (:use [test-project.deps.c]))

(defn foo []
  (a/foo)
  (other-function))
