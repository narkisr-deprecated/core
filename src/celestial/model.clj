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

(ns celestial.model 
  "Model manipulation ns"
  )

(def hypervizors #{:proxmox :aws :vsphere :vagrant})

(defn figure-virt [spec] (first (filter hypervizors (keys spec))))

(defmulti translate
  "Converts general model to specific virtualization model" 
  (fn [spec] (figure-virt spec)))

(defmulti vconstruct 
  "Creates a Virtualized instance model from input spec" 
  (fn [spec] (figure-virt spec)))

(def provisioners #{:chef :puppet :puppet-std})

(defmulti pconstruct
  "Creates a Provisioner instance model from input spec" 
   (fn [type spec] (first (filter provisioners (keys type)))))

(def remoters #{:supernal :capistrano})

(defn figure-rem [spec] (first (filter remoters (keys spec))))

(defmulti rconstruct
  "Creates a Remoter instance model from input spec"
   (fn [spec] (figure-rem spec)))
