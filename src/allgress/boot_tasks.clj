(ns allgress.boot-tasks
  {:boot/export-tasks true}
  (:require
    [boot.pod :as pod]
    [boot.util :as util]
    [boot.core :refer :all]))

(core/deftask serve
         "Start a web server on localhost and serve a directory.

          If no directory is specified the current one is used.  Listens on
          port 3000 by default."
         [d dir PATH str "The directory to serve."
          p port PORT int "The port to listen on."]
         (let [worker (pod/make-pod {:dependencies '[[ring/ring-jetty-adapter "1.3.1"]
                                                     [compojure "1.2.1"]]})
               dir (or dir ".")
               port (or port 3000)]
           (core/cleanup
             (util/info "<< stopping Jetty... >>")
             (pod/eval-in worker (.stop server)))
           (with-pre-wrap
             (pod/eval-in worker
                          (require '[ring.adapter.jetty :refer [run-jetty]]
                                   '[compojure.handler :refer [site]]
                                   '[compojure.route :refer [files]])
                          (def server (run-jetty (files "/" {:root ~dir}) {:port ~port :join? false})))
             (core/add-sync! (str dir "bower_components/") ["bower_components/"])
             (util/info "<< started web server on http://localhost:%d (serving: %s) >>\n" port dir))))


