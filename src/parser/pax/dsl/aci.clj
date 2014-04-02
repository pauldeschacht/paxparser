(ns parser.pax.dsl.aci
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))
;;
;; ACI INTERNATIONAL & WORLDWIDE
;;
(defn aci-pax-to-int []
  (fn [specs value]
    (if (= value "*****")
      nil
      (read-string (clojure.string/replace value " " ""))
      )))

(defn aci-trim []
  (fn [specs value]
    (if (nil? value)
      value
      (clojure.string/trim value))))


(def aci-spec
  {:global {:thousand-separator " "
            :decimal-separator "."
            :output-separator ","}
   
   :skip [(line-contains? ["CODE" "COUNTRY"])
          ]
   :tokens [{:index 0 :name "region"}
           {:index 1 :name "city-country-code" :split (split-into-cells ["origin-city-name" "origin-country-iata"] ",")}
           {:index 2 :name "origin-airport-iata"}
           {:index 3 :name "tottot"}
           ]
   :columns [{:name "paxtype" :value "airport"}
             {:name "paxsource" :value "ACI"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-to)}

             {:name "metric" :value "pax"}
             {:name "segment" :value nil}
             {:name "tottot" :transform (aci-pax-to-int)}
             {:name "origin-city-name" :transform (aci-trim)}
             {:name "origin-country-iata" :transform (aci-trim)}]
   
   :output (generic-pax-output)
   })

(defn test-aci []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/private-data/2014/02/ACI/monthly_international/2013/10/1013MYTDYEiPaxFrt.xlsx"
        f2 "/home/pdeschacht/dev/paxparser/test/private-data/2014/02/ACI/monthly_international/2013/10/test.csv"
        ]
    (convert-pax-file f1 aci-spec f2 "Sheet1")))
