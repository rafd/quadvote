(defproject quadvote "0.0.1"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.764" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [io.bloomventures/omni "0.29.1"]
                 [io.bloomventures/commons "0.12.1"]
                 [com.taoensso/nippy "3.6.0"]
                 [duratom "0.5.9"]
                 [re-frame "0.10.5"]
                 [tada "0.2.2"]]
  :main quadvote.core
  :profiles {:dev {:source-paths ["src" "dev-src"]}})
