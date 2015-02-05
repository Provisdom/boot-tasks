(set-env! :dependencies '[[leiningen-core "2.5.1"]])
(use 'leiningen.core.project)

(eval (read-string (slurp "project.clj")))

(set-env!
  :source-paths #{"src"}
  :resource-paths #{"src" "resources"}
  :wagons '[[s3-wagon-private "1.1.2"]]
  :repositories [["clojars" "http://clojars.org/repo/"]
                 ["maven-central" "http://repo1.maven.org/maven2/"]
                 ["s3" {:url "s3p://aurora-repository/releases/" :username (System/getenv "AWS_KEY") :passphrase (System/getenv "AWS_SECRET")}]]
  :dependencies (vec (:dependencies project)))

(def +version+ (:version project))

(task-options!
  pom {:project (symbol (str (:group project) "/" (:name project)))
       :version +version+
       :description "Allgress boot-tasks."
       :url "https://github.com/allgress/boot-tasks"
       :scm {:url "https://github.com/allgress/boot-tasks"}
       :license {"Allgress" "(c) 2015 Allgress Inc."}}
  push {:repo "s3"})

(deftask build
         "Publish released library to s3 and local repo"
         []
         (set-env! :resource-paths #(conj % "src"))
         (comp (pom)
               (jar)
               (install)))

(deftask release
         "Publish released library to s3 and local repo"
         []
         (comp (build)
               (push)))
