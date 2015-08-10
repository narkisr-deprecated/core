(comment 
   Celestial, Copyright 2012 Ronen Narkis, narkisr.com
   Licensed under the Apache License,
   Version 2.0  (the "License") you may not use this file except in compliance with the License.
   You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.)

(ns celestial.workflows.stacks
  "Stack workflows"
  (:require 
    [celestial.workflows.common :refer (deflow run-hooks)] 
    [celestial.workflows.systems :as wfs] 
    [celestial.persistency.systems :as s]
    [taoensso.timbre :refer  (refer-timbre)]
    [clojure.core.strint :refer (<<)]
    [slingshot.slingshot :refer  [throw+ try+]]
    [clojure.core.async :refer (<!! >!! to-chan) :as async]))

(refer-timbre)

(deflow reload 
  "Reload all systems"
  [spec]
  )

(deflow stop
  "Stop stack systems"
  [spec]
  )

(deflow start 
  "Start stack systems"
  [spec]
  )

(deflow create
  "Create stack from templates"
  [spec]
  )

(deflow destroy 
  "Destroy all stack instances"
  [spec]
  )

(deflow clone
  "Clone a stack"
  [spec])

(deflow clear
  "Clear stack"
  [spec]
  )

(deflow provision
  "Run provisioning on stack instances"
  [spec])

(defn template-chan [provided {:keys [count template]}] 
  (to-chan (map #(s/templatize template provided) (range count))))

(deflow stage
  "create and provision"
  [{:keys [systems defaults] :as spec}] 
  (let [provided {} cs (async/merge (map (partial template-chan provided) systems))]
     
    ))

(deflow run-action
  "Runs an action"
  [spec]
  )

