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

(ns celestial.workflows.common
  (:require 
    [taoensso.timbre :refer  (refer-timbre)]
    [celestial.common :refer (get! resolve-)]
    [taoensso.timbre :refer  (refer-timbre)]
    [clojure.tools.macro :as tm]
    [metrics.timers :refer  [deftimer time!]]))

(refer-timbre)

(defn run-hooks 
  "Runs hooks"
  [args workflow event]
  (doseq [[f conf] (get! :hooks)]
    (debug "running hook" f (resolve- f))
    (try 
      ((resolve- f) (merge args conf {:workflow workflow :event event}))
      (catch Throwable t (error t)) 
      )))

(defmacro deflow
  "Defines a basic flow functions with post-success and post-error hooks"
  [fname & args]
  (let [[name* attrs] (tm/name-with-attributes fname args)
        timer (symbol (str name* "-time"))
        meta-map (meta name*) 
        hook-args (or (some-> (meta-map :hook-args) name symbol) 'spec)]
    `(do
       (deftimer ~timer)
       (defn ~name* ~@(when (seq meta-map) [meta-map]) ~(first attrs)
         (time! ~timer
          (try ~@(next attrs)
           (run-hooks ~hook-args ~(keyword name*) :success)
           (catch Throwable t#
             (run-hooks ~hook-args ~(keyword name*) :error) 
             (throw t#))))))))
 
