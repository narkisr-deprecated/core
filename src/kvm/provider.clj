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

(ns kvm.provider
  (:require 
    [kvm.validations :refer (provider-validation)]
    [clojure.core.strint :refer (<<)] 
    [kvm.clone :refer (clone-domain)]
    [kvm.common :refer (connect get-domain state)]
    [supernal.sshj :refer (ssh-up?)]
    [celestial.core :refer (Vm)] 
    [taoensso.timbre :as timbre]
    [celestial.persistency.systems :as s]
    [celestial.provider :refer (mappings selections transform os->template wait-for wait-for-ssh)]
    [celestial.model :refer (translate vconstruct hypervisor*)])
  (:import org.libvirt.LibvirtException))

(timbre/refer-timbre)

(defn connection [{:keys [host username port]}] 
  (connect (<< "qemu+ssh://~{username}@~{host}:~{port}/system")))

(defmacro with-connection [& body]
  `(let [~'connection (connection ~'node)] (do ~@body)))

(defrecord Domain [system-id node domain]
  Vm
  (create [this]
    (with-connection 
      (let [image (get-in domain [:image :template]) target (select-keys domain [:name :cpu :ram])]
        (clone-domain connection image target)
         this 
        ))) 

  (delete [this]
    (with-connection 
      (.destroy (get-domain connection (domain :name))))
    )

  (start [this]
    (with-connection 
      (when-not (= (.status this) "running")
        (.create (get-domain connection (domain :name))))))

  (stop [this]
    (with-connection 
      (.shutdown (get-domain connection (domain :name)))))

  (status [this]
    (with-connection 
      (try 
        (state (get-domain connection (domain :name)))
          (catch LibvirtException e (debug (.getMessage e)) false))))

  (ip [this]))

(defn machine-ts 
  "Construcuting machine transformations"
  [{:keys [hostname domain] :as machine}]
   {:name (fn [hostname] (<< "~{hostname}.~{domain}")) 
    :image (fn [os] ((os->template :kvm) os))})

(defmethod translate :kvm [{:keys [machine kvm] :as spec}] 
   (-> machine
     (mappings {:os :image :hostname :name})
     (transform (machine-ts machine))
     (selections [[:name :user :image :cpu :ram]])))

(defmethod vconstruct :kvm [{:keys [kvm machine system-id] :as spec}]
   (let [[domain] (translate spec) {:keys [node]} kvm]
     (provider-validation domain)
     (->Domain system-id (hypervisor* :kvm :nodes node) domain)))

