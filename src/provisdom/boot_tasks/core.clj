(ns provisdom.boot-tasks.core
  {:boot/export-tasks true}
  (:require
    [boot.util :as util]
    [boot.core :refer :all]
    [boot.task.built-in :refer :all]
    [adzerk.boot-cljs :refer [cljs]]
    [cljsjs.boot-cljsjs :refer [from-cljsjs]]))

(deftask asset-paths
         "Set :asset-paths"
         [a asset-paths PATHS #{str} ":asset-paths"]
         (set-env! :asset-paths asset-paths)
         (with-pre-wrap fileset
                        fileset))

(deftask build
         "Publish library to local repo"
         []
         (comp (pom)
               (jar)
               (install)))

(deftask publish
         "Publish library and offer command line options (for wercker)"
         [u access-key VALUE str "Access key for rep"
          p secret-key VALUE str "Secret key for repo"
          r repo-uri VALUE str "The repo uri"]
         (push :repo-map {:url        (or repo-uri "s3p://provisdom-artifacts/releases/")
                          :username   (or access-key (System/getenv "AWS_ACCESS_KEY"))
                          :passphrase (or secret-key (System/getenv "AWS_SECRET_KEY"))}))

(deftask release
         "Publish library to S3"
         [u access-key VALUE str "Access key for rep"
          p secret-key VALUE str "Secret key for repo"
          r repo-uri VALUE str "The repo uri"]
         (comp
           (pom)
           (jar)
           (push :repo-map {:url        (or repo-uri "s3p://provisdom-artifacts/releases/")
                            :username   (or access-key (System/getenv "AWS_ACCESS_KEY"))
                            :passphrase (or secret-key (System/getenv "AWS_SECRET_KEY"))})))

(deftask run-jar
         "execute a jar file"
         [j jarfile PATH str "jar file to run"]
         (let [process (atom nil)]

           (cleanup
             (util/info "\n<< stopping %s>>\n" jarfile)
             (.destroy @process))

           (with-pre-wrap fileset
                          (util/info "\n<<  starting %s >>\n" jarfile)
                          (reset! process (.exec (Runtime/getRuntime) (str "java -jar " jarfile)))
                          (future (clojure.java.io/copy (.getInputStream @process) System/out))
                          (future (clojure.java.io/copy (.getErrorStream @process) System/err))
                          fileset)))

(deftask docker-compose
         "execute docker-compose"
         []
         (let [process (atom nil)]

           (cleanup
             (util/info "\n<< stopping docker-compose>>\n")
             (reset! process (.exec (Runtime/getRuntime) "docker-compose stop"))
             (future (clojure.java.io/copy (.getInputStream @process) System/out))
             (future (clojure.java.io/copy (.getErrorStream @process) System/err)))

           (with-pre-wrap fileset
                          (util/info "\n<<  starting  >>\n")
                          (reset! process (.exec (Runtime/getRuntime) "docker-compose start"))
                          (future (clojure.java.io/copy (.getInputStream @process) System/out))
                          (future (clojure.java.io/copy (.getErrorStream @process) System/err))
                          fileset)))