(defproject oxbow "0.1.0-SNAPSHOT"
  :description "I see dead code. All the time. Everywhere."
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [riddley "0.1.6"]]
  :plugins [[lein-midje "3.1.3"]]
  :profiles {:dev {:dependencies [[midje "1.6.0"]]
                   :source-paths ["test-resources/"]}})
