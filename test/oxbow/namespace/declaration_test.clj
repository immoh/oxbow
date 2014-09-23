(ns oxbow.namespace.declaration-test
  (:use midje.sweet)
  (:require [oxbow.namespace.declaration :as ns-decl]))

(fact "Namespace declaration is analyzed correctly"
  (ns-decl/analyze '(ns my.core
                      (:require my.deps.first)
                      (:use my.deps.second)
                      (:import java.util.Date)))
  => {:ns 'my.core
      :deps [{:type :require :ns    'my.deps.first  :spec 'my.deps.first}
             {:type :use     :ns    'my.deps.second :spec 'my.deps.second}
             {:type :import  :class 'java.util.Date :spec 'java.util.Date}]})

(fact "Require alias is analyzed correctly"
  (ns-decl/analyze '(ns my.core
                      (:require [my.deps.first :as first])))
  => (contains {:deps [{:type :require :ns 'my.deps.first :as 'first :spec '[my.deps.first :as first]}]}))

(fact "Multiple requires are analyzed corretcly"
  (ns-decl/analyze '(ns my.core
                      (:require [my.deps.first :as first]
                                [my.deps.second :as second])))
  => (contains {:deps (contains (contains {:type :require :ns 'my.deps.first})
                                (contains {:type :require :ns 'my.deps.second}))}))

(fact "Sequential require libspec without alias is analyzed correctly"
  (ns-decl/analyze '(ns my.core
                      (:require [my.deps.first])))
  => (contains {:deps [{:type :require :ns 'my.deps.first :spec '[my.deps.first]}]}))

(fact "Prefixed libspecs are analyzed correctly"
  (ns-decl/analyze '(ns my.core
                      (:require [my.deps [first :as first]
                                         [second :as second]])))
  => (contains {:deps [{:type :require :ns 'my.deps.first  :as 'first :spec '[my.deps [first :as first]
                                                                                      [second :as second]]}
                       {:type :require :ns 'my.deps.second :as 'second :spec '[my.deps [first :as first]
                                                                                       [second :as second]]}]}))

(fact "Multiple imports are analyzed correctly"
  (ns-decl/analyze '(ns my.core
                      (:import java.util.Date java.util.Calendar)))
  => (contains {:deps [{:type :import :class 'java.util.Date     :spec 'java.util.Date}
                       {:type :import :class 'java.util.Calendar :spec 'java.util.Calendar}]}))

(fact "Prefixed imports are analyzed correctly"
  (ns-decl/analyze '(ns my.core
                      (:import [java.util Date Calendar])))
  => (contains {:deps [{:type :import :class 'java.util.Date     :spec '[java.util Date Calendar]}
                       {:type :import :class 'java.util.Calendar :spec '[java.util Date Calendar]}]}))
