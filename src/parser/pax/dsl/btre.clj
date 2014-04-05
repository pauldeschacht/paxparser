(ns parser.pax.dsl.btre
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))

(defn btre-clean-value [fun]
  (let [function (fun)]
    (fn [specs value]
      (if (empty? value)
        nil
        (let [value* (clojure.string/trim value)]
          (if (or
               (= value* "..")
               (= value* "-")
               )
            nil
            (function specs value*)))))
    )
  )

;;
;; BTRE DOMESTIC
;;
(defn btre-trim []
  (fn [specs value]
    (if (empty? value)
      nil
      (clojure.string/trim value))))

(def btre-dom-spec
  {:global {:thousand-separator " "}
   :input [{:index 1 :name "city-pair" :split (split-into-cells ["origin" "destination"] "-")}
           {:index 2 :name "2012"}
           {:index 3 :name "2013"}
           ]
   :columns [{:name "origin" :transform (btre-trim)}
             {:name "destination" :transform (btre-trim)}
             {:name "2012" :transform (convert-to-int)}
             {:name "2013" :transform (convert-to-int)}]
   
   :output [{:name "type" :value "citypair"}
            {:name "origin"}
            {:name "destination"}
            {:name "tottot" :source "2013"}]

   })
;;
;; BTRE INTERNATIONAL
;;
(def btre-int-pax-spec
  {:global {:thousand-separator " "
            :decimal-separator "."
            :output-separator ","
            }
   :skip [(line-contains? ["TABLE" "Passengers" "Freight" "Foreign" "Inbound" "Outbound" "Total" "ALL SERVICES"])
          (line-empty?)]
   

   
   :tokens [{:index 0 :name "origin-airport-name" }
            {:index 1 :name "destination-airport-name" }
            {:index 6 :name "arrint"}
            {:index 7 :name "depint"}
            {:index 8 :name "totint"}
            ]
   
   :columns [{:name "paxtype" :value "citypair"}
             {:name "paxsource" :value "BTRE"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-to)}
             {:name "metric" :value "pax"}
             {:name "segment" :value false }
             
             {:name "destination-airport-name" :repeat-down true}
             {:name "arrint" :transform (convert-to-int)}
             {:name "depint" :transform (convert-to-int)}
             {:name "totint" :transform (convert-to-int)}]
   
   :output (generic-pax-output)
   })

(def btre-int-countrypairairline-spec
  {:global {:thousand-separator " "
            :decimal-separator ""
            :output-separator ","
            }
   :skip [(line-contains? ["TABLE" "Scheduled" "Inbound" "Outbound" "TOTAL" "Please" "Carried" "ALL SERVICES"])
          (line-empty?)]
   :stop [(line-contains? ["Please"])]
   
   :tokens [{:index 0 :name "airline-name" }
            {:index 1 :name "country" }
            {:index 3 :name "arrint pax"}
            {:index 5 :name "arrint slf"}
            {:index 8 :name "depint pax"}
            {:index 10 :name "depint slf"}
            ]
   
   :columns [{:name "paxtype" :value "countrypairairline"}
             {:name "paxsource" :value "BTRE"}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-to)}
             {:name "segment" :value true }

             {:name "airline-name" :repeat-down true}
             {:name "country" :repeat-down true}
             
             {:name "arrint pax" :transform (btre-clean-value convert-to-int)}
             {:name "arrint slf" :transform (btre-clean-value convert-to-double)}
             {:name "depint pax" :transform (btre-clean-value convert-to-int)}
             {:name "depint slf" :transform (btre-clean-value convert-to-double)}
             ]
   
   :outputs [ (merge-pax-output (generic-pax-output)
                                [{:name "metric" :value "pax"}
                                 {:name "origin-country-name" :source "country"}
                                 {:name "destination-country-name" :value "Australia"}
                                 {:name "arrint" :source "arrint pax"}
                                 ]
                                )
              (merge-pax-output (generic-pax-output)
                                [{:name "metric" :value "pax"}
                                 {:name "origin-country-name" :value "Australia"}
                                 {:name "destination-country-name" :source "country"}
                                 {:name "depint" :source "depint pax"}
                                 ]
                                )
              (merge-pax-output (generic-pax-output)
                                [{:name "metric" :value "slf"}
                                 {:name "origin-country-name" :source "country"}
                                 {:name "destination-country-name" :value "Australia"}
                                 {:name "arrint" :source "arrint slf"}
                                 ]
                                )
              (merge-pax-output (generic-pax-output)
                                [{:name "metric" :value "slf"}
                                 {:name "origin-country-name" :value "Australia"}
                                 {:name "destination-country-name" :source "country"}
                                 {:name "depint" :source "depint slf"}
                                 ]
                                )
              ]
   })

(defn test-btre-int-pax []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTRE_Australia/2013/10/BTRE_International_airline_activity_1310_Tables.xls"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTRE_Australia/2013/10/test_int_pax.csv"
        sheetname "Table_5"
        file-info (extract-file-information f1)
        specs (merge btre-int-pax-spec
                     {:global (merge (:global btre-int-pax-spec)
                                     {:file-info file-info})})
        specs* (add-defaults-to-specs specs)
        params {:filename f1 :sheetname sheetname}
        lines (read-lines params)
        ]

    (->> lines
         (wrap-text-lines)
         (skip-lines (:skip specs*))
         (stop-after (:stop specs*))
         (remove-skip-lines)
         (tokenize-lines (re-pattern (get-in specs* [:global :token-separator])))
         (lines-to-cells (:tokens specs*))
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


(defn test-btre-int-countrypairairline []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTRE_Australia/2013/10/BTRE_International_airline_activity_1310_Tables.xls"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTRE_Australia/2013/10/test_countrypairairline.csv"
        sheetname "Table_3"
        file-info (extract-file-information f1)
        specs (merge btre-int-countrypairairline-spec
                     {:global (merge (:global btre-int-countrypairairline-spec)
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
         (lines-to-cells (:tokens specs*))
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
