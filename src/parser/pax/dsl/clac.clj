(ns parser.pax.dsl.clac
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output])
  (:use [clj-time.core :exclude (second extend) :as t])
  (:use [clj-time.format :as f])
  )

(defn clac-safe-to-int [value]
  (if (empty? value)
    nil
    (try
      (int (Double/parseDouble value))
      (catch Exception e
        nil)
      )))

(defn clac-valid-from []
  (fn [specs value & line]
    (if-let [valid (get-in specs [:global :file-info :valid])]
      (if-let [month (clac-safe-to-int value)]
        (f/unparse (f/formatters :date)
                   (t/first-day-of-the-month (t/year (t/start valid)) month)))
      )))
  
(defn clac-valid-to []
  (fn [specs value & line]
    (if-let [valid (get-in specs [:global :file-info :valid])]
      (if-let [month (clac-safe-to-int value)]
        ( f/unparse (f/formatters :date)
                    (t/last-day-of-the-month (t/year (t/start valid)) month)))
      )))

(def clac-spec
  {:global {:output-separator "\t"} ;; airline contains ,
   ;;TODO use csv/writer and use quotes ?
   ;;TODO or use a safe separator

   :skip [(line-contains? ["CIUDAD-DESTINO"])]

   :tokens [{:index 0 :name "origin-country-name"}
            {:index 1 :name "airline-name"}
            {:index 2 :name "origin-city-name"}
            {:index 3 :name "destination-city-name"}
            {:index 4 :name "deptot"}
            {:index 5 :name "arrtot"}
            {:index 8 :name "valid-from"}
            ]

   :columns [{:name "paxtype" :value "citypair"}
             {:name "paxsource" :value "ACSA"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "deptot" :transform (convert-to-int)}
             {:name "arrtot" :transform (convert-to-int)}
             {:name "valid-from" :transform (clac-valid-from)}
             {:name "valid-to" :merge (merge-from ["valid-from"] "") :transform (clac-valid-to)}
             {:name "metric" :value "pax"}
             {:name "segment" :value false}
             ]

   :output (generic-pax-output)
   }
  )
