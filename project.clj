(defproject allgress/boot-tasks "0.1.17"
  :description "Allgress boot-tasks."
  :url "https://github.com/allgress/boot-tasks"
  :scm {:url "https://github.com/allgress/boot-tasks"}
  :license {:name "(c) 2015 Allgress Inc."
            :url "http://www.allgress.com"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [org.clojure/clojurescript "0.0-2814"]
                 [leiningen-core "2.5.1"]
                 [boot/core "2.0.0-rc13"]
                 [adzerk/boot-cljs "0.0-2814-3"]
                 [adzerk/boot-reload "0.2.6"]
                 [deraen/boot-cljx "0.2.2"]
                 [cljsjs/boot-cljsjs "0.4.6"]
                 [org.clojure/data.json "0.2.6"]]
  :plugins [[s3-wagon-private "1.1.2"]]
  :repositories [["releases" {:url "http://archiva:8080/repository/internal"
                              :username :env/archiva_username
                              :password :env/archiva_password}]])
