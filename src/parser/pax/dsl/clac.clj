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
  (fn [specs value]
    (if-let [valid (get-in specs [:global :file-info :valid])]
      (if-let [month (clac-safe-to-int value)]
        (f/unparse (f/formatters :date)
                   (t/first-day-of-the-month (t/year (t/start valid)) month)))
      )))
  
(defn clac-valid-to []
  (fn [specs value]
    (if-let [valid (get-in specs [:global :file-info :valid])]
      (if-let [month (clac-safe-to-int value)]
        ( f/unparse (f/formatters :date)
                    (t/last-day-of-the-month (t/year (t/start valid)) month)))
      )))

(def clac-spec
  {:global {:output-separator ","}

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
             ]

   :output (generic-pax-output)
   }
  )

(defn test-clac[]
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/CLAC/2013/04/CLAC_2013.xls"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/CLAC/2013/04/CLAC_2013.csv"
        sheet "Registro Datos"]
    (convert-pax-file f1 clac-spec f2 sheet)))
