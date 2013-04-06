(ns celestial.test.tasks
  (:use 
    midje.sweet
    [celestial.config :only (config)]
    [celestial.tasks :only (resolve- post-create-hooks)])  
  (:import clojure.lang.ExceptionInfo)
 )

(defn foo-hook [v] v)

(def identity-hook
  {:hooks {:post-create {'celestial.test.tasks/foo-hook {:foo 1}}}})

(let [machine {:machine {:hostname "foo" :ip_address "192.168.2.1"}} merged (merge machine {:foo 1})]
  (with-redefs [config identity-hook]
    (fact ""
      (post-create-hooks machine) => nil
      (provided 
        (foo-hook merged) => merged :times 1))))

(fact "missing fn resolution error"
  (resolve- "non.existing/fn") => (throws ExceptionInfo))
