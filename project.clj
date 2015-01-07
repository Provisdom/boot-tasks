(defproject allgress/boot-tasks "0.1.6"
  :description "Allgress boot-tasks."
  :url "https://github.com/allgress/boot-tasks"
  :scm {:url "https://github.com/allgress/boot-tasks"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [boot/core "2.0.0-rc3"]
                 [adzerk/boot-cljs "0.0-2629-2"]
                 [adzerk/boot-reload "0.2.3"]
                 [deraen/boot-cljx "0.2.1"]]
  :plugins [[s3-wagon-private "1.1.2"]]
  :repositories [["s3" {:url           "s3p://aurora-repository/releases/"
                        :username      :env/aws_key
                        :passphrase    :env/aws_secret
                        :sign-releases false}]])
