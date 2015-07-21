(comment Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns celestial.jobs.systems
  "System jobs"
  (:require 
    [taoensso.timbre :refer (refer-timbre)]
    [taoensso.carmine.locks :refer (with-lock)]
    [gelfino.timbre :refer (set-tid)]
    [celestial.redis :refer (create-worker server-conn)]
    [celestial.model :refer (set-env)]
    [celestial.security :refer (set-user)]
    [celestial.workflows :as wf]  
    [flatland.useful.map :refer (map-vals)]
    [celestial.persistency.systems :as s]
    [celestial.common :refer (minute)]
    [celestial.jobs.common :refer (save-status apply-config job*)]))

(refer-timbre)

(def defaults {:wait-time 5 :expiry 30})

(defn job-exec [f {:keys [message attempt]}]
  "Executes a job function tries to lock identity first (if used)"
  (let [{:keys [identity args tid env user] :as spec} message]
    (set-user user
      (set-env env
        (set-tid tid 
           (let [{:keys [wait-time expiry]} (map-vals (or (job* :lock) defaults) #(* minute %))
                 hostname (when identity (get-in (s/get-system identity) [:machine :hostname]))
                 spec' (merge spec (meta f) {:start (System/currentTimeMillis) :hostname hostname})]
            (try 
              (if identity
                (do (with-lock (server-conn) identity expiry wait-time (apply f args))
                    (save-status spec' :success)) 
                (do (apply f args) 
                    (save-status spec' :success))) 
              (catch Throwable e 
                (error e) 
                (save-status spec' :error)
                ))))))))

(defn create-wks [queue f total]
  "create a count of workers for queue"
  (mapv 
     (fn [v] (create-worker (name queue) (partial job-exec (with-meta f {:queue queue})))) (range total)))

(defn jobs []
  {:reload [wf/reload 2] :destroy [wf/destroy 2] :provision [wf/provision 2]
   :stage [wf/stage 2] :run-action [wf/run-action 2] :create [wf/create 2]
   :start [wf/start 2] :stop [wf/stop 2] :clear [wf/clear 1] :clone [wf/clone 1]})

(defn initialize-workers [workers]
  (doseq [[q [f c]] (apply-config (jobs))]
    (swap! workers assoc q (create-wks q f c))))
