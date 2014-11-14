(ns parser.pax.dsl.indonesia
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))

(def indonesia-spec
  {:global {:thousand-separator "."
            :decimal-separator nil
            :output-separator "^"
            :token-separator "^"
            }

   :tokens [{:index 0 :name "paxtype"}
            {:index 1 :name "year"}
            {:index 2 :name "month"}
            {:index 3 :name "origin-airport-iata"}
            {:index 4 :name "tottot"}
            ]

   :columns [{:name "paxsource" :value "INDONESIA"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-from)}

             {:name "metric" :value "pax"}
             {:name "segment" :value nil}
             
             {:name "tottot" :transform (convert-to-int)}
             ]


   :output (generic-pax-output)
   }
  )


  
