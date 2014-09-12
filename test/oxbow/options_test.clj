(ns oxbow.options-test
  (:use midje.sweet)
  (:require [oxbow.options :as options]))

(tabular
  (fact "Foobar"
    (options/api-namespace? {:api-namespaces ?option-value} ?ns-symbol) => ?expected-result)
  ?option-value                  ?ns-symbol       ?expected-result
  nil                            'clojure.core    falsey
  'clojure.core                  'clojure.core    truthy
  'clojure.core                  'clojure.test    falsey
  "clojure.core"                 'clojure.core    truthy
  "clojure.core"                 'clojure.test    falsey
  ['clojure.core "clojure.set"]  'clojure.core    truthy
  ['clojure.core "clojure.set"]  'clojure.set     truthy
  ['clojure.core "clojure.set"]  'clojure.test    falsey)
