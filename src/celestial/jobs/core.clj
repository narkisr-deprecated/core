(comment Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns celestial.jobs.core
  (:refer-clojure :exclude [identity])
  (:use   
    [clojure.core.strint :only (<<)]
    [celestial.common :only (get* minute import-logging)]
    )
  (:require  
    [celestial.jobs.systems :as sj]
    [celestial.jobs.common :refer (job*)]
    [flatland.useful.map :refer (map-vals)]
    [taoensso.carmine :as car]
    [taoensso.carmine.message-queue :as mq]
    [components.core :refer (Lifecyle)] 
    [celestial.redis :refer (create-worker wcar server-conn clear-locks)]
    ))

(import-logging)

(def workers (atom {}))

(defn enqueue 
  "Placing job in redis queue"
  [queue payload] 
  {:pre [(contains? (sj/jobs) (keyword queue))]}
  (trace "submitting" payload "to" queue) 
  (wcar (mq/enqueue queue payload)))

(defn status [queue uuid]
  (wcar (mq/message-status queue uuid)))

(def readable-status
  {:queued :queued :locked :processing :recently-done :done :backoff :backing-off nil :unkown})

(defn- message-desc [type js]
  (mapv 
    (fn [[jid {:keys [identity args tid env] :as message}]] 
      {:type type :status (readable-status (status type jid))
       :env env :id identity :jid jid :tid tid}) (apply hash-map js)))

(defn queue-status 
  "The entire queue message statuses" 
  [job]
  (let [ks [:messages :locks :backoffs]]
    (reduce 
      (fn [r message] (into r (message-desc job message))) []
      (apply merge (vals (select-keys (mq/queue-status (server-conn) job) ks))))))

(defn running-jobs-status
  "Get all jobs status" 
  []
  (reduce (fn [r t] (into r (queue-status (name t)))) [] (keys (sj/jobs))))

(defn by-env 
   "filter jobs status by envs" 
   [envs js]
   (filter (fn [{:keys [env]}] (envs env)) js))

(defn jobs-status [envs]
  (map-vals  
    {:jobs (running-jobs-status)}
    (partial by-env (into #{} envs))))

(defn clear-queues []
  (info "Clearing job queues")
  (apply mq/clear-queues (server-conn) (mapv name (keys (sj/jobs)))))

(defn shutdown-workers []
  (doseq [[k ws] @workers]
    (doseq [w ws]
      (trace "Shutting down" k w) 
      (mq/stop w))))

(defrecord Jobs
  []
  Lifecyle
  (setup [this]) 
  (start [this] 
    (info "Starting job workers")
    (when (= (job* :reset-on) :start)
      (clear-queues)
      (clear-locks))
      (sj/initialize-workers workers))
  (stop [this]
    (info "Stopping job workers")
    (shutdown-workers)
    (when (= (job* :reset-on) :stop)
      (clear-queues)
      (clear-locks))))

(defn instance 
   "Creates a jobs instance" 
   []
  (Jobs.))
