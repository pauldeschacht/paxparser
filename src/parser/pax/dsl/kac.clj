(ns parser.pax.dsl.kac
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))


(def kac-spec
  {:global {:thousand-separator ","
            :decimal-separator nil
            :output-separator "^"
            :token-separator "^"
            }

   :skip [(line-contains? ["total"])]

   :tokens [{:index 0 :name "type"}
            {:index 1 :name "year"}
            {:index 2 :name "month"}
            {:index 3 :name "origin-airport-iata"}
            {:index 4 :name "origin-airport-name"}
            {:index 5 :name "destination-airport-iata"}
            {:index 6 :name "destination-airport-name"}
            {:index 7 :name "airline-iata"}
            {:index 8 :name "arrdom"}
            {:index 9 :name "depdom"}
            {:index 10 :name "totdom"}
            {:index 11 :name "arrint"}
            {:index 12 :name "depint"}
            {:index 13 :name "totint"}
            ]

   :columns [{:name "paxtype" :value "airportpairairline"}
             {:name "paxsource" :value "KAC"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-from)}

             {:name "origin-airport-iata" :repeat true}
             {:name "origin-airport-name" :repeat true}
             {:name "destination-airport-iata" :repeat true}
             {:name "destination-airport-name" :repeat true}
             
             {:name "metric" :value "pax"}
             {:name "segment" :value nil}
             
             {:name "arrdom" :transform (convert-to-int)}
             {:name "depdom" :transform (convert-to-int)}
             {:name "totdom" :transform (convert-to-int)}
             {:name "depdom" :transform (convert-to-int)}
             {:name "depint" :transform (convert-to-int)}
             {:name "totint" :transform (convert-to-int)}
             ]


   :output (generic-pax-output)
   }
  )
