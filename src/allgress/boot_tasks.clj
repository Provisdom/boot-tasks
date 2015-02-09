(ns allgress.boot-tasks
  #_{:boot/export-tasks true}
  (:require
    [clojure.set :as set]
    [boot.pod :as pod]
    [boot.util :as util]
    [boot.core :refer :all]
    [boot.task.built-in :refer :all]
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [cljs.source-map :as src-map]
    [boot.task-helpers :as helpers]
    [boot.tmpregistry :refer [add-sync!]]
    [adzerk.boot-cljs :refer [cljs]]
    [adzerk.boot-reload :refer [reload]]
    [deraen.boot-cljx :refer [cljx]]))

(defn- read-project
  []
  (set-env! :dependencies (conj (get-env :dependencies) '[leiningen-core "2.5.0" :scope "test"]))
  (use 'leiningen.core.project)

  (let [p (read-string (slurp "project.clj"))]
    (into {:project-name (nth p 1)
           :version      (nth p 2)}
          (map vec (partition 2 (drop 3 p))))))

(defn pom-options!
  []
  (let [project (read-project)]
    (task-options!
      pom {:project     (:project-name project)
           :version     (:version project)
           :description (:description project)
           :url         (:url project)
           :scm         (:scm project)
           :license {(:name (:license project)) (:url (:license project))}})))

(defn default-task-options!
  []
  (pom-options!)
  (task-options!
    cljs {:unified-mode  true
          :source-map    true
          :optimizations :none}
    push {:repo "s3"}
    reload {:on-jsload 'allgress.web-components.core/on-jsload}
    watch {:debounce 50}))

(defn set-project-deps!
  []
  (let [project (read-project)]
    (set-env! :dependencies (into (get-env :dependencies) (vec (:dependencies project))))))

#_(deftask cljs-testable
         "compile cljs including tests"
         []
         (cljs :output-to "testable.js" :optimizations :whitespace))

#_(deftask cljs-test
         "run cljs tests"
         []
         (with-pre-wrap fileset
                        (let [testable (first (by-name ["testable.js"] (output-files fileset)))
                              runner (io/resource "runner.js")
                              runner-path (str (get-env :tgt-path) "/runner.js")]
                          (spit runner-path (slurp runner))
                          (when testable
                            (util/dosh "phantomjs" runner-path (.getPath testable))))
                        fileset))

(deftask asset-paths
         "Set :asset-paths"
         [a asset-paths PATHS #{str} ":asset-paths"]
         (set-env! :asset-paths asset-paths)
         (with-pre-wrap fileset
                        fileset))

(deftask build
         "Publish library to local repo"
         []
         (comp (cljx)
               (pom)
               (jar)
               (install)))

(deftask release
         "Publish released library to s3 and local repo"
         []
         (comp (build)
               (push)))

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

(deftask build-uberjar
         "Builds an uberjar of this project that can be run with java -jar."
         []
         (let [project (read-project)
               project-name (:project-name project)
               core (symbol (str (namespace project-name) "." (name project-name) ".core"))]
           (comp
             (cljx)
             (pom)
             (aot :namespace #{core})
             (uber)
             (jar :main core))))

#_(deftask cljs-map
         "Builds source-mapping utilities and data based on *.js.map files"
         []
         (let [tmp (temp-dir!)]
           (fn middleware [next-handler]
             (fn handler [fileset]
               (empty-dir! tmp)
               (let [in-files (output-files fileset)
                     map-files (by-ext [".js.map"] in-files)]
                 (doseq [in map-files]
                   (let [in-file (tmpfile in)
                         in-path (tmppath in)
                         out-path (str in-path ".bork")
                         out-file (io/file tmp out-path)
                         source-map (src-map/decode (json/read-str (slurp in-file) :key-fn keyword))]
                     (doto out-file
                       io/make-parents
                       (spit (json/write-str source-map)))))
                 (-> fileset
                     (add-resource tmp)
                     commit!
                     next-handler))))))