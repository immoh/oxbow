(ns oxbow.checkers.ns-var)

(defn- check-ns-intern [ns used-vars [symbol var]]
  (let [{:keys [line column]} (meta var)]
    (when (and line (not (used-vars var)))
      {:type   :unused-var
       :ns     ns
       :symbol symbol
       :line   line
       :column column})))

(defn- private? [[_ var]]
  (:private (meta var)))

(defn- check-ns [used-vars {:keys [ns interns api-namespace?]}]
  (keep (partial check-ns-intern ns used-vars)
        (if api-namespace? (filter private? interns) interns)))

(defn- get-used-vars [analyzed-nses]
  (set (mapcat (comp vals :symbols-to-vars) analyzed-nses)))

(defn- api-namespace? [{:keys [api-namespaces]} ns]
  (and api-namespaces (api-namespaces ns)))

(defn check
  ([analyzed-nses]
   (check analyzed-nses {}))
  ([analyzed-nses opts]
   (mapcat (partial check-ns (get-used-vars analyzed-nses))
           (map #(assoc % :api-namespace? (api-namespace? opts (:ns %))) analyzed-nses))))
