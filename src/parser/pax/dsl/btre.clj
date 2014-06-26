(ns parser.pax.dsl.btre
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))

(defn btre-clean-value [fun]
  (let [function (fun)]
    (fn [specs value & line]
      (if (empty? value)
        nil
        (let [value* (clojure.string/trim value)]
          (if (or
               (= value* "..")
               (= value* "-")
               )
            nil
            (function specs value* line)))))))

;;
;; BTRE DOMESTIC
;;
(defn btre-trim []
  (fn [specs value & line]
    (if (empty? value)
      nil
      (clojure.string/trim value))))

(def btre-dom-spec
  {:global {:thousand-separator " "
            :output-separator ","
            }
   :skip [(line-empty?)
          (line-contains? ["Passenger" "City-Pair"])]

   :stop [(line-contains? ["Total domestic network"])]
   
   :tokens [{:index 1 :name "city-pair" :split (split-into-cells ["origin-city-name" "destination-city-name"] "-")}
           {:index 3 :name "tottot"}
           ]
   :columns [{:name "paxtype" :value "citypair"}
             {:name "paxsource" :value "BTRE"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-to)}
             {:name "origin-city-name" :transform (btre-trim)}
             {:name "destination-city-name" :transform (btre-trim)}
             {:name "metric" :value "pax"}
             {:name "segment" :value nil}
             {:name "tottot" :transform (convert-to-int)}]
   
   :output (generic-pax-output)
   })
;;
;; BTRE INTERNATIONAL
;;
(def btre-int-airportpair-spec
  {:global {:thousand-separator " "
            :decimal-separator "."
            :output-separator ","
            }
   :skip [(line-contains? ["TABLE" "Passengers" "Freight" "Foreign" "Inbound" "Outbound" "Total" "ALL SERVICES"])
          (line-empty?)]

   :stop [(line-contains? ["explanatory notes"])]
   
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
   :stop [(line-contains? ["Please" "In addition to the above"])]
   
   :tokens [{:index 0 :name "airline-name" }
            {:index 1 :name "country" }
            {:index 3 :name "arrint pax"}
            {:index 5 :name "arrint slf"}
            {:index 8 :name "depint pax"}
            {:index 10 :name "depint slf"}
            ]
   
   :columns [{:name "paxtype" :value "countrypairairline"}
             {:name "paxsource" :value "BTRE"}
             {:name "fullname" :transform (get-fullname)}
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
   
   :outputs [(merge-pax-output (generic-pax-output)
                               [{:name "metric" :value "pax"}
                                {:name "origin-country-name" :value "Australia"}
                                {:name "destination-country-name" :source "country"}
                                {:name "depint" :source "depint pax"}
                                {:name "arrint" :source "arrint pax"}
                                ]
                               )
             (merge-pax-output (generic-pax-output)
                               [{:name "metric" :value "slf"}
                                {:name "origin-country-name" :value "Australia"}
                                {:name "destination-country-name" :source "country"}
                                {:name "depint" :source "depint slf"}
                                {:name "arrint" :source "arrint slf"}
                                 ]
                                )
              ]
   })
