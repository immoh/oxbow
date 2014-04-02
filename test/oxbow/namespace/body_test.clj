(ns oxbow.namespace.body-test
  (:use midje.sweet)
  (:require [oxbow.namespace.body :as ns-body]
            [oxbow.namespace.test-macros :refer [form->string]]))

(fact "Resolves referred symbols"
  (binding [*ns* *ns*]
    (ns test (:require [clojure.set :refer [difference]]))
    (ns-body/analyze ['(difference #{1 2} #{1})])) => {:symbols-to-vars {'difference #'clojure.set/difference}})

(fact "Doesn't resolve locals"
  (binding [*ns* *ns*]
    (ns test (:require [clojure.set :refer [difference]]))
    (ns-body/analyze ['(let [difference 1] (inc difference))]) =not=> (just {:symbols-to-vars (contains {'difference anything})})))

(fact "Doesn't choke on unqualified Java class names"
  (ns-body/analyze ['(handle-class System)]) => {:symbols-to-vars {}})

(fact "Doesn't choke on ns names"
  (ns-body/analyze ['(handle-ns clojure.core.async)]) => {:symbols-to-vars {}})

(fact "Handles macro calls with special forms"
      (ns-body/analyze ['(form->string (catch [:foo] {} nil))]) => {:symbols-to-vars {'form->string #'oxbow.namespace.test-macros/form->string}})
