(set-env! :dependencies '[[leiningen-core "2.5.0"]])
(use 'leiningen.core.project)

(eval (read-string (slurp "project.clj")))

(set-env!
  :src-paths #{"src"}
  :rsc-paths #{"resources"}
  :wagons '[[s3-wagon-private "1.1.2"]]
  :repositories [["clojars" "http://clojars.org/repo/"]
                 ["maven-central" "http://repo1.maven.org/maven2/"]
                 ["s3" {:url "s3p://aurora-repository/releases/" :username (System/getenv "AWS_KEY") :passphrase (System/getenv "AWS_SECRET")}]]
  :dependencies (:dependencies project))

(require '[tailrecursion.boot-useful :refer :all])
(def +version+ (:version project))
(useful! +version+)

(task-options!
  pom [:project (symbol (str (:group project) "/" (:name project)))
       :version +version+
       :description "Allgress boot-tasks."
       :url "https://github.com/allgress/boot-tasks"
       :scm {:url "https://github.com/allgress/boot-tasks"}
       :license {:name "Eclipse Public License"
                 :url  "http://www.eclipse.org/legal/epl-v10.html"}]
  push [:repo "s3"])

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