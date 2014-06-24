(ns parser.pax.dsl.dgca
(:use [parser.core])
(:use [parser.pax.core])
(:use [parser.pax.dsl.generic-pax-output]))

(defn dgca-month? []
  (fn [specs value]
    (some #(substring? % value) ["JAN" "FEB" "MAR" "APR" "MAY" "JUN" "JUL" "AUG" "SEP" "OCT" "NOV" "DEC"]))
  )

(defn dgca-month []
  (fn [specs value & line]
    (-> value
        (clojure.string/trim)
        (clojure.string/replace "(R)" "")
        (clojure.string/replace "(P)" ""))))

(def dgca-india-spec
  {:global {:thousand-separator ","
            :decimal-separator ""
            :output-separator "\t"}

   :skip [(not dgca-month?)]

   :tokens [ {:index 0 :name "month"}
            {:index 4 :name "pax"}
            ]

   :columns [ {:name "month" :transform (dgca-month)}]

   :outputs [{:name "type" :value "airline"}
             {:name "year" :value "2013"}
             {:name "iata" :value ""}
             {:name "icao" :value ""}
             ]
   })
