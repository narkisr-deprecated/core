(comment Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns celestial.jobs.common
  "Common jobs logic"
  (:require 
    [celestial.redis :refer (server-conn)]
    [taoensso.carmine.message-queue :as mq]
    [celestial.model :refer (operations)]
    [taoensso.timbre :refer (refer-timbre)]
    [celestial.common :refer (get*)]
    [es.jobs :as es]))

(refer-timbre)

(defn job* 
  "Get job conf value"
   [& ks]
   (get-in (get* :celestial :job) ks))

(defn save-status
   "marks jobs as succesful" 
   [spec status]
  (let [status-exp (* 1000 60 (or (job* :status-expiry) (* 24 60)))]
    (es/put (merge spec {:status status :end (System/currentTimeMillis)}) status-exp :flush? true) 
    (trace "saved status" (merge spec {:status status :end (System/currentTimeMillis)}))
    {:status status}))

(defn apply-config [js]
  {:post [(= (into #{} (keys %)) operations)]} 
  (reduce (fn [m [k [f c]]] (assoc m k [f (or (job* :workers k) c)])) {} js))


