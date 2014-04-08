(ns parser.pax.dsl.acsa
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output])
  (:use [clj-time.core :exclude (second extend) :as t])
  )
;;
;; ACSA (manual entries)
;;
(defn safe-to-int [value]
  (if (empty? value)
    nil
    (try
      (int (Double/parseDouble value))
      (catch Exception e
        nil)
      )))

(defn btre-line-does-not-contain? [words]
  (fn [line]
    (if (some #(substring? % line) words)
      false
      true)
    ))

(defn btre-to-int []
  (fn [specs value]
    (let [v (safe-to-int value)]
      (if (= v 0)
        nil
        v))))

(defn btre-valid-from []
  (fn [specs value]
    (let [tokens (clojure.string/split value (re-pattern "-"))
          year (safe-to-int (first tokens))
          month (safe-to-int (second tokens))
          ]
      (t/first-day-of-the-month year month))))

(defn btre-valid-to []
  (fn [specs value]
    (let [tokens (clojure.string/split value (re-pattern "-"))
          year (read-string (first tokens))
          month (read-string (second tokens))]
      (t/last-day-of-the-month year month))
    ))

(def acsa-spec
  { :global {:output-separator ","
             :decimal-separator "."
             }

   :skip [(btre-line-does-not-contain? ["airport"])]

   :tokens [{:index 0 :name "paxtype"}
            {:index 1 :name "origin-airport-iata"}
            {:index 2 :name "origin-airport-icao"}
            {:index 3 :name "origin-airport-name"}
            {:index 4 :name "year"}
            {:index 5 :name "month"}
            {:index 6 :name "arrdom"}
            {:index 7 :name "arrint"}
            {:index 8 :name "arrtot"}
            {:index 9 :name "depdom"}
            {:index 10 :name "depint"}
            {:index 11 :name "deptot"}
            {:index 12 :name "tottot"}
            ]
   :columns [{:name "paxsource" :value "ACSA"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :merge (merge-from ["year" "month"] "-") :transform (btre-valid-from)}
             {:name "valid-to" :merge (merge-from ["year" "month"] "-") :transform (btre-valid-to)}
             {:name "metric" :value "pax"}
             {:name "segment" :value nil}
             {:name "arrdom" :transform (btre-to-int)}
             {:name "arrint" :transform (btre-to-int)}
             {:name "arrtot" :transform (btre-to-int)}
             {:name "depdom" :transform (btre-to-int)}
             {:name "depint" :transform (btre-to-int)}
             {:name "deptot" :transform (btre-to-int)}
             {:name "tottot" :transform (btre-to-int)}
             ]
   :output (generic-pax-output)
   }
  )

(defn test-acsa []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ACSA/2014/01/manual entries.xlsx"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ACSA/2014/01/acsa.csv"
        ]
    (convert-pax-file f1 acsa-spec f2 "Sheet1"))
  )

