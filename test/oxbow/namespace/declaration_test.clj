(ns oxbow.namespace.declaration-test
  (:use midje.sweet)
  (:require [oxbow.namespace.declaration :as ns-decl]))

(fact "Namespace declaration is analyzed correctly"
  (ns-decl/analyze '(ns my.core
                      (:require my.deps.first)
                      (:use my.deps.second)))
  => {:ns 'my.core
      :deps [{:type :require :ns 'my.deps.first  :spec 'my.deps.first}
             {:type :use     :ns 'my.deps.second :spec 'my.deps.second}]})

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
