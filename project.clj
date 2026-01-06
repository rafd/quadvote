(defproject quadvote "0.0.1"
  :dependencies [[org.clojure/clojure "1.12.1"]
                 [org.clojure/clojurescript "1.12.134" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [io.bloomventures/omni "0.34.1"]
                 [io.bloomventures/commons "0.15.1"]
                 [com.taoensso/nippy "3.6.0"]
                 [com.draines/postal "2.0.3"]
                 [markdown-clj "1.11.4"]
                 [duratom "0.5.9"]
                 [re-frame "0.10.5"]
                 [tada "0.2.2"]]
  :main quadvote.core

  :plugins [[io.bloomventures/omni "0.32.2"]]
  :omni-config quadvote.omni-config/omni-config

  :profiles {:dev {:source-paths ["src" "dev-src"]
                   :dependencies [[org.clojure/data.csv "1.1.1"]]}
             :uberjar {:aot [quadvote.core]
                       :prep-tasks [["omni" "compile"]
                                    "compile"]}})
