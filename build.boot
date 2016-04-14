(set-env!
  :resource-paths #{"src" "resources"}
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [org.clojure/clojurescript "1.8.40"]
                  [boot/core "2.5.5"]
                  [adzerk/boot-cljs "1.7.228-1"]
                  [cljsjs/boot-cljsjs "0.5.1"]
                  [org.clojure/data.json "0.2.6"]
                  [boot-deps "0.1.6"]])

(def +version+ "0.6.0")

(task-options!
  pom {:project     'provisdom/boot-tasks
       :version     +version+
       :description "Provisdom boot-tasks"
       :url         "https://github.com/Provisdom/boot-tasks"
       :scm         {:url "https://github.com/Provisdom/boot-tasks"}
       :license     {"Provisdom" "(c) 2015 Provisdom Inc."}})

(require '[provisdom.boot-tasks.core :refer :all])