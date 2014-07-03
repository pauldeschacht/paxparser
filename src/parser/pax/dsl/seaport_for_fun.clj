(ns parser.pax.dsl.seaport-for-fun
  (:use [parser.core :as parser])
  (:use [clj-time.core :exclude (second extend) :as t])
  (:use [clj-time.format :as f])
  )
;;
;; SEAPORT
;;
(defn seaport-clean []
  (fn [specs value & line]
    (if (= value "Nul")
      ""
      value)))
(defn seaport-trim []
  (fn [specs value & line]
    (if (nil? value)
      value
      (clojure.string/trim value))))

(defn seaport-dollar [fun]
  (let [function (fun)]
    (fn [specs value & line]
      (if (nil? value)
        value
        (let [value* (clojure.string/replace value "$" "")]
          (function specs value*))))))

(defn seaport-date []
  (fn [specs value & line]
     (if (nil? value)
       value
       ;;(#July 01, 2014)
       (f/unparse (f/formatters :date)
                  (f/parse (f/formatter "MMM dd, yyyy") value)))))

(def seaport-spec
  {:global {:thousand-separator ","
            :decimal-separator "."
            :output-separator ";"
            :token-separator "\t"}
   
   :skip [(line-contains? ["Average" "Total"])
          ]
   :tokens [{:index 0 :name "region"}
           {:index 1 :name "segment" :split (split-into-cells ["origin-city-name" "destination-city-name"] "-")}
           {:index 2 :name "date"}
           {:index 3 :name "pax"}
           {:index 4 :name "tkt"}
           {:index 5 :name "avg"}
           {:index 6 :name "fee"}
           {:index 7 :name "fee_percentage"}
           {:index 8 :name "total"}
           ]
   :columns [{:name "region" :repeat-down true :transform (seaport-clean)}
             {:name "origin-city-name" :transform (seaport-trim)}
             {:name "destination-city-name" :transform (seaport-trim)}
             {:name "date" :transform (seaport-date)}
             {:name "pax" :transform (convert-to-int)}
             {:name "tkt" :transform (seaport-dollar convert-to-double)}
             {:name "avg" :transform (seaport-dollar convert-to-double)}
             {:name "fee" :transform (seaport-dollar convert-to-double)}
             {:name "fee_percentage"}
             {:name "total" :transform (seaport-dollar convert-to-double)}
             ]
   
   :output [{:name "region"}
            {:name "origin-city-name"}
            {:name "destination-city-name"}
            {:name "date"}
            {:name "pax"}
            {:name "tkt"}
            {:name "avg"}
            {:name "fee"}
            {:name "fee_percentage"}
            {:name "total"}]
   })

(defn test-seaport []
  (let [f1 "/home/pdeschacht/pdf/Seaport/01Jul14_daily_segment_report.info.csv"
        f2 "/home/pdeschacht/pdf/Seaport/01Jul14_daily_segment_report.csv"
        ]
    (parser/convert-file f1 seaport-spec f2 nil)))
