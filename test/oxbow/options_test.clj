(ns oxbow.options-test
  (:use midje.sweet)
  (:require [oxbow.options :as options]))

(fact "Option :api-namespaces is always parsed to set of namespace symbols"
  (options/parse {:api-namespaces nil}) => nil
  (options/parse {:api-namespaces "clojure.core"}) => {:api-namespaces #{'clojure.core}}
  (options/parse {:api-namespaces 'clojure.core}) => {:api-namespaces #{'clojure.core}}
  (options/parse {:api-namespaces ["clojure.core"]}) => {:api-namespaces #{'clojure.core}}
  (options/parse {:api-namespaces ['clojure.core]}) => {:api-namespaces #{'clojure.core}}
  (options/parse {:api-namespaces ['clojure.core "clojure.set"]}) => {:api-namespaces #{'clojure.core 'clojure.set}})
