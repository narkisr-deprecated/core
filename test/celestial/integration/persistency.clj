(ns celestial.integration.persistency
  "Integration test for persistency that use a redis instance"
  (:refer-clojure :exclude [type])
  (:require 
    [celestial.persistency :as p])
  (:use clojure.test
    [celestial.fixtures :only (spec redis-type)]
    [celestial.redis :only (clear-all)]))


(deftest ^:redis sanity
    (clear-all)
    (p/new-type "redis" redis-type) 
    (p/register-host spec) 
    (is (= (p/type-of "redis") redis-type))
    (is (= (p/host "red1") spec))
    )

(deftest ^:redis fuzzy-lookup 
    (clear-all)
    (p/new-type "redis" redis-type) 
    (p/register-host spec) 
    (is (= (p/fuzzy-host "red1") spec)))

