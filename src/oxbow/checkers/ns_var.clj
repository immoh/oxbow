(ns oxbow.checkers.ns-var)

(defn- check-ns-intern [ns used-vars [symbol var]]
  (when-not (used-vars var)
    {:type   :unused-var
     :ns     ns
     :symbol symbol}))

(defn- check-ns [used-vars {:keys [ns interns]}]
  (keep (partial check-ns-intern ns used-vars) interns))

(defn- get-used-vars [analyzed-nses]
  (set (mapcat (comp vals :symbols-to-vars) analyzed-nses)))

(defn check [analyzed-nses]
  (mapcat (partial check-ns (get-used-vars analyzed-nses)) analyzed-nses))
