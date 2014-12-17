(ns allgress.boot-tasks
  {:boot/export-tasks true}
  (:require
    [boot.pod :as pod]
    [boot.util :as util]
    [boot.core :refer :all]
    [boot.task.built-in :refer :all]
    [clojure.java.io :as io]
    [boot.task-helpers :as helpers]
    [boot.tmpregistry :refer [add-sync!]]
    [adzerk.boot-cljs :refer [cljs]]))

(defn default-task-options!
  [project]
  (task-options!
    cljs {:unified-mode true
          :source-map true
          :optimizations :none}
    pom {:project (symbol (str (:group project) "/" (:name project)))
         :version (:version project)
         :description (:description project)
         :url (:url project)
         :scm (:scm project)
         :license (:license project)}
    push {:repo "s3"}))

(deftask cljs-testable
         "compile cljs including tests"
         []
         (cljs :output-to "testable.js" :optimizations :whitespace))

(deftask cljs-test
         "run cljs tests"
         []
         (with-pre-wrap fileset
           (let [testable (first (by-name ["testable.js"] (output-files)))
                 runner (io/resource "runner.js")
                 runner-path (str (get-env :tgt-path) "/runner.js")]
             (spit runner-path (slurp runner))
             (when testable
               (util/dosh "phantomjs" runner-path (.getPath testable))))
           fileset))

(deftask build
         "Publish released library to s3 and local repo"
         []
         (set-env! :resource-paths (conj (get-env :resource-paths) "src"))
         (comp (pom)
               (jar)
               (install)))

(deftask release
         "Publish released library to s3 and local repo"
         []
         (comp (build)
               (push)))