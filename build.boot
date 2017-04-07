(def project 'provisdom/boot-tasks)
(def version "1.2")

(set-env!
  :resource-paths #{"src"}
  :dependencies '[[org.slf4j/slf4j-nop "1.7.13" :scope "test"]
                  [org.clojure/clojure "1.8.0" :scope "provided"]
                  [boot/core "2.6.0" :scope "provided"]
                  [adzerk/bootlaces "0.1.13"]
                  [version-clj "0.1.2"]])

(require
  '[provisdom.boot-tasks.core :as core :refer :all]
  '[adzerk.bootlaces :refer [build-jar push-release]])
;;\.cljs\.edn$
(task-options!
  pom {:project     project
       :version     version
       :description "Provisdom boot-tasks"
       :url         "https://github.com/Provisdom/boot-tasks"
       :scm         {:url "https://github.com/Provisdom/boot-tasks"}
       :license     {"Provisdom" "(c) 2015 Provisdom Inc."}})