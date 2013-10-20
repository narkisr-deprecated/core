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

(ns aws.provider
  (:require 
    [aws.common :refer (with-ctx instance-desc creds)]
    [aws.networking :refer (update-ip set-hostname pub-dns)]
    [aws.sdk.ec2 :as ec2]
    [aws.sdk.ebs :as ebs]
    [aws.sdk.eip :as eip]
    [aws.validations :refer (provider-validation)]
    [celestial.persistency :as p]
    [clojure.core.strint :refer (<<)] 
    [supernal.sshj :refer (ssh-up?)] 
    [flatland.useful.utils :refer (defm)] 
    [flatland.useful.map :refer (dissoc-in*)] 
    [slingshot.slingshot :refer  [throw+ try+]] 
    [trammel.core :refer (defconstrainedrecord)] 
    [celestial.provider :refer (wait-for)] 
    [celestial.core :refer (Vm)] 
    [celestial.common :refer (import-logging )] 
    [celestial.persistency.systems :as s]
    [celestial.model :refer (translate vconstruct hypervisor)]))

(import-logging)

(defn wait-for-status [instance req-stat timeout]
  "Waiting for ec2 machine status timeout is in mili"
  (wait-for {:timeout timeout} #(= req-stat (.status instance))
    {:type ::aws:status-failed :message "Timed out on waiting for status" :status req-stat :timeout timeout}))

(defn image-desc [endpoint ami & ks]
  (-> (ec2/describe-images (assoc (creds) :endpoint endpoint) (ec2/image-id-filter ami))
      first (apply ks)))



(defn wait-for-attach [endpoint instance-id timeout]
  (wait-for {:timeout timeout} 
            #(= "attached" (instance-desc endpoint instance-id :block-device-mappings 0 :ebs :status)) 
            {:type ::aws:ebs-attach-failed :message "Failed to wait for ebs root device attach"}))

(defn wait-for-ssh [endpoint instance-id user timeout]
    (wait-for {:timeout timeout}
              #(ssh-up? {:host (pub-dns endpoint instance-id) :port 22 :user user})
              {:type ::aws:ssh-failed :message "Timed out while waiting for ssh" :timeout timeout}))

(defn instance-id*
  "grabbing instance id of spec"
   [spec]
  (get-in (s/get-system (spec :system-id)) [:aws :instance-id]))

(defmacro with-instance-id [& body]
 `(if-let [~'instance-id (instance-id* ~'spec)]
    (do ~@body) 
    (throw+ {:type ::aws:missing-id :message "Instance id not found"}))) 

(defn image-id [machine]
  (hypervisor :aws :ostemplates (machine :os) :ami))

(defn handle-volumes 
   "attached and waits for ebs volumes" 
   [{:keys [aws machine] :as spec} endpoint instance-id]
  (when (= (image-desc endpoint (image-id machine) :root-device-type) "ebs")
    (wait-for-attach endpoint instance-id [10 :minute]))
  (let [zone (instance-desc endpoint instance-id :placement :availability-zone)]
    (doseq [{:keys [device size]} (aws :volumes)]
      (let [{:keys [volumeId]} (with-ctx ebs/create-volume size zone)]
        (wait-for {:timeout [10 :minute]} #(= "available" (with-ctx ebs/state volumeId))
           {:type ::aws:ebs-volume-availability :message "Failed to wait for ebs volume to become available"})
        (with-ctx ebs/attach-volume volumeId instance-id device)
        (wait-for {:timeout [10 :minute]} #(= "attached" (with-ctx ebs/attachment-status volumeId))
           {:type ::aws:ebs-volume-attach-failed :message "Failed to wait for ebs volume device attach"})))))

(defn delete-volumes 
   "Clear instance volumes" 
   [endpoint instance-id]
   (doseq [{:keys [ebs]} (-> (instance-desc endpoint instance-id) :block-device-mappings rest)]
     (trace "deleting volume" ebs)
     (with-ctx ebs/detach-volume (ebs :volume-id))
     (wait-for {:timeout [10 :minute]} #(= "available" (with-ctx ebs/state  (ebs :volume-id)))
        {:type ::aws:ebs-volume-availability :message "Failed to wait for ebs volume to become available"})
     (with-ctx ebs/delete-volume (ebs :volume-id))))

(defn create-instance 
   "creates instance from aws" 
   [{:keys [aws machine] :as spec} endpoint]
   {:pre [(clojure.set/subset? (into #{} (keys aws))
       #{:volumes :min-count :max-count :instance-type :key-name})]}
   (let [inst (merge (dissoc aws :volumes) {:image-id (image-id machine)})]
     (-> (with-ctx ec2/run-instances inst) :instances first :id)))

(defconstrainedrecord Instance [endpoint spec user]
  "An Ec2 instance"
  [(provider-validation spec) (-> endpoint nil? not)]
  Vm
  (create [this] 
    (let [instance-id (create-instance spec endpoint)]
       (s/partial-system (spec :system-id) {:aws {:instance-id instance-id}})
       (debug "created" instance-id)
       (handle-volumes spec endpoint instance-id)    
       (when-let [ip (get-in spec [:machine :ip])] 
         (debug (<<  "Associating existing ip ~{ip} to instance-id"))
          (with-ctx eip/assoc-pub-ip instance-id ip))
       (update-ip spec endpoint instance-id)
       (wait-for-ssh endpoint instance-id user [5 :minute])
       (set-hostname spec endpoint instance-id user)
        this))

  (start [this]
    (with-instance-id
      (debug "starting" instance-id)
      (with-ctx ec2/start-instances instance-id) 
      (wait-for-status this "running" [5 :minute]) 
      (when-let [ip (get-in spec [:machine :ip])] 
        (debug (<<  "Associating existing ip ~{ip} to instance-id"))
        (with-ctx eip/assoc-pub-ip instance-id ip))
      (update-ip spec endpoint instance-id)
      (wait-for-ssh endpoint instance-id user [5 :minute])
      ))

  (delete [this]
    (with-instance-id
      (debug "deleting" instance-id)
      (delete-volumes endpoint instance-id) 
      (with-ctx ec2/terminate-instances instance-id) 
      (wait-for-status this "terminated" [5 :minute])
      ; for reload support
      (s/update-system (spec :system-id) 
         (dissoc-in* (s/get-system (spec :system-id)) [:aws :instance-id]))
      ))

  (stop [this]
    (with-instance-id 
       (debug "stopping" instance-id)
       (when-not (first (:addresses (with-ctx eip/describe-eip instance-id)))
         (debug "clearing dynamic ip from system")
         (s/update-system (spec :system-id) 
           (dissoc-in* (s/get-system (spec :system-id)) [:machine :ip])))
       (with-ctx ec2/stop-instances instance-id) 
       (wait-for-status this "stopped" [5 :minute])))

  (status [this] 
    (try+ 
      (with-instance-id 
         (instance-desc endpoint instance-id :state :name)) 
      (catch [:type ::aws:missing-id] e 
        (debug "No AWS instance id, most chances this instance hasn't been created yet") false))))

(def defaults {:aws {:min-count 1 :max-count 1}})

(defn aws-spec 
  "creates an ec2 spec" 
  [{:keys [aws machine] :as spec}]
  (merge-with merge (dissoc-in* spec [:aws :endpoint]) defaults))

(defmethod translate :aws [{:keys [aws machine] :as spec}] 
  [(aws :endpoint) (aws-spec spec) (or (machine :user) "root")])

(defmethod vconstruct :aws [spec]
  (apply ->Instance (translate spec)))

(comment 
  (use 'celestial.fixtures)
  (def m (vconstruct celestial.fixtures/puppet-ami)) 
  (:endpoint m )
  (.status m)
  (.start m)
  (clojure.pprint/pprint 
    (celestial.model/set-env :dev 
      (instance-desc  "i-cdd59781") ))

  (clojure.pprint/pprint 
    (celestial.model/set-env :dev 
     (first (:addresses (eip/describe-eip (assoc (creds) :endpoint "ec2.eu-west-1.amazonaws.com") "i-cdd59781"))))) 
  ) 


