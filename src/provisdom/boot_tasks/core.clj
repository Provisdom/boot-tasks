(ns provisdom.boot-tasks.core
  {:boot/export-tasks true}
  (:require
    [boot.util :as util]
    [boot.core :as core]
    [boot.pod :as pod]
    [boot.task.built-in :as built-in]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [version-clj.core :as version]))

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
  (str/ends-with? (str/lower-case version) "snapshot"))

(defn version->map
  "Parses string `s` into a version map"
  [s]
  (let [[version-nums qualifiers] (version/version->seq s)
        [major minor patch] version-nums
        [prerelease build] qualifiers]
    {:major       major
     :minor       minor
     :patch       patch
     :pre-release (if (= prerelease "snapshot") "SNAPSHOT" prerelease) ; need to do this b/c the version parsing auto lowercases everything
     :build       build}))

(defn map->version
  [m]
  (let [{:keys [major minor patch pre-release build]} m]
    (str major
         "."
         minor
         (when (and minor patch) ".")
         patch
         (when pre-release "-")
         pre-release
         build)))

(defn version->version-range
  [version]
  (let [version-map (version->map version)
        major-minor (fn [vmap] (str (:major vmap) "." (:minor vmap)))]
    (str "[" (major-minor version-map) "," (major-minor (update version-map :minor inc)) ")")))

(defn dep-version->version-range
  [dep-vec matching-groups]
  (let [[artifact-name _ & opts] dep-vec
        opts-map (into {} (map vec (partition 2 opts)))]
    (if (or (contains? matching-groups (namespace artifact-name))
            (:timestamp? opts-map))
      (update dep-vec 1 version->version-range)
      dep-vec)))

(def ^:dynamic *snapshot-replace-group-ids* #{"provisdom"})
(def ^:private boot-set-env! core/set-env!)

(defn set-env2!
  "Same as Boot's `set-env!` except this will replace SNAPSHOT versions that have a group
  id in [[*snapshot-replace-group-ids*]] or have `:timestamp? true` in the dependency vec
  with a version range."
  [& kvs]
  (let [kvs (reduce (fn [acc [k v]]
                      (conj acc
                            [k (if (= k :dependencies)
                                 (fn [deps]
                                   (let [new-deps (if (fn? v)
                                                    (v deps)
                                                    deps)]
                                     (map #(dep-version->version-range % *snapshot-replace-group-ids*) new-deps)))
                                 v)])) [] (partition 2 kvs))]
    (do (apply boot-set-env! (flatten kvs))
        (core/get-env))))

(def boot-home
  (or (io/file (System/getenv "BOOT_HOME"))
      (io/file (System/getProperty "user.home") ".boot")))

(defn provisdom-init
  "Initializes `set-env!` wrapper."
  []
  (alter-var-root #'boot.core/set-env! (constantly set-env2!))
  (core/set-env! :dependencies identity)
  nil)

(core/deftask build
  "Installs a jar to your local Maven repo."
  []
  (comp (built-in/pom) (built-in/jar) (built-in/install)))

(core/deftask inst
  "Installs a jar to your local Maven repo. If the jar is a SNAPSHOT version then the SNAPSHOT suffix
  will be replaced with the current time in milliseconds."
  [n no-replace? bool "True if SNAPSHOT versions should NOT be replaced with a timestamp."
   i skip-install? bool "True if the local install should be skipped."]
  (let [pom-task-opts (pom-opts)]
    (comp (apply built-in/pom
                 (if (and (not no-replace?) (snapshot? (:version pom-task-opts)))
                   (-> (update pom-task-opts :version (fn [v]
                                                        (map->version
                                                          (assoc (version->map v)
                                                            :patch (str (System/currentTimeMillis))
                                                            :pre-release nil
                                                            :build nil))))
                       vec flatten)
                   []))
          (built-in/jar)
          (if skip-install? identity (built-in/install)))))

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
    (inst :skip-install? true)
    (built-in/push :repo-map {:url        (or repo-uri (System/getenv "MAVEN_URI") "s3p://provisdom-artifacts/releases/")
                              :username   (or access-key (System/getenv "AWS_ACCESS_KEY_ID"))
                              :passphrase (or secret-key (System/getenv "AWS_SECRET_ACCESS_KEY"))})))

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