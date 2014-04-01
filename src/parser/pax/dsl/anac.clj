(ns parser.pax.dsl.anac
(:use [parser.core])
(:use [parser.pax.core])
(:use [parser.pax.dsl.generic-pax-output));;
;; ANAC
;;
(defn anac-trim []
  (fn [ctxt value]
    (clojure.string/trim value)))

(def anac
  {:global {:thousand-separator "."
            :decimal-separator ""
            }

   :input [{:index 0 :name "icao_name" :split (split-into-cells ["icao" "name"] "-")}
           {:index 4 :name "domtot"}
           {:index 5 :name "inttot"}
           {:index 6 :name "tottot"}
           ]
   :columns [{:name "icao" :skip-row (cell-contains? "Superintendência") :transform (anac-trim)}
             {:name "name" :transform (anac-trim)}]

   :footer [(line-contains? ["INFRAERO"])]
   
   :output [{:name "type" :value "airport"}
            {:name "iata" :value ""}
            {:name "icao"}
            {:name "name"}
            {:name "region" :value ""}
            {:name "domtot"}
            {:name "inttot"}
            {:name "tottot"}]
   })
