(def project 'provisdom/boot-tasks)
(def version "0.6.1")

(set-env!
  :resource-paths #{"src" "resources"}
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [org.clojure/clojurescript "1.8.51"]
                  [boot/core "2.5.5"]
                  [adzerk/boot-cljs "1.7.228-1"]
                  [cljsjs/boot-cljsjs "0.5.1"]])

(require '[provisdom.boot-tasks.core :refer :all])

(task-options!
  pom {:project     project
       :version     version
       :description "Provisdom boot-tasks"
       :url         "https://github.com/Provisdom/boot-tasks"
       :scm         {:url "https://github.com/Provisdom/boot-tasks"}
       :license     {"Provisdom" "(c) 2015 Provisdom Inc."}})