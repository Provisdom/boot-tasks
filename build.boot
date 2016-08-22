(def project 'provisdom/boot-tasks)
(def version "0.6.4")

(set-env!
  :resource-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                  [boot/core "2.6.0" :scope "provided"]])

(require '[provisdom.boot-tasks.core :refer :all])
;;\.cljs\.edn$
(task-options!
  pom {:project     project
       :version     version
       :description "Provisdom boot-tasks"
       :url         "https://github.com/Provisdom/boot-tasks"
       :scm         {:url "https://github.com/Provisdom/boot-tasks"}
       :license     {"Provisdom" "(c) 2015 Provisdom Inc."}})