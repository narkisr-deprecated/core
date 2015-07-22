(comment Celestial, Copyright 2012 Ronen Narkis, narkisr.com
  Licensed under the Apache License,
  Version 2.0  (the "License") you may not use this file except in compliance with the License.
  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.)

(ns celestial.jobs.stacks
  "Stack jobs"
  (:require 
    [taoensso.timbre :refer (refer-timbre)]
    [taoensso.carmine.locks :refer (with-lock)]
    [gelfino.timbre :refer (set-tid)]
    [celestial.redis :refer (create-worker server-conn)]
    [celestial.model :refer (set-env)]
    [celestial.security :refer (set-user)]
    [celestial.workflows.stacks :as wf]  
    [flatland.useful.map :refer (map-vals)]
    [celestial.persistency.stacks :as s]
    [celestial.common :refer (minute)]
    [celestial.jobs.common :refer (save-status apply-config job*)])
 )

(refer-timbre)

(defn jobs []
  {:reload [wf/reload 2] :destroy [wf/destroy 2] :provision [wf/provision 2]
   :stage [wf/stage 2] :run-action [wf/run-action 2] :create [wf/create 2]
   :start [wf/start 2] :stop [wf/stop 2] :clear [wf/clear 1] :clone [wf/clone 1]})
