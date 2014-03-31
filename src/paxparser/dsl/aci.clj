(ns paxparser.dsl.aci
  (:use [paxparser.core]))
;;
;; ACI INTERNATIONAL & WORLDWIDE
;;
(defn aci-pax-to-int []
  (fn [value]
    (if (= value "*****")
      nil
      (read-string (clojure.string/replace value " " ""))
      )))

(defn aci-trim []
  (fn [value]
    (clojure.string/trim value)))

(def aci-spec
  {:global {:token-separator ";"
            :thousand-separator " "
            :decimal-separator "."}
   
   :skip [(line-contains? ["CODE" "COUNTRY"])
          ]
   :tokens [{:index 0 :name "region"}
           {:index 1 :name "city-country-code" :split (split-into-cells ["city" "country"] ",")}
           {:index 2 :name "iata"}
           {:index 3 :name "tottot"}
           ]
   :columns [{:name "tottot" :transform (aci-pax-to-int)}
             {:name "city" :transform (aci-trim)}
             {:name "country" :transform (aci-trim)}
             {:name "name" :transform (aci-trim)}
             ]
   :output [{:name "type" :value "airport"}
            {:name "iata"}
            {:name "city"}
            {:name "region"}
            {:name "country"}
            {:name "tottot"}
            ]
   })
