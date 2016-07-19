(ns provisdom.boot-tasks.core
  {:boot/export-tasks true}
  (:require
    [boot.util :as util]
    [boot.core :as core]
    [boot.task.built-in :as bi]))

(core/deftask asset-paths
  "Set :asset-paths"
  [a asset-paths PATHS #{str} ":asset-paths"]
  (core/set-env! :asset-paths asset-paths)
  (core/with-pre-wrap fileset
                      fileset))

(core/deftask build
  "Publish library to local repo"
  []
  (comp (bi/pom)
        (bi/jar)
        (bi/install)))

(core/deftask auto-build
  []
  (comp (bi/watch) (bi/pom) (bi/jar) (bi/install)))

(core/deftask publish
  "Publish library and offer command line options (for wercker)"
  [u access-key VALUE str "Access key for rep"
   p secret-key VALUE str "Secret key for repo"
   r repo-uri VALUE str "The repo uri"]
  (bi/push :repo-map {:url        (or repo-uri "s3p://provisdom-artifacts/releases/")
                      :username   (or access-key (System/getenv "AWS_ACCESS_KEY"))
                      :passphrase (or secret-key (System/getenv "AWS_SECRET_KEY"))}))

(core/deftask release
  "Publish library to S3"
  [u access-key VALUE str "Access key for rep"
   p secret-key VALUE str "Secret key for repo"
   r repo-uri VALUE str "The repo uri"]
  (comp
    (bi/pom)
    (bi/jar)
    (bi/push :repo-map {:url        (or repo-uri "s3p://provisdom-artifacts/releases/")
                        :username   (or access-key (System/getenv "AWS_ACCESS_KEY"))
                        :passphrase (or secret-key (System/getenv "AWS_SECRET_KEY"))})))

(core/deftask run-jar
  "execute a jar file"
  [j jarfile PATH str "jar file to run"]
  (let [process (atom nil)]

    (core/cleanup
      (util/info "\n<< stopping %s>>\n" jarfile)
      (.destroy @process))

    (core/with-pre-wrap fileset
                        (util/info "\n<<  starting %s >>\n" jarfile)
                        (reset! process (.exec (Runtime/getRuntime) (str "java -jar " jarfile)))
                        (future (clojure.java.io/copy (.getInputStream @process) System/out))
                        (future (clojure.java.io/copy (.getErrorStream @process) System/err))
                        fileset)))

(core/deftask docker-compose
  "execute docker-compose"
  []
  (let [process (atom nil)]

    (core/cleanup
      (util/info "\n<< stopping docker-compose>>\n")
      (reset! process (.exec (Runtime/getRuntime) "docker-compose stop"))
      (future (clojure.java.io/copy (.getInputStream @process) System/out))
      (future (clojure.java.io/copy (.getErrorStream @process) System/err)))

    (core/with-pre-wrap fileset
                        (util/info "\n<<  starting  >>\n")
                        (reset! process (.exec (Runtime/getRuntime) "docker-compose start"))
                        (future (clojure.java.io/copy (.getInputStream @process) System/out))
                        (future (clojure.java.io/copy (.getErrorStream @process) System/err))
                        fileset)))