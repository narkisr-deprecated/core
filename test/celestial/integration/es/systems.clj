(ns celestial.integration.es.systems
  "Testing system searching"
  (:require 
    [es.common :as es]
    [es.systems :as sys]
    [clojurewerkz.elastisch.query :as q]
    [celestial.security :refer (set-user)]
    [celestial.fixtures.data :refer (redis-prox-spec redis-ec2-spec)]
    [celestial.fixtures.core :refer (with-conf)]
    [celestial.fixtures.populate :refer (add-users re-initlize)])
  (:use midje.sweet))

(defn add-systems 
   "adds a list of systems into ES" 
   []
  (es/initialize)
  (sys/put "1" (assoc redis-prox-spec :owner "admin"))        
  (sys/put "2" (assoc-in redis-ec2-spec [:machine :hostname] "foo-1" ))        
  (sys/put "3" (assoc redis-ec2-spec :env :prod-1))        
  (sys/put "4" redis-prox-spec :flush? true)
  )

(defn total [res] (get-in res [:hits :total]))

(with-conf
  (against-background [(before :facts (do (re-initlize true) (add-systems) (add-users))) ]
   (fact "basic system put and get" :integration :elasticsearch
     (get-in (sys/get "1") [:source :env]) => ":dev")
   
   (fact "term searching" :integration :elasticsearch
    (total (sys/query {:bool {:must {:term {:machine.cpus 4 }}}})) => 2)

   (fact "pagination" :integration :elasticsearch
     (let [query {:bool {:must {:term {"machine.cpus" "4" }}}}
          {:keys [hits]} (sys/query query :size 2 :from 1)]
        (-> hits :hits count) => 1))

   (fact "aws" :integration :elasticsearch
     (let [query {:wildcard {:aws.endpoint "*"}}
           {:keys [hits]} (sys/query query :size 2 :from 0)]
        (-> hits :hits count) => 2))

   (fact "find proxmox systems for su user" :integration :elasticsearch
       (total (sys/systems-for "admin" {:bool {:must {:term {:machine.cpus "4" }}}} 0 5)) => 2)

   (fact "find by hostname" :integration :elasticsearch
      (total (sys/systems-for "admin" {:bool {:must {:term {:machine.hostname "red1" }}}} 0 5)) => 2
      (total (sys/systems-for "admin" {:bool {:must [{:term {:machine.hostname "foo-1" }}]}} 0 5)) => 1

      (total (sys/systems-for "admin" {:bool {:must {:wildcard {:machine.hostname "r*" }}}} 0 5)) => 2
         )

   (fact "find proxmox systems for non su user" :integration :elasticsearch
      (total (sys/systems-for "ronen" {:bool {:must {:term {:machine.cpus "4" }}}} 0 5)) => 1)

   (fact "find ec2 systems for su user" :integration :elasticsearch
      (total (sys/systems-for "admin" {:bool {:must {:wildcard {:aws.endpoint "*"}}}} 0 5)) => 1)

   (fact "find dev systems" :integration :elasticsearch
      (total (sys/systems-for "admin" 
         {:bool {:should [{:term {:env "dev"}}]}} 0 5)) => 3)))

