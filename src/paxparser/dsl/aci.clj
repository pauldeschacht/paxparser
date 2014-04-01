(ns paxparser.dsl.aci
  (:use [paxparser.core]))
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

(defn get-filename []
  (fn [specs value]
    (:filename specs)))

(defn get-capture-date []
  (fn [specs value]
    (:capture-date specs)))

(defn get-valid-from []
  (fn [specs value]
    (:valid-from specs)))

(defn get-valid-to []
  (fn [specs value]
    (:valid-to specs)))


(def aci-spec
  {:global {:thousand-separator " "
            :decimal-separator "."}
   
   :skip [(line-contains? ["CODE" "COUNTRY"])
          ]
   :tokens [{:index 0 :name "region"}
           {:index 1 :name "city-country-code" :split (split-into-cells ["city" "country"] ",")}
           {:index 2 :name "origin-airport-iata"}
           {:index 3 :name "tottot"}
           ]
   :columns [{:name "paxtype" :value "PaxAirport"}
             {:name "paxsource" :value "ACI"}
             {:name "fullname" :transform (get-filename)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-to)}
             
             {:name "tottot" :transform (aci-pax-to-int)}
             {:name "origin-city-name" :transform (aci-trim)}
             {:name "origin-country-iata" :transform (aci-trim)}]
   :output [{:name "type" :value "airport"}
            {:name "iata"}
            {:name "city"}
            {:name "region"}
            {:name "country"}
            {:name "tottot"}
            ]
   })

