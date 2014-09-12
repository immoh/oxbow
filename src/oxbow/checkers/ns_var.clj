(ns oxbow.checkers.ns-var
  (:require [oxbow.options :as options]))

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

(defn- check-ns [opts used-vars {:keys [ns interns]}]
  (keep (partial check-ns-intern ns used-vars)
        (if (options/api-namespace? opts ns)
          (filter private? interns)
          interns)))

(defn- get-used-vars [analyzed-nses]
  (set (mapcat (comp vals :symbols-to-vars) analyzed-nses)))

(defn- api-namespace? [{:keys [api-namespaces]} ns]
  (and api-namespaces (api-namespaces ns)))

(defn check
  ([analyzed-nses]
   (check analyzed-nses {}))
  ([analyzed-nses opts]
   (mapcat (partial check-ns opts (get-used-vars analyzed-nses))analyzed-nses)))
