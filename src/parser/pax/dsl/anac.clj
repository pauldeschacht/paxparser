(ns parser.pax.dsl.anac
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))
;;
;; ANAC
;;
(defn anac-trim []
  (fn [ctxt value]
    (if (not (empty? value))
      (clojure.string/trim value)
      value)))

(defn anac-convert-to-int []
  (fn [specs value]
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

(def anac-spec-temp
  {:global {:thousand-separator "."
            :decimal-separator ""
            :output-separator "\t"
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
   :output [{:name "paxtype" :value "airport"}
            {:name "origin-airport-icao"}
            {:name "origin-airport-iata"}
            {:name "origin-airport-name"}
            {:name "totdom"}
            {:name "totint"}
            {:name "tottot"}
            {:name "transit" :value ""}]
   })


(defn test-anac []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2013/12/ANAC.xlsx"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2013/12/test.csv"
        sheetname "Sheet1"
        file-info (extract-file-information f1)
        specs (merge anac-spec {:global (merge (:global anac-spec) {:file-info file-info})})
        specs* (add-defaults-to-specs specs)
        params {:filename f1 :sheetname sheetname :max 200}
        lines (read-lines params)
        ]

    (->> lines
         (wrap-text-lines)
         (skip-lines (:skip specs*))
         (remove-skip-lines)
         (tokenize-lines (re-pattern (get-in specs* [:global :token-separator])))
         (lines-to-cells (:tokens specs*))
         (merge-lines-with-column-specs (:columns specs*))
         (add-new-column-specs-lines (:columns specs*))
         (transform-lines specs*)
         (output-lines specs*)
         (clean-outputs-lines)
         (outputs-to-csv-lines (get-in specs* [:global :output-separator]))
         (csv-outputs-to-file f2)
         )
    ))

(defn test-anac-temp []
  (let [f1 "/home/pdeschacht/MyDocuments/ANAC_201402.xlsx"
        f2 "/home/pdeschacht/ANAC_201402.csv"
        sheetname "Sheet1"
        ;;file-info (extract-file-information f1)
        file-info {}
        specs (merge anac-spec-temp {:global (merge (:global anac-spec-temp) {:file-info file-info})})
        specs* (add-defaults-to-specs specs)
        params {:filename f1 :sheetname sheetname}
        lines (read-lines params)
        ]

    (->> lines
         (wrap-text-lines)
         (skip-lines (:skip specs*))
         (remove-skip-lines)
         (tokenize-lines (re-pattern (get-in specs* [:global :token-separator])))
         (lines-to-cells (:tokens specs*))
         (merge-lines-with-column-specs (:columns specs*))
         (add-new-column-specs-lines (:columns specs*))
         (transform-lines specs*)
         (output-lines specs*)
         (clean-outputs-lines)
         (outputs-to-csv-lines (get-in specs* [:global :output-separator]))
         (csv-outputs-to-file f2)
         )
    ))

