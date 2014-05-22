(ns oxbow.options)

(defmulti parse-option-value (fn [[option value]] option))

(defmethod parse-option-value :api-namespaces [[_ value]]
  (when value
    {:api-namespaces (if (coll? value)
                       (set (map symbol value))
                       #{(symbol value)})}))

(defmethod parse-option-value :default [& _] nil)

(defn parse [opts]
  (apply merge (keep parse-option-value opts)))
