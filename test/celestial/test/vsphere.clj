(ns celestial.test.vsphere
  (:require vsphere.provider)
  (:use 
    midje.sweet
    [clojure.core.strint :only (<<)]
    [celestial.config :only (config)]
    [celestial.model :only (vconstruct)]
    [celestial.fixtures :only (redis-vsphere-spec with-conf)]))

(with-conf
  (fact "basic construction"
    (let [vm (vconstruct redis-vsphere-spec)]
      (:allocation vm ) => {:datacenter "playground" :hostname "red1"}
      (:machine vm ) => {:cpus 1 :disk 10 :memory 512 :template "ubuntu-13.04_puppet-3.1"})))