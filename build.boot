(set-env! :dependencies '[[leiningen-core "2.5.3" :scope "test"]
                          [boot-deps "0.1.6"]])
(use 'leiningen.core.project)

(eval (read-string (slurp "project.clj")))

(set-env!
  :source-paths #{"src"}
  :resource-paths #{"src" "resources"}
  :repositories [["clojars" "http://clojars.org/repo/"]
                 ["maven-central" "http://repo1.maven.org/maven2/"]]
  :dependencies (vec (:dependencies project))
  :wagons '[[s3-wagon-private "1.2.0"]])

(def +version+ (:version project))

(task-options!
  pom {:project     (symbol (str (:group project) "/" (:name project)))
       :version     +version+
       :description "Provisdom boot-tasks."
       :url         "https://github.com/Provisdom/boot-tasks"
       :scm         {:url "https://github.com/Provisdom/boot-tasks"}
       :license     {"Provisdom" "(c) 2015 Provisdom Inc."}}
  push {:repo "releases"})

(deftask build
         "Publish released library to s3 and local repo"
         []
         (set-env! :resource-paths #(conj % "src"))
         (comp (pom)
               (jar)
               (install)))

(deftask release
         "Publish library to S3"
         [u access-key VALUE str "Access key for rep"
          p secret-key VALUE str "Secret key for repo"
          r repo-uri VALUE str "The repo uri"]
         (comp
           (pom)
           (jar)
           (push :repo-map {:url        (or repo-uri "s3p://provisdom-artifacts/releases/")
                            :username   access-key
                            :passphrase secret-key})))
