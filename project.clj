(defproject allgress/boot-tasks "0.1.9"
  :description "Allgress boot-tasks."
  :url "https://github.com/allgress/boot-tasks"
  :scm {:url "https://github.com/allgress/boot-tasks"}
  :license {:name "(c) 2015 Allgress Inc."
            :url "http://www.allgress.com"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [boot/core "2.0.0-rc8"]
                 [adzerk/boot-cljs "0.0-2629-8"]
                 [adzerk/boot-reload "0.2.3"]
                 [deraen/boot-cljx "0.2.1"]]
  :plugins [[s3-wagon-private "1.1.2"]]
  :repositories [["s3" {:url           "s3p://aurora-repository/releases/"
                        :username      :env/aws_key
                        :passphrase    :env/aws_secret
                        :sign-releases false}]])
