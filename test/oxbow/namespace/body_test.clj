(ns oxbow.namespace.body-test
  (:use midje.sweet)
  (:require [oxbow.namespace.body :as ns-body]
            [oxbow.namespace.test-macros :refer [form->string]]))

(fact "Resolves referred symbols"
  (binding [*ns* *ns*]
    (ns test (:require [clojure.set :refer [difference]]))
    (ns-body/analyze ['(difference #{1 2} #{1})])) => (contains {:resolved-symbols {'difference #'clojure.set/difference}}))

(fact "Doesn't resolve locals"
  (binding [*ns* *ns*]
    (ns test (:require [clojure.set :refer [difference]]))
    (ns-body/analyze ['(let [difference 1] (inc difference))]) =not=> (contains {:resolved-symbols (contains {'difference anything})})))

(fact "Doesn't choke on unqualified Java class names"
  (ns-body/analyze ['(handle-class System)]) => (contains {:resolved-symbols {'System java.lang.System}}))

(fact "Doesn't choke on ns names"
  (ns-body/analyze ['(handle-ns clojure.core.async)]) => (contains {:resolved-symbols {}}))

(fact "Handles macro calls with special forms"
  (ns-body/analyze ['(form->string (catch [:foo] {} nil))]) => (contains {:resolved-symbols {'form->string #'oxbow.namespace.test-macros/form->string}}))

(fact "Analyzes inside Java interop calls"
  (binding [*ns* *ns*]
    (ns test (:require [clojure.set :refer [difference]]))
    (ns-body/analyze ['(.toString (difference #{1 2} #{1}))]) => (contains {:resolved-symbols (just {'difference anything})})))

(fact "Reports unused locals for binding forms with empty body"
  (ns-body/analyze-form '(let [x 1])) => (contains {:unused-locals '(x)})
  (ns-body/analyze-form '(fn [x])) => (contains {:unused-locals '(x)})
  (ns-body/analyze-form '(loop [x 1])) => (contains {:unused-locals '(x)})
  (ns-body/analyze-form '(letfn [(f [])])) => (contains {:unused-locals '(f)})
  (ns-body/analyze-form '(try 1 (catch Exception e))) => (contains {:unused-locals '(e)})
  (ns-body/analyze-form '(reify Object (toString [this]))) => (contains {:unused-locals '(this)}))
