(ns test-project.core
  (:require [test-project.deps.a :as a]
            [test-project.deps.d :as d])
  (:use [test-project.deps.c]))

(defn foo []
  (a/foo))
