(ns xyznotifications.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :as middleware]
            [xyznotifications.query :as query]
            [taoensso.timbre :as timbre]
            [clojure.data.json :as json]
            [xyznotifications.config :as config]
            [xyznotifications.config :as api]
            [ring.util.response :refer [response redirect]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            )
  )


(defroutes app-routes
           (POST "/" request
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
                                                           :title (str "Limit Exceeded" )
                                                           :attributes (.getMessage e)
                                                           }
                                                    })
                                      )
                  :body-encoding "UTF-8" :content-type :json}
                                  )
               )

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
                                         :status 500
                                         :title "Invalid Link Found"
                                         :description "Link seems to be down or non-existent"
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
