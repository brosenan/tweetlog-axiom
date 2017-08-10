(defproject tweetlog "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.562" :scope "provided"]
                 [ring "1.6.1"] ;; Required for figwheel
                 [reagent "0.6.0"]
                 [axiom-clj/permacode "MONOLITH-SNAPSHOT"]
                 [axiom-clj/axiom-cljs "MONOLITH-SNAPSHOT"]]

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-environ "1.1.0"]
            [midje "1.8.3"]
            [axiom-clj/lein-axiom "MONOLITH-SNAPSHOT"]
            [axiom-clj/permacode "MONOLITH-SNAPSHOT"]]

  :min-lein-version "2.6.1"

  :source-paths ["src/clj" "src/cljs"]

  :test-paths ["test/clj" "test/cljs"]

  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]

  ;; Use `lein run` if you just want to start a HTTP server, without figwheel
  :main tweetlog.application

  ;; nREPL by default starts in the :main namespace, we want to start in `user`
  ;; because that's where our development helper functions like (go) and
  ;; (browser-repl) live.
  :repl-options {:init-ns user}

  :cljsbuild {:builds
              [{:id "app"
                :source-paths ["src/cljs" "dev"]

                :figwheel {:on-jsload "tweetlog.system/reset"}

                :compiler {:main cljs.user
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/tweetlog.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}

               {:id "test"
                :source-paths ["src/cljs" "test/cljs"]
                :compiler {:output-to "resources/public/js/compiled/testable.js"
                           :main tweetlog.test-runner
                           :optimizations :none}}

               {:id "min"
                :source-paths ["src/cljs"]
                :jar true
                :compiler {:main tweetlog.system
                           :output-to "resources/public/js/compiled/tweetlog.js"
                           :output-dir "target"
                           :source-map-timestamp true
                           :optimizations :advanced
                           :pretty-print false}}]}

  ;; When running figwheel from nREPL, figwheel will read this configuration
  ;; stanza, but it will read it without passing through leiningen's profile
  ;; merging. So don't put a :figwheel section under the :dev profile, it will
  ;; not be picked up, instead configure figwheel here on the top level.

  :figwheel {;; :http-server-root "public"       ;; serve static assets from resources/public/
             ;; :server-port 3449                ;; default
             ;; :server-ip "127.0.0.1"           ;; default
             :css-dirs ["resources/public/css"]  ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process. We
             ;; don't do this, instead we do the opposite, running figwheel from
             ;; an nREPL process, see
             ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
             ;; :nrepl-port 7888

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             :server-logfile "log/figwheel.log"}

  :doo {:build "test"}

  :profiles {:dev
             {:dependencies [[figwheel "0.5.10"]
                             [figwheel-sidecar "0.5.10"]
                             [com.cemerick/piggieback "0.2.1"]
                             [org.clojure/tools.nrepl "0.2.13"]
                             [lein-doo "0.1.7"]
                             [reloaded.repl "0.2.3"]
                             [midje "1.8.3"]
                             [axiom-clj/cloudlog "MONOLITH-SNAPSHOT"]
                             [axiom-clj/cloudlog-events "MONOLITH-SNAPSHOT"]]

              :plugins [[lein-figwheel "0.5.10"]
                        [lein-doo "0.1.7"]]

              :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  
  :axiom-run-config
  {:zookeeper-config {:url "127.0.0.1:2181"}
   :zk-plan-config {:num-threads 5
                    :parent "/my-plans"}
   :migration-config {:number-of-shards 3
                      :plan-prefix "/my-plans"
                      :clone-location "/tmp"
                      :clone-depth 10}
   :local-storm-cluster true
   :fact-spout {:include [:rabbitmq-config]}
   :store-bolt {:include [:dynamodb-event-storage-num-threads
                          :dynamodb-default-throughput
                          :dynamodb-config]}
   :output-bolt {:include [:rabbitmq-config]}
   :initlal-link-bolt {:include [:storage-local-path
                                 :storage-fetch-url]}
   :link-bolt {:include [:storage-local-path
                         :storage-fetch-url
                         :dynamodb-config
                         :dynamodb-default-throughput
                         :num-database-retriever-threads]}
   :use-dummy-authenticator true ;; Completely remove this entry to avoid the dummy authenticator
   :dummy-version "dev-705491"
   :http-config {:port 8080}}

  :axiom-deploy-config
  {:storage-local-path "/tmp/axiom-perms"
   :storage-fetch-url "https://s3.amazonaws.com/brosenan-test"
   :rabbitmq-config {:username "guest"
                     :password "guest"
                     :vhost "/"
                     :host "localhost"
                     :port 5672}
   :dynamodb-config {:access-key "STANDALONE-DB"
                     :secret-key "XXYY"
                     :endpoint "http://localhost:8006"}
   :num-database-retriever-threads 1
   :dynamodb-default-throughput {:read 1 :write 1}
   :dynamodb-event-storage-num-threads 3})
