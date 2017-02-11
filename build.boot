

(set-env!
  :resource-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                  [boot/core "2.6.0" :scope "provided"]
                  [version-clj "0.1.2"]])

(require '[provisdom.boot-tasks.core :as core :refer :all])
;;\.cljs\.edn$
(task-options!
  pom {:project     core/project
       :version     core/version
       :description "Provisdom boot-tasks"
       :url         "https://github.com/Provisdom/boot-tasks"
       :scm         {:url "https://github.com/Provisdom/boot-tasks"}
       :license     {"Provisdom" "(c) 2015 Provisdom Inc."}})