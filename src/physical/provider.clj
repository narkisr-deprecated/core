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

(ns physical.provider
  "Physical machine management, 
   * creation is not supported maybe pxe boot in future?
   * deletion is not supported. 
   * start will use wake on lan)
   * stop will run remote stop command via ssh
   * status will use ssh to try and see if the machine is running 
    "
  (:require 
    [physical.validations :refer (validate-provider)]
    [celestial.provider :refer (wait-for-ssh mappings)]
    [celestial.common :refer (import-logging bash-)] 
    [clojure.core.strint :refer (<<)] 
    [supernal.sshj :refer (ssh-up? execute)] 
    [trammel.core :refer (defconstrainedrecord)] 
    [celestial.core :refer (Vm)] 
    [slingshot.slingshot :refer  [throw+ try+]] 
    [physical.wol :refer (wol)] 
    [celestial.model :refer (translate vconstruct)] 
    ))

(import-logging)

(defconstrainedrecord Machine [remote interface]
  " "
  [(validate-provider remote interface)]
  Vm
  (create [this] 
     (throw+ {:type ::not-supported :msg "cannot create a phaysical machine"}))

  (delete [this]
     (throw+ {:type ::not-supported :msg "cannot delete a phaysical machine"}))

  (start [this]
     (wol interface)
     (wait-for-ssh (remote :host) (remote :user) [10 :minute]))

  (stop [this]
     (execute (bash- ("sudo" "shutdown" "0" "-P")) remote))

  (status [this] 
     (if (ssh-up? remote) "running" "NaN")))

(defmethod translate :physical 
  [{:keys [physical machine]}]
  [(mappings  (select-keys machine [:hostname :ip :user]) {:hostname :host})
   (select-keys physical [:mac :broadcast])])

(defmethod vconstruct :physical [spec]
   (apply ->Machine  (translate spec)))
