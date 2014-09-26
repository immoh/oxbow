(ns oxbow.walk)
;; local analysis for empty binding forms

(def ^{:dynamic true :private true} *locals* (atom nil))

(defn locals []
  (when *locals*
    @*locals*))

(let [a (atom 0)]
  (defn unique-id []
    (swap! a inc)))

(defn with-lexical-context* [f]
  (binding [*locals* (atom (locals))]
    (f)))

(defmacro with-lexical-context [& body]
  `(with-lexical-context* (fn [] ~@body)))

(defn register-locals [& x]
  (swap! *locals* merge (zipmap x (repeatedly unique-id))))

(defmulti handle-special-form (fn [_ x] (first x)))

(defn handle-let-and-loop [f x]
  (let [[sym bindings & body] x]
    (f sym)
    (doseq [[k v] (partition 2 bindings)]
      (f k)
      (register-locals (first bindings))
      (f v))
    (dorun (map f body))))

(defmethod handle-special-form 'let* [f x]
  (handle-let-and-loop f x))

(defmethod handle-special-form 'loop* [f x]
  (handle-let-and-loop f x))

(defmethod handle-special-form 'fn* [f x]
  (let [[[fn' fn-name] bodies] (split-with (complement sequential?) x)]
    (f fn')
    (when fn-name
      (f fn-name)
      (register-locals fn-name))
    (doseq [[args & body] (if (vector? (first bodies)) (list bodies) bodies)]
      (dorun (map f args))
      (with-lexical-context
        (apply register-locals args)
        (dorun (map f body))))))

(defmethod handle-special-form 'def [f x]
  (let [[def' sym & remainder] x]
    (f def')
    (f sym)
    (register-locals sym)
    (dorun (map f remainder))))

(defn- split-try-catch-finally [x]
  (partition-by #(when (seq? %)
                  (#{'catch 'finally} (first %)))
                x))

(defmethod handle-special-form 'try [f x]
  (let [[try-exprs catch-clauses finally-clauses] (split-try-catch-finally x)]
    (dorun (map f try-exprs))
    (doseq [[catch' e-class e-sym & body] catch-clauses]
      (dorun (map f [catch' e-class e-sym]))
      (with-lexical-context
        (register-locals e-sym)
        (dorun (map f body))))
    (dorun (map f finally-clauses))))

(defn- handle-method [f [method-name args & body]]
  (dorun (map f [method-name args]))
  (with-lexical-context
    (apply register-locals args)
    (dorun (map f body))))

(defmethod handle-special-form 'deftype* [f x]
  (let [[deftype' type resolved-type fields implements' interfaces & methods] x]
    (dorun (map f [deftype' type resolved-type fields]))
    (apply register-locals fields)
    (dorun (map f [implements' interfaces]))
    (dorun (map (partial handle-method f) methods))))

(defmethod handle-special-form 'reify* [f x]
  (let [[reify' classes & methods] x]
    (dorun (map f [reify' classes]))
    (dorun (map (partial handle-method f) methods))))

(defmethod handle-special-form :default [f x]
  (dorun (map f x)))

(defn walk! [f x]
  (let [x' (if (seq? x) (macroexpand-1 x) x)]
    (when-not (identical? x x')
      (f (first x)))
    (f x')
    (cond
      (and (seq? x') (special-form? (first x'))) (with-lexical-context
                                                   (handle-special-form (partial walk f) x'))
      (coll? x')     (dorun (map (partial walk f) x')))))

#_(walk prn '(defn foo [x] 1))

#_(defn print-env [x]
  (prn x)
  (prn (locals))
  (println))

#_(walk print-env '(let [x 1] (let [x 2] x)))

#_(walk print-env '(fn foo ([x] 5) ([x y] (+ x y))))

#_(println "----")

#_(walk identity '(fn moi [x] 1))


#_(walk print-env '(def x "this is 5" 5))

#_(walk print-env '(try
                  1
                  (+ 1 1)
                  (catch RuntimeException r
                    2
                    (+ 2 2))
                  (catch Exception e
                    3
                    (+ 3 3))
                  (finally
                    4
                    (+ 4 4))))

#_(walk print-env '(deftype Foo [x] Object (toString [this] "moi")))

#_(walk print-env '(reify java.util.Date (toString [foo] "moi")))



