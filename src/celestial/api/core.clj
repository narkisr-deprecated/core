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

(ns celestial.api.core
  "Core api components"
  (:require 
    [clojure.java.io :refer (file)]
    [celestial.api :refer (app)]
    [ring.adapter.jetty :refer (run-jetty)] 
    [components.core :refer (Lifecyle)] 
    [celestial.common :refer (get! get* import-logging)]
    ))

(import-logging)

(def jetty (atom nil))

(defrecord Jetty 
  [] 
  Lifecyle
  (setup [this])

  (start [this]
    (info "Starting jetty")
    (reset! jetty
      (run-jetty (app true)
       {:port (get! :celestial :port) :join? false }))
    )
  (stop [this]
    (when @jetty 
      (info "Stopping jetty")
      (.stop @jetty)
      (reset! jetty nil))))

(defn instance 
   "creates a Elastic components" 
   []
  (Jetty.))
