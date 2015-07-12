(ns celestial.persistency.stacks
  "Stack model"
  (:require 
    [subs.core :as subs :refer (validate! validation when-not-nil every-v every-kv)]
    [puny.core :refer (entity)]
    ))


(entity {:version 1} stack)

(validation :count {
  :template #{:required :Keyword} :count #{:required :Integer} :instances #{:Vector}
})

(validation :system* (every-v #{:count}))

(validation :shared* 
  (every-kv {
     :machine #{:Map} :openstack #{:Map}
   }))

(def stack-base {
   :systems #{:system*}
   :defaults #{:required :shared*}
})

(defn validate-stack
  [stack]
  (validate! stack stack-base :error ::non-valid-stack))
