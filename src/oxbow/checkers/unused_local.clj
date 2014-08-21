(ns oxbow.checkers.unused-local)

(defn- format-result [ns sym]
  {:type  :unused-local
   :ns     ns
   :symbol sym})

(defn- get-unused-locals [{:keys [ns unused-locals]}]
  (map (partial format-result ns) unused-locals))

(defn check
  ([analyzed-nses]
   (check analyzed-nses {}))
  ([analyzed-nses opts]
   (mapcat get-unused-locals analyzed-nses)))