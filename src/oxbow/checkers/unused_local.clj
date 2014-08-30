(ns oxbow.checkers.unused-local)

(defn- exclude-symbol? [sym]
  (when sym
    (some #(re-matches % (name sym)) [#"&.*" #"_.*"])))

(defn- format-result [ns sym]
  (let [{:keys [line column]} (meta sym)]
    {:type   :unused-local
     :ns     ns
     :symbol sym
     :line   line
     :column column}))

(defn- get-unused-locals [{:keys [ns unused-locals]}]
  (->> unused-locals
       (remove exclude-symbol?)
       (map (partial format-result ns))))

(defn check
  ([analyzed-nses]
   (check analyzed-nses {}))
  ([analyzed-nses opts]
   (mapcat get-unused-locals analyzed-nses)))
