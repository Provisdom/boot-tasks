(defproject allgress/boot-tasks "0.0.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [boot/core "2.0.0-pre24" :scope "provided"]
                 [tailrecursion/boot-useful "0.1.3" :scope "test"]]
  :plugins [[s3-wagon-private "1.1.2"]]
  :repositories [["s3" {:url           "s3p://aurora-repository/releases/"
                        :username      :env/aws_key
                        :passphrase    :env/aws_secret
                        :sign-releases false}]])
