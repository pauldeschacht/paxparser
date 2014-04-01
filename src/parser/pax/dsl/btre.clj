(ns parser.pax.dsl.btre
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))
;;
;; BTRE DOMESTIC
;;
(defn btre-trim []
  (fn [specs value]
    (clojure.string/trim value)))

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
(def btre-int-spec
  {:global {:thousand-separator " "
            :decimal-separator ""
            :output-separator "\t"
            }
   :header [(line-contains? ["TABLE" "Passengers" "Freight" "Foreign" "Inbound" "Outbound"])
            (line-empty?)]
   :input [{:index 0 :name "origin" }
           {:index 1 :name "destination" }
           {:index 2 :name "arrint 2012" }
           {:index 3 :name "depint 2012" }
           {:index 4 :name "totint 2012"}
           {:index 6 :name "arrint 2013"}
           {:index 7 :name "depint 2013"}
           {:index 8 :name "totint 2013"}
           ]
   :columns [{:name "origin" :skip-row (cell-contains? ["Total"])}
             {:name "destination" :repeat-down true}
             {:name "arrint" :transform (convert-to-int)}
             {:name "depint" :transform (convert-to-int)}
             {:name "totint" :transform (convert-to-int)}]
   
   :footer [(line-contains? ["Please"])
            (line-empty?)]
   :outputs [
             [{:name "type" :value "citypair"}
              {:name "year" :value "2012"}
              {:name "iata" :value ""}
              {:name "icao" :value ""}
              {:name "origin"}
              {:name "destination"}
              {:name "arrint 2012"}
              {:name "depint 2012"}
              {:name "totint 2012"}
              ]
             [{:name "type" :value "citypair"}
              {:name "year" :value "2013"}
              {:name "iata" :value ""}
              {:name "icao" :value ""}
              {:name "origin"}
              {:name "destination"}
              {:name "arrint 2013"}
              {:name "depint 2013"}
              {:name "totint 2013"}

              ]
             ]
   })
