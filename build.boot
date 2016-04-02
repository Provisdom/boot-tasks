(set-env!
  :resource-paths #{"src" "resources"}
  :repositories [["clojars" "http://clojars.org/repo/"]
                 ["maven-central" "http://repo1.maven.org/maven2/"]
                 ["provisdom" {:url        "s3p://provisdom-artifacts/releases/"
                               :username   (System/getenv "AWS_ACCESS_KEY")
                               :passphrase (System/getenv "AWS_SECRET_KEY")}]]
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [org.clojure/clojurescript "1.7.228"]
                  [leiningen-core "2.5.3"]
                  [boot/core "2.5.5"]
                  [adzerk/boot-cljs "1.7.228-1"]
                  [adzerk/boot-reload "0.4.4"]
                  [deraen/boot-cljx "0.3.0"]
                  [cljsjs/boot-cljsjs "0.5.1"]
                  [org.clojure/data.json "0.2.6"]
                  [leiningen-core "2.5.3" :scope "test"]
                  [boot-deps "0.1.6"]]
  :wagons '[[s3-wagon-private "1.2.0"]])

(def +version+ "0.5.0")

(task-options!
  pom {:project     'provisdom/boot-tasks
       :version     +version+
       :description "Provisdom boot-tasks"
       :url         "https://github.com/Provisdom/boot-tasks"
       :scm         {:url "https://github.com/Provisdom/boot-tasks"}
       :license     {"Provisdom" "(c) 2015 Provisdom Inc."}})

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

(deftask testing123
         ""
         []
         (println "FOO" (System/getenv "FOO"))
         (println "BAR" (System/getenv "BAR"))
         (with-pre-wrap fileset
                        fileset))
