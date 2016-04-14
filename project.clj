(defproject
  provisdom/boot-tasks
  "0.6.0"
  :plugins
  [[s3-wagon-private "1.2.0"]]
  :dependencies
  [[org.clojure/clojure "1.8.0"]
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
  :repositories
  [["clojars" "http://clojars.org/repo/"]
   ["maven-central" "http://repo1.maven.org/maven2/"]
   ["provisdom"
    {:username :env/aws_access_key,
     :passphrase :env/aws_secret_key,
     :url "s3p://provisdom-artifacts/releases/"}]]
  :source-paths
  ["src" "resources"])