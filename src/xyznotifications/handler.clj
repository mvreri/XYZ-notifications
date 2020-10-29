(ns xyznotifications.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :as middleware]
            [xyznotifications.query :as query]
            [taoensso.timbre :as timbre]
            [clojure.data.json :as json]
            [xyznotifications.config :as config]
            [xyznotifications.config :as api]
            [throttler.core :refer [throttle-chan throttle-fn fn-throttler]]
            [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]
            [throttle.core :as throttle]
            [ring.util.response :refer [response redirect]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            )
  )

;(def daily-api-throttler-single (fn-throttler 4 :minute))
(def daily-api-throttler-everyone (fn-throttler 4 :day))
;(def api-throttler (throttle/make-throttler :email, :attempts-threshold 2, :initial-delay-ms 20000, :delay-exponent 0 ))
(def api-throttler (throttle/make-throttler :email, :attempts-threshold config/attempts-threshold, :initial-delay-ms config/initial-delay-ms, :delay-exponent config/delay-exponent ))
(def get-messages-db (daily-api-throttler-everyone query/get-messages))

;(def apit-throttler (throttler/throttle :phone, :COMPOJURE_THROTTLE_TTL config/COMPOJURE_THROTTLE_TTL, :COMPOJURE_THROTTLE_TOKENS config/COMPOJURE_THROTTLE_TOKENS ))

(defroutes app-routes
           #_(POST "/" request
             (try
               {:status 200
                :body (with-out-str (json/pprint {:data {
                                                         :status 200
                                                         :title (str "User Details" )
                                                         :attributes (query/get-messages (get-in (:body request) ["phone"]))
                                                         }
                                                  })
                                    )
                :body-encoding "UTF-8" :content-type :json}

               (catch Exception e
                 {:status 200
                  :body (with-out-str (json/pprint {:errors {
                                                           :status 403
                                                           :title (str "Error" )
                                                           :attributes (.getMessage e)
                                                           }
                                                    })
                                      )
                  :body-encoding "UTF-8" :content-type :json}
                                  )
               )

             )

           ;metabase/throttle
           ;ANY - POST/GET
           (POST "/users" request
             ;we check whether the daily limit has been reached
             (get-messages-db (get-in (:body request) ["phone"]))
             ;if not,
             ;email is passed through the header and is used to determine the number of requests per client
             (throttle/check api-throttler (:email (clojure.walk/keywordize-keys (:headers request)) ))
             ;(get-messages-db (get-in (:body request) ["phone"]))
             {:status 200
              :body (json/write-str {:errors {
                                              :status 200
                                              :title (str "Users")
                                              :attributes (get-messages-db (get-in (:body request) ["phone"]))
                                              }
                                     }
                                    )
              :body-encoding "UTF-8" :content-type :json}

             )

           (route/resources "/")
           (route/not-found "Not Found")
           )

;take care of requests with no response
(defn wrap-exception [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        #_(println "--------------" request)
        {:status 200
         :body (json/write-str {:errors {
                                         :status 429
                                         :title "Request limit reached"
                                         :description (.getMessage e)
                                         }
                                }
                               )
         :body-encoding "UTF-8" :content-type :json}
        )))
  )


(def app
  (-> app-routes
      (middleware/wrap-json-body)
      (middleware/wrap-json-response)
      (wrap-defaults app-routes)
      (wrap-exception)

      ))
