(ns oxbow.namespace.body-test
  (:use midje.sweet)
  (:require [oxbow.namespace.body :as ns-body]
            [oxbow.namespace.test-macros :refer [form->string]]))

(fact "Resolves referred symbols"
  (binding [*ns* *ns*]
    (ns test (:require [clojure.set :refer [difference]]))
    (ns-body/analyze ['(difference #{1 2} #{1})])) => {:resolved-symbols {'difference 'clojure.set}})

(fact "Doesn't resolve namespace qualified symbols"
  (binding [*ns* *ns*]
    (ns test (:require clojure.set))
    (ns-body/analyze ['(clojure.set/difference #{1 2} #{1})]) => {:resolved-symbols {}}))

(fact "Doesn't resolve locals"
  (binding [*ns* *ns*]
    (ns test (:require [clojure.set :refer [difference]]))
    (ns-body/analyze ['(let [difference 1] (inc difference))]) =not=> (just {:resolved-symbols (contains {'difference anything})})))

(fact "Doesn't choke on unqualified Java class names"
  (ns-body/analyze ['(handle-class System)]) => {:resolved-symbols {}})

(fact "Handles macro calls with special forms"
  (ns-body/analyze ['(form->string (catch [:foo] {} nil))]) => {:resolved-symbols {'form->string 'oxbow.namespace.test-macros}})
