(ns allgress.boot-tasks
  {:boot/export-tasks true}
  (:require
    [clojure.set :as set]
    [boot.pod :as pod]
    [boot.util :as util]
    [boot.core :refer :all]
    [boot.task.built-in :refer :all]
    [clojure.java.io :as io]
    [boot.task-helpers :as helpers]
    [boot.tmpregistry :refer [add-sync!]]
    [adzerk.boot-cljs :refer [cljs]]
    [adzerk.boot-reload :refer [reload]]))

(defn default-task-options!
  [project]
  (task-options!
    cljs {:unified-mode  true
          :source-map    true
          :optimizations :none}
    pom {:project     (symbol (str (:group project) "/" (:name project)))
         :version     (:version project)
         :description (:description project)
         :url         (:url project)
         :scm         (:scm project)
         :license     (:license project)}
    push {:repo "s3"}
    reload {:on-jsload 'allgress.web-components.core/on-jsload}))

(deftask sync-target
         "Sync directory contents with a copy in target."
         [d dir PATH str "Path to directory to sync"]
         (let [tmp (temp-dir!)]
           (with-pre-wrap fileset
                          (boot.file/sync :time (io/file dir) tmp)
                          (-> fileset
                              (add-resource tmp)
                              (commit!)))))

(deftask cljs-testable
         "compile cljs including tests"
         []
         (cljs :output-to "testable.js" :optimizations :whitespace))

(deftask cljs-test
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

(deftask build
         "Publish released library to s3 and local repo"
         []
         (comp (pom)
               (jar)
               (install)))

(deftask release
         "Publish released library to s3 and local repo"
         []
         (comp (build)
               (push)))