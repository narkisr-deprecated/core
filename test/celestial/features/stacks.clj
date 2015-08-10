(ns celestial.features.stacks
  "Stack feature"
  (:require 
    [celestial.workflows.stacks :as wf]
    [flatland.useful.map :refer  (dissoc-in*)]
    [celestial.persistency.stacks :as s]
    [celestial.fixtures.data :refer (simple-stack)]
    [celestial.fixtures.core :refer (with-conf)]
    [celestial.fixtures.core :refer (with-m?)]
    [celestial.fixtures.populate :refer (re-initlize)])
  (:use midje.sweet)
  (:import clojure.lang.ExceptionInfo)
 )

(with-conf
  (with-state-changes [(before :facts (re-initlize))]
    (fact "stack persistency" :integration :stacks :redis :persistency
      (s/add-stack simple-stack) => 1
      (s/update-stack "1" (assoc-in simple-stack [:shared :env] :qa)) 
      (get-in (s/get-stack 1) [:shared :env])  => :qa
      (s/delete-stack "1") => 1)
    
    (fact "stack staging" :integration :stacks :openstack
       (s/add-stack simple-stack) => 1
       (wf/create simple-stack) => nil)))

(fact "stack validations" :validations :stacks
   (s/validate-stack (assoc-in simple-stack [:defaults :dev :machine] [])) => 
      (throws ExceptionInfo (with-m? '{:defaults ({:dev {:machine "must be a map"}})}))
   (s/validate-stack (assoc-in simple-stack [:systems 2] {})) => 
      (throws ExceptionInfo 
        (with-m? '{:systems ({2 {:count "must be present", :template "must be present"}})})))
