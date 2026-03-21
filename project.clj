(defproject quadvote "0.0.1"
  :dependencies [[org.clojure/clojure "1.12.1"]
                 [org.clojure/clojurescript "1.12.134" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [io.bloomventures/omni "0.36.3"]
                 [io.bloomventures/commons "0.17.1"]
                 [com.github.rafd/sys "0.3.2"]
                 [com.github.rafd/dat "0.0.1-20260321-0"]

                 ;; [com.hyperfiddle/rcf "20220926-202227"] from commons

                 [com.taoensso/telemere "1.2.1"]

                 [com.draines/postal "2.0.3"]
                 [datascript "1.7.8"]
                 [markdown-clj "1.11.4"]
                 [jarohen/chime "0.3.2"]
                 [tada "0.3.1"]]
  :main quadvote.core

  :plugins [[io.bloomventures/omni "0.36.3"]]
  :omni-config quadvote.omni-config/omni-config

  :profiles {:dev {:source-paths ["src" "dev-src"]
                   :dependencies [[org.clojure/data.csv "1.1.1"]]}
             :uberjar {:aot [quadvote.core]
                       :prep-tasks [["omni" "compile"]
                                    "compile"]}})
