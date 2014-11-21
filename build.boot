(set-env!
  :src-paths    #{"src"}
  :wagons       '[[s3-wagon-private "1.1.2"]]
  :repositories '[["clojars" "http://clojars.org/repo/"]
                  ["maven-central" "http://repo1.maven.org/maven2/"]
                  ["s3" {:url "s3p://aurora-repository/releases/" :username "AKIAI32ZF3YKANV3FQ5A" :passphrase "yDWZJnuyoz/E9/sFP9UtNelot1Zpd7buv/eYvIC9"}]]
  :dependencies '[[org.clojure/clojure "1.7.0-alpha4" :scope "provided"]
                  [boot/core "2.0.0-pre24" :scope "provided"]
                  [tailrecursion/boot-useful "0.1.3" :scope "test"]])

(require '[tailrecursion.boot-useful :refer :all])
(def +version+ "0.0.1-SNAPSHOT")
(useful! +version+)

(task-options!
  pom [:project 'allgress/boot-tasks
       :version +version+
       :description "Allgress boot-tasks."
       :url "https://github.com/allgress/boot-tasks"
       :scm {:url "https://github.com/allgress/boot-tasks"}
       :license {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}])