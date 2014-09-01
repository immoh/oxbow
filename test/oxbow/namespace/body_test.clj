(ns oxbow.namespace.body-test
  (:use midje.sweet)
  (:require [oxbow.namespace.body :as ns-body]
            [oxbow.namespace.test-macros :refer [form->string]]))

(fact "Resolves referred symbols"
  (binding [*ns* *ns*]
    (ns test (:require [clojure.set :refer [difference]]))
    (ns-body/analyze ['(difference #{1 2} #{1})])) => (contains {:symbols-to-vars {'difference #'clojure.set/difference}}))

(fact "Doesn't resolve locals"
  (binding [*ns* *ns*]
    (ns test (:require [clojure.set :refer [difference]]))
    (ns-body/analyze ['(let [difference 1] (inc difference))]) =not=> (contains {:symbols-to-vars (contains {'difference anything})})))

(fact "Doesn't choke on unqualified Java class names"
  (ns-body/analyze ['(handle-class System)]) => (contains {:symbols-to-vars {}}))

(fact "Doesn't choke on ns names"
  (ns-body/analyze ['(handle-ns clojure.core.async)]) => (contains {:symbols-to-vars {}}))

(fact "Handles macro calls with special forms"
  (ns-body/analyze ['(form->string (catch [:foo] {} nil))]) => (contains {:symbols-to-vars {'form->string #'oxbow.namespace.test-macros/form->string}}))

(fact "Analyzes inside Java interop calls"
  (binding [*ns* *ns*]
    (ns test (:require [clojure.set :refer [difference]]))
    (ns-body/analyze ['(.toString (difference #{1 2} #{1}))]) => (contains {:symbols-to-vars (just {'difference anything})})))

(fact "Doesn't choke on defprotocols"
  (ns-body/analyze ['(defprotocol Foo)]) => anything)

(fact "Doesn't report unused symbols for case form with default value"
  (ns-body/analyze-form '(case x 0 0 nil)) => (contains {:unused-locals nil}))
