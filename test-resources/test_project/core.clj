(ns test-project.core
  (:require [test-project.deps.a :as a]
            [test-project.deps.d :as d]
            [test-project.deps.e :refer [my-function other-function]]
            [test-project.deps.f :as f :refer :all]
            [test-project.deps.g :refer :all]
            [test-project.deps.h :refer :all])
  (:use [test-project.deps.c :only [main]]
        [test-project.deps.i]
        [test-project.deps.j :only [configuration j-function]]
        [test-project.deps.k :rename {main k-main}]))

(defn- square [x]
  (* x x))

(defn- square-sum [& xs]
  (apply + (map square xs)))

(defn execute []
  (a/foo)
  (other-function)
  (f/f-function)
  (g-function)
  (i-function)
  (j-function)
  (k-main))

(defn increment-first [x y]
  (inc x))
