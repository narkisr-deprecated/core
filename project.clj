(defproject celestial "0.0.1"
  :description "A launching pad for virtualized applications"
  :url "https://github.com/celestial-ops/celestial-core"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.1" ]
                 [clj-ssh "0.5.0" ]
                 [clj-config "0.2.0" ]
                 [prismatic/plumbing "0.0.1"]
                 [bouncer "0.2.3-beta1"]
                 [cheshire "5.0.2"]
                 [com.taoensso/timbre "1.5.2"]
                 [com.narkisr/gelfino-client "0.4.0"]
                 [org.clojure/core.incubator "0.1.2"]
                 [slingshot "0.10.3" ]
                 [clj-http "0.6.5"]
                 [swag "0.1.3"]
                 [clj-yaml "0.4.0"]
                 [org.clojure/data.json "0.2.1" ]
                 [com.narkisr/carmine "1.6.0"]
                 [org.clojure/core.memoize "0.5.2" :exclusions [org.clojure/core.cache]]
                 [org.slf4j/slf4j-simple "1.6.4"]; required for codahale metrics
                 [metrics-clojure "0.9.2"]
                 [clj-aws-ec2 "0.2.1" :exclusions  [org.codehaus.jackson/jackson-core-asl]]
                 [narkisr/trammel "0.8.0-freez"]
                 [mississippi "1.0.1"]
                 [org.flatland/useful "0.9.5"]
                 [fogus/minderbinder "0.2.0"]
                 [metrics-clojure-ring "0.9.2"]
                 [ring "1.1.8"]
                 [compojure "1.1.5" :exclusions  [ring/ring-core]]
                 [ring/ring-jetty-adapter "1.1.8"]
                 [org.bouncycastle/bcprov-jdk16 "1.46"]
                 [com.cemerick/friend "0.1.4"]
                 [net.schmizz/sshj "0.8.1"]
                 [org.clojure/tools.macro "0.1.2"]
                 [ring-middleware-format "0.2.4"]]

  :exclusions [org.clojure/clojure]

  :plugins  [[jonase/eastwood "0.0.2"] [lein-pedantic "0.0.5"] [lein-midje "3.0.0"]]

  :injections [(require '[redl core complete])]

  :profiles {:dev {:dependencies [[org.clojure/tools.trace "0.7.5"] [redl "0.1.0" :exclusions  [commons-io]]
                                  [ring-mock "0.1.3"]  [midje "1.5.1" :exclusions [org.clojure/core.unify]]
                                  [junit/junit "4.8.1"] ]}}
                 
  :tar  {:uberjar true}

  :aot [proxmox.provider celestial.core celestial.puppet-standalone celestial.launch]

  :test-selectors {:default #(not-any? % [:proxmox :redis :integration :puppet :ec2]) 
                   :redis :redis
                   :proxmox :proxmox
                   :puppet :puppet
                   :ec2 :ec2
                   :integration :integration
                   :all (constantly true)}

  :repositories  {"sonatype" "http://oss.sonatype.org/content/repositories/releases"}
      
  :resource-paths  ["src/main/resource"]

  :aliases  
  {"reload"  ["run" "-m" "celestial.tasks" "reload" "systems/baseline.edn" "proxmox"]
   "puppetize"  ["run" "-m" "celestial.tasks" "puppetize" "systems/baseline.edn"]}

  :main celestial.launch
  )
