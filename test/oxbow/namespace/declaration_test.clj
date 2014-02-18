(ns oxbow.namespace.declaration-test
  (:use midje.sweet)
  (:require [oxbow.namespace.declaration :as ns-decl]))

(fact "Namespace declaration is analyzed correctly"
  (ns-decl/analyze '(ns my.core
                      (:require my.deps.first)
                      (:use my.deps.second)))
  => {:ns 'my.core
      :requires [{:ns 'my.deps.first  :alias 'my.deps.first  :spec 'my.deps.first}]
      :uses     [{:ns 'my.deps.second :alias 'my.deps.second :spec 'my.deps.second}]})


(fact "Require alias is analyzed correctly"
  (ns-decl/analyze '(ns my.core
                      (:require [my.deps.first :as first])))
  => (contains {:requires [{:ns 'my.deps.first :alias 'first :spec '[my.deps.first :as first]}]}))

(fact "Multiple requires are analyzed corretcly"
  (ns-decl/analyze '(ns my.core
                      (:require [my.deps.first :as first]
                                [my.deps.second :as second])))
  => (contains {:requires (contains (contains {:ns 'my.deps.first}) (contains {:ns 'my.deps.second}))}))
