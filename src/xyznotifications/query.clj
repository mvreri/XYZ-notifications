(ns xyznotifications.query
  (:require [taoensso.timbre :as timbre]
            [ring.middleware.json :as middleware]
            [clojure.data.json :as json]
            [korma.db :refer :all]
            [xyznotifications.config :as config]
            [clj-http.client :as client]
            [noir.util.crypt :as crypt]
            [clj-time.core :as time]
            [clj-time.coerce :as tc]
            [korma.core :refer :all]
            [throttle.core :as throttle]

            )
  )

(config/initialize-db)

(def db-connection-xyz
  {:classname "org.postgresql.Driver"
   :subprotocol config/db-subprotocol
   :user config/db-user
   :password config/db-pass
   :subname config/db-subname
   })

;managing requests per user
(def api-throttler (throttle/make-throttler :phone, :attempts-threshold 2, :initial-delay-ms 20000, :delay-exponent 0 ))
(def client-throttler (throttle/make-throttler :apikey, :attempts-threshold 10, :initial-delay-ms 20000, :delay-exponent 0 ))

(defn get-users [phone]
  (throttle/check api-throttler phone)
  (with-db db-connection-xyz (exec-raw (format "SELECT fname from users
                                                WHERE phone='%s' limit 1;" phone) :results ))

  )

(defn get-messages [phone]
  (with-db db-connection-xyz (exec-raw (format "SELECT fname from users
                                                WHERE phone='%s' limit 1;" phone) :results ))


  )