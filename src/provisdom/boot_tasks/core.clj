(ns provisdom.boot-tasks.core
  {:boot/export-tasks true}
  (:require
    [boot.util :as util]
    [boot.core :as core]
    [boot.task.built-in :as bi]
    [clojure.edn :as edn]
    [clojure.java.io :as io]))

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

(core/deftask update-file
  "Updates files whose path matches `match-files` with the value returned from `expr`.
  `expr` is a function that takes the parsed content of the file and returns the new
  content of the file. Optionally, you can specify `parse-fn`, a function that takes the
  `slurp`ed content of the file and returns the parsed version. By default `parse-fn`
  is set to `clojure.edn/read-string`."
  [m match-files MATCH #{regex} "The set of regexes that the paths must match"
   e expr VAL code "The function called to transform the content of the matched files"
   p parse-fn VAL code "The function called to parse the files matched."]
  (when-not (and match-files expr)
    (throw (Exception. "need match-files and expr to update files.")))
  (core/with-pre-wrap
    fileset
    (let [parse-fn (or parse-fn edn/read-string)
          matching-tmp-files (->> fileset core/input-files (core/by-re match-files))
          update-content (comp expr parse-fn slurp core/tmp-file)]
      (core/commit!
        (reduce (fn [fileset tmp-file]
                  (let [new-dir (core/tmp-dir!)]
                    (-> new-dir
                        (str "/" (:path tmp-file))
                        io/file
                        (spit (update-content tmp-file)))
                    (core/add-resource fileset new-dir))) fileset matching-tmp-files)))))

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