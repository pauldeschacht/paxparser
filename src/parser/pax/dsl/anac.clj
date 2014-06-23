(ns parser.pax.dsl.anac
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))
;;
;; ANAC
;;
(defn anac-trim []
  (fn [ctxt value & line]
    (if (not (empty? value))
      (clojure.string/trim value)
      value)))

(defn anac-convert-to-int []
  (fn [specs value & line]
    (if (empty? value)
      nil
      (let [value* (clojure.string/replace value #"[\.,]" "")]
        (try
          (Integer/parseInt value*)
          (catch Exception e (do (println (.getMessage e))
                                 nil)))
        ))))
  
(def anac-spec
  {:global {:thousand-separator "."
            :decimal-separator ""
            :output-separator ","
            }

   :skip [(line-contains? ["Dependência" "Superintendência" "INFRAERO"])]
   
   :tokens [{:index 0 :name "icao_name" :split (split-into-cells ["origin-airport-icao" "origin-airport-name"] "-")}
            {:index 4 :name "totdom"}
            {:index 5 :name "totint"}
            {:index 6 :name "tottot"}
           ]
   :columns [{:name "paxtype" :value "airport"}
             {:name "paxsource" :value "ANAC"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-to)}
             {:name "metric" :value "pax"}
             {:name "segment" :value nil}
             {:name "origin-aiport-icao" :transform (anac-trim)}
             {:name "origin-airport-name" :transform (anac-trim)}
             {:name "totdom" :transform (anac-convert-to-int)}
             {:name "totint" :transform (anac-convert-to-int)}
             {:name "tottot" :transform (anac-convert-to-int)}]
   :output (generic-pax-output)
   })

(defn test-anac []
  (let [in "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2013/12/ANAC.xlsx"
        out "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2013/12/ANAC.csv"
        sheet "Sheet1"
        ]
    (convert-pax-file in anac-spec out sheet)
    )
  )

(defn anac-all [folder]
  (for [year (range 2012 2013)
        month (range 1 13)]
    (let [str-month (format "%02d" month)
          in (str folder "/" year "/" str-month "/01_importAirport.xls")
          out (str folder "/" year "/" str-month "/02_importAirport.csv")
          sheetname "Sheet1"
          _ (println (str "Converting file " in " into " out))
          ]
      (convert-pax-file in anac-spec out)
      )))

