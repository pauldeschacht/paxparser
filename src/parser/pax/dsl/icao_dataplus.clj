(ns parser.pax.dsl.icao-dataplus
  (:use [parser.core :as core])
  (:use [parser.pax.core :as pax])
  (:use [parser.pax.dsl.generic-pax-output])
  (:use [clj-time.core :exclude (second extend) :as t]))

(def icao-month-lst ["jan" "feb" "mar" "apr" "may" "jun" "jul" "aug" "sep" "oct" "nov" "dec"])
;;
;; trim 
;;
(defn- icao-trim []
  (fn [specs value line]
    (if (nil? value)
      value
      (clojure.string/trim value))))

(defn- icao-clean []
  (fn [specs value line]
    (if (or (nil? value)
            (empty? value))
      nil
      (->
       value
       (clojure.string/replace "(" "")
       (clojure.string/replace ")" "")
       (clojure.string/trim))

      )))
(defn- icao-split-name-code [names separator]
  (fn [value]
    (let [values (clojure.string/split value (re-pattern separator))
          cells [ {:name (first names) :value value}
                  {:name (second names) :value (last values)}]
          ]
      cells

      ))
  )
;;
;; transform name of the month to the index (example "may" --> 5)
;; 
(defn- index-of [coll item]
  (count (take-while (partial not= (clojure.string/lower-case item)) coll)))
;;
;;
;;
;;
;; extract the cell with a given name
;;
(defn- icao-get-value-with-name [line name]
  (if-let [cell (first (filter #(= (:name %1) name) (:columns line)))]
    (:value cell)
    nil
    ))

(defn- icao-get-valid [start-or-end]
  (fn [specs value line]
    (let [year-str (icao-get-value-with-name line "year")
          year (if (= (class year-str) java.lang.String)
                 (int (Double/parseDouble year-str))
                 year)
          month-str (icao-get-value-with-name line "month")
          month (inc (index-of icao-month-lst month-str))
          period (pax/valid-period-month year month)
          ]
      (-> period
          (start-or-end)
          (pax/date-to-str))
      )))

(defn- icao-get-valid-from []
  (icao-get-valid t/start))
  
(defn- icao-get-valid-to []
  (icao-get-valid t/end))

(defn- icao-get-field [category relation]
  (fn [specs value line]
    (let [c (icao-get-value-with-name line "category")]
      (if (= (clojure.string/lower-case c) category)
        (case relation
          "arrival" (icao-get-value-with-name line "arrival")
          "departure" (icao-get-value-with-name line "departure")
          "total" (icao-get-value-with-name line "total")
          nil)
        nil
        )
      )
    )
  )
;; parse the excel with city pair data
;;
(def icao-airport-spec
  {:global {:thousand-separator "."
            :decimal-separator ""
            :output-separator "^"}
   
   :skip [(line-empty?)
          (line-contains? ["State" "Total" "Non-scheduled" "Not specified"])
          ]
   
   :tokens [{:index 3 :name "origin-airport"
             :split (icao-split-name-code ["origin-airport-name" "origin-airport-iata"] "\\(")
             }
            {:index 5 :name "category"}
            {:index 6 :name "subcategory"}
            {:index 7 :name "year"}
            {:index 8 :name "month"}
            {:index 9 :name "departure"}
            {:index 10 :name "arrival"}
            {:index 11 :name "total"}

            ]
   
   :columns [{:name "paxtype" :value "airport"}
             {:name "paxsource" :value "ICAO"}

             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (icao-get-valid-from)}
             {:name "valid-to" :transform (icao-get-valid-to)}

             {:name "metric" :value "pax"}
             {:name "segment" :value nil}

             {:name "origin-airport-name" :transform (icao-clean)}
             {:name "origin-airport-iata" :transform (icao-clean)}
             
             {:name "arrdom" :transform (icao-get-field "domestic" "arrival")}
             {:name "depdom" :transform (icao-get-field "domestic" "departure")}
             {:name "totdom" :transform (icao-get-field "domestic" "total")}
             
             {:name "arrint" :transform (icao-get-field "international" "arrival")}
             {:name "depint" :transform (icao-get-field "international" "departure")}
             {:name "totint" :transform (icao-get-field "international" "total")}
             ]
   
   :output (generic-pax-output)
   }
  )
