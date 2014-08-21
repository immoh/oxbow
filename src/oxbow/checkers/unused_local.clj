(ns oxbow.checkers.unused-local)

(defn- format-result [ns sym]
  (let [{:keys [line column]} (meta sym)]
    {:type   :unused-local
     :ns     ns
     :symbol sym
     :line   line
     :column column}))

(defn- get-unused-locals [{:keys [ns unused-locals]}]
  (map (partial format-result ns) unused-locals))

(defn check
  ([analyzed-nses]
   (check analyzed-nses {}))
  ([analyzed-nses opts]
   (mapcat get-unused-locals analyzed-nses)))