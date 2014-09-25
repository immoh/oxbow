(ns oxbow.walk)

;; locals
;; - def
;; - try-catch
;; - reify
;; - deftype


;; macroexpand-1 doesn't preserve meta

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
  (let [[def' sym] x
        init (last x)]
    (f def')
    (f sym)
    (register-locals sym)
    (f init)))


(defn walk [f x]
  #_(println "walk:" x)
  (let [x' (if (seq? x) (macroexpand-1 x) x)]

    (when-not (identical? x x')
      ; call for first symbol before macroexpansion
      (f (first x)))

    (f x')

    (cond
      (and (seq? x') (get (methods handle-special-form) (first x')))
      (with-lexical-context
        (handle-special-form (partial walk f) x'))

      (coll? x')
      (dorun (map (partial walk f) x')))))

(println "---------------")
(println)

#_(walk prn '(defn foo [x] 1))

(defn print-env [x]
  (prn x)
  (prn (locals))
  (println))

#_(walk print-env '(let [x 1] (let [x 2] x)))

#_(walk print-env '(fn foo ([x] 5) ([x y] (+ x y))))

#_(println "----")

#_(walk identity '(fn moi [x] 1))


(walk print-env '(def x "this is 5" 5))

