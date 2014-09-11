(ns oxbow.core-test
  (:use midje.sweet)
  (:require [oxbow.core :as core]))

(def check-test-project (memoize (fn
                                   ([] (core/check ["test-resources/test_project"]))
                                   ([opts] (core/check ["test-resources/test_project"] opts)))))

(defn- symbol-matching-regex [re]
  (fn [sym]
    (re-matches re (name sym))))

(facts "About dead-ns checker"

  (fact "Namespaces that are not referenced from anywhere are reported as dead"
    (check-test-project) => (contains {:type :dead-ns
                                       :name 'test-project.deps.b}))

  (fact "Namespaces imported with :require  are not reported as dead"
    (check-test-project) =not=> (contains (contains {:type :dead-ns
                                                     :name 'test-project.deps.a})))

  (fact "Namespaces imported with :use are not reported as dead"
    (check-test-project) =not=> (contains (contains {:type :dead-ns
                                                     :name 'test-project.deps.c})))

  (fact "API Namespaces are not reported as dead"
    (check-test-project {:api-namespaces ['test-project.api]}) =not=> (contains {:type :dead-ns
                                                                                 :name  'test-project.api})))


(facts "About ns-dependency checker"

  (fact "Unused require is reported"
    (check-test-project) => (contains {:type   :unused-ns-dependency
                                       :ns     'test-project.core
                                       :spec   '[test-project.deps.d :as d]
                                       :line   3
                                       :column 13})

    (check-test-project) => (contains {:type  :unused-ns-dependency
                                       :ns    'test-project.core
                                       :spec  '[test-project.deps.h :refer :all]
                                       :line   7
                                       :column 13}))

  (fact "Used require is not reported"
    (check-test-project) =not=> (contains (contains {:type :unused-ns-dependency
                                                     :ns   'test-project.core
                                                     :spec (contains 'test-project.deps.a)})))

  (fact "Unused referred symbols from require are reported"
    (check-test-project) => (contains {:type    :unused-require-refer-symbols
                                       :ns      'test-project.core
                                       :spec    '[test-project.deps.e :refer [my-function other-function]]
                                       :symbol  'my-function
                                       :line     4
                                       :column   13}))

  (fact "Require :refer :all with no used symbols is reported as unused"
    (check-test-project) => (contains {:type   :unused-require-refer-symbols
                                       :ns     'test-project.core
                                       :spec   '[test-project.deps.f :as f :refer :all]
                                       :line   5
                                       :column 13}))

  (fact "Require :refer :all with at least one used symbol is not reported as unused"
    (check-test-project) =not=> (contains (contains {:type :unused-require-refer-symbols
                                                     :ns   'test-project.core
                                                     :spec '[test-project.deps.g :refer :all]})))

  (fact "Unused :use is reported"
    (check-test-project) => (contains {:type :unused-ns-dependency
                                       :ns     'test-project.core
                                       :spec   '[test-project.deps.c :only [main]]
                                       :line   9
                                       :column 9}))

  (fact "Used :use is not reported"
    (check-test-project) =not=> (contains (contains {:type :unused-ns-dependency
                                                     :ns   'test-project.core
                                                     :spec '[test-project.deps.i]})))

  (fact "Unused :use :only symbols are reported"
    (check-test-project) => (contains {:type     :unused-use-only-symbols
                                       :ns      'test-project.core
                                       :spec    '[test-project.deps.j :only [configuration j-function]]
                                       :symbol  'configuration
                                       :line    11
                                       :column  9}))

  (fact "Used :use symbol wth :rename is not reported"
    (check-test-project) =not=> (contains (contains {:type :unused-use-only-symbols
                                                     :ns   'test-project.core
                                                     :spec  (contains 'test-project.deps.k)}))))

(facts "About unused-var checker"

  (fact "Unused private var is reported"
    (check-test-project) => (contains {:type   :unused-var
                                       :ns     'test-project.core
                                       :symbol 'square-sum
                                       :line    17
                                       :column  1}))

  (fact "Used private var is not reported"
    (check-test-project) =not=> (contains (contains {:type   :unused-var
                                                     :ns     'test-project.core
                                                     :symbol 'square})))
  (fact "Unused public var is reported"
    (check-test-project) => (contains {:type   :unused-var
                                       :ns     'test-project.core
                                       :symbol 'execute
                                       :line   20
                                       :column 1}))

  (fact "Used public var is not reported"
    (check-test-project) =not=> (contains (contains {:type   :unused-var
                                                     :ns     'test-project.deps.a
                                                     :symbol 'foo})))
  (fact "Unused private var in API namespace is reported"
    (check-test-project {:api-namespaces ['test-project.api]}) => (contains {:type   :unused-var
                                                                             :ns     'test-project.api
                                                                             :symbol 'helper
                                                                             :line   3
                                                                             :column 1}))

  (fact "Unused public var in API namespace is not reported"
    (check-test-project {:api-namespaces ['test-project.api]}) =not=> (contains {:type   :unused-var
                                                                                 :ns     'test-project.api
                                                                                 :symbol 'execute}))

  (fact "Unused vars without position are not reported"
    (check-test-project) =not=> (contains (contains {:type :unused-var
                                                     :line nil}))))

(facts "About unused-local checker"
  (fact "Unused local is reported"
    (check-test-project) => (contains {:type   :unused-local
                                       :ns     'test-project.core
                                       :symbol 'y
                                       :line   29
                                       :column 15}))

  (fact "Used local is not reported"
    (check-test-project) =not=> (contains (contains {:type   :unused-local
                                                     :ns     'test-project.core
                                                     :symbol 'y
                                                     :line   30})))

  (fact "Locals beginning with ampersand (&) are not reported as unused"
    (check-test-project) =not=> (contains (contains {:type   :unused-local
                                                     :symbol (symbol-matching-regex #"&.*")})))

  (fact "Locals beginning with Undescore (_) are not reported as unused"
    (check-test-project) =not=> (contains (contains {:type   :unused-local
                                                     :symbol (symbol-matching-regex #"_.*")})))

  (fact "Generated symbols in syntax quoted forms are not reported as unused"
    (check-test-project) =not=> (contains (contains {:type   :unused-local
                                                     :symbol (symbol-matching-regex #".*__auto__")})))

  (fact "Symbols without location are not reported as unused"
    (check-test-project) =not=> (contains (contains {:type :unused-local
                                                     :line nil}))))
