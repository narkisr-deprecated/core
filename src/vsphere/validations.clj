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

(ns vsphere.validations
  (:use 
    [vsphere.vijava :only (disk-format-types)]
    [clojure.core.strint :only (<<)]
    [bouncer [core :as b] [validators :as v :only (defvalidatorset)]])
  (:require 
    [celestial.validations :as cv]))

(defvalidatorset guest-common
     :password [v/required cv/str?]
     :user [v/required cv/str?]) 

(defvalidatorset machine-entity :os [v/required cv/keyword?])

(defvalidatorset machine-common
    :cpus [v/number v/required]
    :memory [v/number v/required]
    :ip [cv/str?]
    :hostname [v/required cv/str?])

(defvalidatorset machine-provider
     :template [v/required cv/str?])

(def formats (into #{} (keys disk-format-types)))

(defvalidatorset allocation-provider
    :pool [cv/str?]
    :disk-format [v/required (v/member formats :message (<< "disk format must be either ~{formats}"))]
    :datacenter [cv/str? v/required] 
  )

(defvalidatorset vcenter-provider 
   :guest guest-common
   :allocation allocation-provider
   :machine machine-common
   :machine machine-provider  
  )

(defn provider-validation [allocation machine guest]
  (cv/validate!! ::invalid-vm {:allocation allocation :machine machine :guest guest} vcenter-provider))


(defvalidatorset vsphere-entity
    :pool [cv/str?]
    :datacenter [cv/str? v/required] 
    :disk-format [v/required (v/member formats :message (<< "disk format must be either ~{formats}"))]
    :guest guest-common; see http://bit.ly/15G1S46
  )

(defvalidatorset entity-validation
   :machine machine-common 
   :machine machine-entity
  )

(defn validate-entity
 "vcenter based system entity validation for persistence layer" 
  [vcenter]
   (cv/validate!! ::invalid-system vcenter entity-validation)
   (cv/validate!! ::invalid-system (vcenter :vsphere) vsphere-entity)
  )

