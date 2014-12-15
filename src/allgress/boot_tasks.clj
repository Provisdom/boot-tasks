(ns allgress.boot-tasks
  {:boot/export-tasks true}
  (:require
    [boot.pod :as pod]
    [boot.util :as util]
    [boot.core :refer :all]
    [boot.task.built-in :refer :all]
    [clojure.java.io :as io]
    [boot.task-helpers :as helpers]
    [adzerk.boot-cljs :refer [cljs]]))

(deftask serve
         "Start a web server on localhost and serve a directory.

          If no directory is specified the current one is used.  Listens on
          port 3000 by default."
         [d dir PATH str "The directory to serve."
          p port PORT int "The port to listen on."]
         (let [worker (pod/make-pod {:dependencies '[[ring/ring-jetty-adapter "1.3.1"]
                                                     [compojure "1.2.1"]]})
               dir (or dir ".")
               port (or port 3000)]
           (cleanup
             (util/info "<< stopping Jetty... >>")
             (pod/eval-in worker (.stop server)))
           (with-pre-wrap
             (pod/eval-in worker
                          (require '[ring.adapter.jetty :refer [run-jetty]]
                                   '[compojure.handler :refer [site]]
                                   '[compojure.route :refer [files]])
                          (def server (run-jetty (files "/" {:root ~dir}) {:port ~port :join? false})))
             (util/info "<< started web server on http://localhost:%d (serving: %s) >>\n" port dir))))

(deftask sync-target
         "Sync directory contents with a copy in target."
         [d dirs PATHS #{[str str]} "Paths to directories to sync"
          t target TARGET str "Target folder"]
         (let [target (or target "target/")]
           (with-pre-wrap
             (doseq [[src dest] dirs]
               (util/info "Sync %s to %s\n" src (str target dest))
               (add-sync! (str target dest) [src])))))

(deftask cljs-testable
         "compile cljs including tests"
         []
         (cljs :output-to "testable.js" :optimizations :whitespace))

(deftask cljs-test
         "run cljs tests"
         []
         (with-pre-wrap
           (let [testable (first (by-name ["testable.js"] (tgt-files)))
                 runner (io/resource "runner.js")
                 runner-path (str (get-env :tgt-path) "/runner.js")]
             (spit runner-path (slurp runner))
             (when testable
               (helpers/dosh "phantomjs" runner-path (.getPath testable))))))

(deftask build
         "Publish released library to s3 and local repo"
         []
         (comp (pom)
               (add-src)
               (jar)
               (install)))

(deftask release
         "Publish released library to s3 and local repo"
         []
         (comp (build)
               (push)))