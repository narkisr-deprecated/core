(ns celestial.schedule
  "Scheduled tasks"
 (:require 
   [celestial.common :refer (get* resolve- import-logging)]
   [chime :refer [chime-ch]]
   [components.core :refer (Lifecyle)]
   [clj-time.core :as t]
   [clj-time.periodic :refer  [periodic-seq]]
   [clojure.core.async :as a :refer [<! close! go-loop]]))  

(import-logging)

(defn schedule [f chimes args]
 (go-loop []
   (when-let [msg (<! chimes)]
     (f args)
     (recur))))

(defn time-fn [unit]
  (resolve-  (symbol (str "clj-time.core/" (name unit)))))

(defn load-schedules 
  "Load all scheduled tasks"
   []
  (doseq [[f m] (get* :scheduled) :let [{:keys [every args]} m]]
    (let [[t unit] every]
      (schedule (resolve- f) (chime-ch (periodic-seq (t/now) ((time-fn unit) t))) args))))

(defn schedules []
  (map 
    (fn [[f {:keys [every args]}]]
      (let [[t unit] every]
        [(resolve- f) (chime-ch (periodic-seq (t/now) ((time-fn unit) t))) args])) (get* :scheduled)))

(defrecord Schedule
  [scs] 
  Lifecyle
  (setup [this])
  (start [this]
    (info "Starting scheduled tasks")
    (doseq [s scs] (apply schedule s)))
  (stop [this]
    (doseq [[_ c _] scs] (close! c))))

(defn instance []
  (Schedule. (schedules)))
