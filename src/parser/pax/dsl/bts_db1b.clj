(ns parser.pax.dsl.bts-db1b
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output])
  (:use [clj-time.core :exclude (second extend) :as t])
  (:use [clj-time.format :as f])
  )

(defn bts-trim []
  (fn [specs value & line]
    (if (empty? value)
      nil
      (clojure.string/trim value))))

(defn bts-take-city-name []
  (fn [specs value & line]
    (if-let [tokens (clojure.string/split value #",")]
      (first tokens)
      "")))

(defn bts-safe-to-int [value]
  (if (empty? value)
    nil
    (try
      (int (Double/parseDouble value))
      (catch Exception e
        nil)
      )))

(defn bts-valid-from []
  (fn [specs value & line]
    (let [tokens (clojure.string/split value (re-pattern "-"))
          year (bts-safe-to-int (first tokens))
          month (bts-safe-to-int (second tokens))
          ]
      (t/first-day-of-the-month year month)))
  )

(defn bts-valid-to []
  (fn [specs value & line]
    (let [tokens (clojure.string/split value (re-pattern "-"))
          year (bts-safe-to-int (first tokens))
          month (bts-safe-to-int (second tokens))
          ]
      (t/last-day-of-the-month year month)))
  )

(defn bts-clean []
  (fn [specs value & line]
    (if (empty? value)
      nil
      (-> value
          (clojure.string/replace "\"" "")
          (clojure.string/trim)))))


(def bts-t100-spec
  {:global {:decimal-separator "."
            :output-separator ","
            :token-separator ","}

   :skip [(line-contains? ["DEPARTURES"])]

   :tokens [{:index 1 :name "movements"}
            {:index 3 :name "seats"}
            {:index 4 :name "pax"}
            {:index 44 :name "year"}
            {:index 46 :name "month"}
            {:index 10 :name "airline-iata"}
            {:index 12 :name "airline-name"}
            {:index 22 :name "origin-airport-iata"}
            {:index 23 :name "origin-city-name" }
            {:index 27 :name "origin-country-iata"}
            {:index 28 :name "origin-country-name"}
            {:index 32 :name "destination-airport-iata"}
            {:index 33 :name "destination-city-name"}
            {:index 38 :name "destination-country-iata"}
            {:index 39 :name "destination-country-name"}
            ]

   :columns [{:name "paxsource" :value "BTS"}
             {:name "paxtype" :value "citypairairline"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "movements" :transform (convert-to-int)}
             {:name "seats" :transform (convert-to-int)}
             {:name "pax" :transform (convert-to-int)}
             {:name "origin-city-name" :transform (bts-take-city-name)}
             {:name "destination-city-name" :transform (bts-take-city-name)}
             {:name "segment" :value true}
             {:name "valid-from" :merge (merge-from ["year" "month"] "-") :transform (bts-valid-from)}
             {:name "valid-to"   :merge (merge-from ["year" "month"] "-") :transform (bts-valid-to)}
             
             ]

   :outputs [(merge-pax-output (generic-pax-output)
                               [{:name "metric" :value "pax"}
                                {:name "tottot" :source "pax"}])

             (merge-pax-output (generic-pax-output)
                               [{:name "metric" :value "movs"}
                                {:name "tottot" :source "movements"}])

             (merge-pax-output (generic-pax-output)
                               [{:name "metric" :value "seats"}
                                {:name "tottot" :source "seats"}])
             ]
   }
  )

(def bts-db1b-spec
  {:global {:thousand-separator ""
            :decimal-sepatator "."
            :token-separator ","
            :output-separator ","
            }

   :skip [(line-contains? ["ItinID"])]

   :tokens [{:index 8 :name "origin-airport-iata"}
            {:index 9 :name "origin-country-iata"}
            {:index 17 :name "destination-airport-iata"}
            {:index 18 :name "destination-country-iata"}
            {:index 33 :name "deptot"}
            ]
   
   :columns [{:name "paxtype" :value "airportpair"}
             {:name "paxsource" :value "DB1B"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-to)}
             {:name "metric" :value "pax"}
             {:name "segment" :value false}
             
             {:name "origin-airport-iata" :transform (bts-clean)}
             {:name "origin-country-iata" :transform (bts-clean)}
             {:name "destination-airport-iata" :transform (bts-clean)}
             {:name "destination-country-iata" :transform (bts-clean)}
             
             ]
   
   :output (generic-pax-output)

   })

(defn test-bts-t100 []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTS/T100/2013/08/BTS_T100.csv"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTS/T100/2013/08/BTS_T100_parsed.csv"]
    (convert-pax-file f1 bts-t100-spec f2))
  )
