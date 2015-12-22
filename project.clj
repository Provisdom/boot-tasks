(defproject provisdom/boot-tasks "0.3.0"
  :description "Provisdom boot-tasks."
  :url "https://github.com/Provisdom/boot-tasks"
  :scm {:url "https://github.com/Provisdom/boot-tasks"}
  :license {:name "(c) 2015 Provisdom Inc."
            :url  "http://www.provisdom.com"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-2814"]
                 [leiningen-core "2.5.1"]
                 [boot/core "2.0.0-rc13"]
                 [adzerk/boot-cljs "0.0-2814-4"]
                 [adzerk/boot-reload "0.2.6"]
                 [deraen/boot-cljx "0.2.2"]
                 [cljsjs/boot-cljsjs "0.4.7"]
                 [org.clojure/data.json "0.2.6"]]
  :plugins [[s3-wagon-private "1.1.2"]])
