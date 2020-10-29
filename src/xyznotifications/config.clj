(ns xyznotifications.config
  (:require [clojure.tools.logging :as log])
  (:import  [java.util Properties]
            [java.io FileInputStream]
            )
  (:use [clojure.walk])
  (:use korma.core)
  (:use korma.db))

(def app-configs (atom {}))

(defn- config-file []
  ;this is the env variable
  (let [result (let [result (System/getenv "xyznotifications")]
                 (when result (java.io.File. result)))]
    (if (and result (.isFile result))
      result
      (do (log/fatal (format "serverConfig(%s) = nil" result))
          (throw (Exception. (format "Server configuration file (%s) not found." result)))))))

(defn load-config [config]
  (let [properties (Properties.)
        fis (FileInputStream. config)]
    ; populate the properties hashmap with values from the output stream
    (.. properties (load fis))
    (keywordize-keys (into {} properties))))

(defn config-value [name & args]
  (let [value (@app-configs name)]
    (when value
      (let [args (when args (apply assoc {} args))
            {type :type} args
            args (dissoc args :type)
            value (if (vector? value)
                    (loop [x (first value)
                           xs (next value)]
                      (let [properties (dissoc x :value)]
                        (if (or (and (empty? args)
                                     (empty? properties))
                                (and (not (empty? args))
                                     (every? (fn [[k v]]
                                               (= (properties k) v))
                                             args)))
                          (x :value)
                          (when xs
                            (recur (first xs) (next xs))))))
                    value)]
        (when value
          (let [value #^String value]
            (cond (or (nil? type) (= type :string)) value
                  ;; ---
                  (= type :int) (Integer/valueOf value)
                  (= type :long) (Long/valueOf value)
                  (= type :bool) (contains? #{"yes" "true" "y" "t" "1"}
                                            (.toLowerCase value))
                  (= type :path) (java.io.File. value)
                  (= type :url) (java.net.URL. value)
                  )))))))

(defn initialize-config []
  (log/info "initializing configurations..")
  (reset! app-configs (load-config (config-file)))


  ;; database configs
  (def db-host (config-value :db-host))
  (def db-port (config-value :db-port))
  (def db-name (config-value :db-name))
  (def db-user (config-value :db-user))
  (def db-pass (config-value :db-password))
  (def db-subprotocol (config-value :db-subprotocol))
  (def db-subname (config-value :db-subname))

  ;:attempts-threshold 10, :initial-delay-ms 20000, :delay-exponent 0
  (def attempts-threshold (config-value :attempts-threshold))
  (def initial-delay-ms (config-value :initial-delay-ms))
  (def delay-exponent (config-value :delay-exponent))

  (def COMPOJURE_THROTTLE_TTL (config-value :COMPOJURE_THROTTLE_TTL))
  (def COMPOJURE_THROTTLE_TOKENS (config-value :COMPOJURE_THROTTLE_TOKENS))
  (def COMPOJURE_THROTTLE_RESPONSE_CODE (config-value :COMPOJURE_THROTTLE_RESPONSE_CODE))



  (println "Host -> "db-host)
  (println "Port -> "db-port)
  (println "Name -> "db-name)
  (println "User -> "db-user)
  (println "Pass -> "db-pass)
  (println "Protocol -> "db-subprotocol)
  (println "Subname -> "db-subname)


  )


(defn initialize-db []
  (initialize-config)
  (defdb xyz (postgres {:db db-name
                                   :user db-name
                                   :password db-pass
                                   :host db-host
                                   :port db-port
                                   :delimiters ""}))
  )
