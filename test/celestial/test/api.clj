(ns celestial.test.api
  "These scenarios describe how the API works and mainly validates routing"
  (:refer-clojure :exclude [type])
  (:use 
    [clojure.core.strint :only (<<)]
    [celestial.common :only (slurp-edn)]
    ring.mock.request
    expectations.scenarios 
    [celestial.api :only (app)])
  (:require 
    [celestial.jobs :as jobs]
    [celestial.persistency :as p]))

(def host-req 
  (merge {:params (slurp-edn "fixtures/redis-system.edn")}
         (header (request :post "/registry/host") "Content-type" "application/edn")))

(scenario
  (let [{:keys [machine host type]} host-req]
    (expect {:status 200} (in (app (request :post "/registry/host" host-req)))) 
    (expect (interaction (p/register-host host type machine)))))

(scenario
  (expect {:status 200} (in (app (request :get (<< "/registry/host/machine/redis-local"))))) 
  (expect (interaction (p/host "redis-local"))))

(scenario
  (stubbing [p/fuzzy-host {:type "redis"} p/type {:classes {:redis {:append true}}}]
     (expect {:status 200} 
         (in (app (header (request :get (<< "/registry/host/type/redis-local")) "accept"  "application/json")))) 
            ))

(def type-req
  (merge {:params (slurp-edn "fixtures/redis-type.edn")}
         (header (request :post "/registry/type") "Content-type" "application/edn")))

(scenario
  (expect {:status 200} (in (app type-req))) 
  (expect 
    (interaction 
      (p/new-type "redis" (dissoc (slurp-edn "fixtures/redis-type.edn") :type)))))

(scenario 
  (let [machine {:type "redis" :machine {:host "foo"}} type {:classes {:redis {}}}]
    (stubbing [p/host machine  p/type type]
              (expect {:status 200} (in (app (request :post "/provision/redis-local")))) 
              (expect (interaction (jobs/enqueue "provision" (merge machine type)))))))

(scenario 
  (expect {:status 200} (in (app (request :post "/stage/redis-local")))) 
  (expect (interaction (p/host "redis-local")))  
  (expect (interaction (jobs/enqueue "stage" "p/host result"))))

(scenario 
  (expect {:status 200} (in (app (request :post "/machine/redis-local")))) 
  (expect (interaction (p/host "redis-local")))  
  (expect (interaction (jobs/enqueue "machine" "p/host result"))))
