(ns provisdom.boot-tasks.core
  {:boot/export-tasks true}
  (:require
    [boot.util :as util]
    [boot.core :as core]
    [boot.pod :as pod]
    [boot.task.built-in :as built-in]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]))

(core/deftask asset-paths
  "Set :asset-paths"
  [a asset-paths PATHS #{str} ":asset-paths"]
  (core/set-env! :asset-paths asset-paths)
  (core/with-pre-wrap fileset
                      fileset))

(defn pom-opts
  "Returns the `pom` task options."
  []
  (-> #'built-in/pom meta :task-options))

(defn snapshot?
  "Return true if `version` is a SNAPSHOT version"
  [version]
  (str/ends-with? version "SNAPSHOT"))

(def semver-pattern #"^(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?(?:\+([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?$")

(defn- maybe-int
  [x]
  (try (Integer/parseInt x) (catch NumberFormatException _ x)))

(defn version->map
  "Parses string `s` into a version map"
  [s]
  (let [[[_ major minor patch pre-release build]] (re-seq semver-pattern s)]
    {:major       (maybe-int major)
     :minor       (maybe-int minor)
     :patch       (maybe-int patch)
     :pre-release pre-release
     :build       build}))

(defn map->version
  [m]
  )

(defn snapshot->version-range
  [version]
  (-> version
      (str/replace "-SNAPSHOT" "")))

(defn dep-version->version-range
  [dep-vec matching-groups]
  (let [[artifact-name version & opts] dep-vec
        opts-map (into {} (partition 2 opts))]
    #_(if (or (contains? matching-groups (namespace artifact-name))
              (:timestamp? opts-map))
        #_(update dep-vec 1))))

(defn set-env!-wrapper
  [& kvs]
  (let [env (do (apply core/set-env! kvs)
                (core/get-env))]
    (update env :dependencies (fn [deps]
                                ))))

(core/deftask build
  "Installs a jar to your local Maven repo."
  []
  (comp (built-in/pom) (built-in/jar) (built-in/install)))

(core/deftask inst
  "Installs a jar to your local Maven repo. If the jar is a SNAPSHOT version then the SNAPSHOT suffix
  will be replaced with the current time in milliseconds."
  []
  (let [pom-task-opts (pom-opts)]
    (comp (apply built-in/pom
                 (if (snapshot? (:version pom-task-opts))
                   (-> (update pom-task-opts :version str/replace "SNAPSHOT" (str (System/currentTimeMillis)))
                       vec flatten)
                   []))
          (built-in/jar)
          (built-in/install))))

(core/deftask auto-build
  []
  (comp (built-in/watch) (build)))

(core/deftask push-jar
  "Build and publish library to S3. Defaults to Provisdom's S3 maven repo using AWS_ACCESS_KEY_ID
  and AWS_SECRET_KEY for the env vars for `push`."
  [u access-key VALUE str "Access key for rep"
   p secret-key VALUE str "Secret key for repo"
   r repo-uri VALUE str "The repo uri"]
  (comp
    (built-in/pom)
    (built-in/jar)
    (built-in/push :repo-map {:url        (or repo-uri "s3p://provisdom-artifacts/releases/")
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
                    (-> (io/file new-dir (:path tmp-file))
                        (spit (update-content tmp-file)))
                    (core/add-resource fileset new-dir))) fileset matching-tmp-files)))))

;; taken from https://github.com/boot-clj/boot/wiki/Snippets#check-dependency-conflicts
(core/deftask check-conflicts
  "Verify there are no dependency conflicts."
  []
  (core/with-pass-thru fs
                       (require '[boot.pedantic :as pedant])
                       (let [dep-conflicts (resolve 'pedant/dep-conflicts)]
                         (if-let [conflicts (not-empty (dep-conflicts pod/env))]
                           (throw (ex-info (str "Unresolved dependency conflicts. "
                                                "Use :exclusions to resolve them!")
                                           conflicts))
                           (println "\nVerified there are no dependency conflicts.")))))

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