(ns parser.pax.dsl.bts-db1b
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))

(defn bts-trim []
  (fn [specs value]
    (if (empty? value)
      nil
      (clojure.string/trim value))))

(defn bts-clean []
  (fn [specs value]
    (if (empty? value)
      nil
      (-> value
          (clojure.string/replace "\"" "")
          (clojure.string/trim)))))

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
            {:index 33 :name "pax"}
            ]
   
   :columns [{:name "paxtype" :value "airportpair"}
             {:name "paxsource" :value "DB1B"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-to)}
             {:name "metric" :value "pax"}
             {:name "segment" :value true}
             
             {:name "origin-airport-iata" :transform (bts-clean)}
             {:name "origin-country-iata" :transform (bts-clean)}
             {:name "destination-airport-iata" :transform (bts-clean)}
             {:name "destination-country-iata" :transform (bts-clean)}
             
             ]
   
   :output (generic-pax-output)

   })

(defn test-bts-db1b []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTS/DB1B/2013/Q3/BTSDB1B.csv"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTS/DB1B/2013/Q3/test.csv"
        sheetname ""
        file-info (extract-file-information f1)
        specs (merge bts-db1b-spec
                     {:global (merge (:global bts-db1b-spec)
                                     {:file-info file-info})})
        specs* (add-defaults-to-specs specs)
        params {:filename f1 :sheetname sheetname :max 200}
        lines (read-lines params)
        ]

    (->> lines
         (wrap-text-lines)
         (skip-lines (:skip specs*))
         (stop-after (:stop specs*))
         (remove-skip-lines)
         (tokenize-lines (re-pattern (get-in specs* [:global :token-separator])))
         (map #(dissoc %1 :text))
         (lines-to-cells (:tokens specs*))
         (map #(dissoc %1 :tokens))
         (merge-lines-with-column-specs (:columns specs*))
         (add-new-column-specs-lines (:columns specs*))
         (transform-lines specs*)
         (repeat-down-lines specs*)
         (output-lines specs*)
         (clean-outputs-lines)
         (outputs-to-csv-lines (get-in specs* [:global :output-separator]))
         (csv-outputs-to-file f2)
         )
    )
  )
