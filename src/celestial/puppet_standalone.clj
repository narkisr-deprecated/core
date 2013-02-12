(ns celestial.puppet-standalone
  "A standalone puppet provisioner"
  (:import com.jcraft.jsch.JSchException)
  (:use 
    clojure.core.strint
    celestial.core
    clj-ssh.ssh
    [taoensso.timbre :only (debug info error warn)]
    [clojure.string :only (join)]
    [slingshot.slingshot :only  [throw+ try+]]))



(def ssh-opts {:username "root" :strict-host-key-checking :no})

(defn with-session [host f]
  (let [session (session (ssh-agent {}) host ssh-opts)] 
    (try+
      (with-connection session (f session))
      (catch #(= (:message %) "Auth fail") e
        (throw+ {:type ::auth :host host} 
                "Failed to login make sure to ssh-copy-id to the remote host")))))

(defn put [host file dest]
  (with-session host
    (fn [session]
      (let [channel (ssh-sftp session)]
        (with-channel-connection channel
          (sftp channel {} :put file dest))))))

(defn copy [server module]
  (put (:host server) (str (module :src) (module :name) ".tar.gz")  "/tmp"))

(def os  (java.io.ByteArrayOutputStream.))

(defn log-output [out]
  (doseq [line (line-seq (clojure.java.io/reader out))] (debug line) )) 

(defn execute [{:keys [host]} & batches]
  "Executes remotly using ssh for example: (execute {:host \"192.168.20.171\"} [\"ls\"])"
  (with-session host
    (fn [session]
      (doseq [b batches]
        (let [{:keys [channel out-stream] :as res} (ssh session {:in  (join "\n" b)  :out :stream})]
          (log-output out-stream)
          (let [exit (.getExitStatus channel)]
            (when-not (= exit 0) 
              (throw+ (merge res {:type ::provision-failed :exit exit} (meta b))))))))))

(defn step [n & steps] ^{:step n} steps)

(deftype Standalone [server module]
  Provision
  (apply- [this]
    (use 'celestial.puppet-standalone)
    (use 'celestial.core)
    (copy server module) 
    (execute server 
      (step :extract "cd /tmp" (<< "tar -xzf ~(:name module).tar.gz")) 
      (step :run (<< "cd /tmp/~(:name module)") "./run.sh")
      (step :cleanup "cd /tmp" (<< "rm -rf ~(:name module)*"))) ))


#_(.apply-
    (Standalone. {:host "192.168.5.203"} {:name "puppet-base-env" :src "/home/ronen/code/"}))

