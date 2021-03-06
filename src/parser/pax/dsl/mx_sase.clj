(ns parser.pax.dsl.mx-sase
  (:use [parser.core :as core])
  (:use [parser.pax.core :as pax])
  (:use [parser.pax.dsl.generic-pax-output])
  (:use [clj-time.core :exclude (second extend) :as t]))

;;
;; MX SASE XLSX 
;;
(def mx-month-lst ["jan" "feb" "mar" "apr" "may" "jun" "jul" "aug" "sep" "oct" "nov" "dec"])
;;
;; trim 
;;
(defn- mx-trim []
  (fn [specs value lines]
    (if (nil? value)
      value
      (clojure.string/trim value))))
;;
;; transform name of the month to the index (example "may" --> 5)
;; 
(defn- index-of [coll item]
  (count (take-while (partial not= item) coll)))
;;
;; extract the cell with a given name
;;
(defn- mx-get-value-with-name [line name]
  (if-let [cell (first (filter #(= (:name %1) name) (:columns line)))]
    (:value cell)
    nil
    ))
;;
;; interpret the zero as VOID
;;
(defn- mx-skip-if-zero []
  (fn [specs value]
    (if (or (nil? value)
            (= 0 value))
      true
      false))
  )

(defn- mx-skip-if-empty []
  (fn [specs value]
    (if (or (nil? value)
            (= 0 (count value)))
      true
      false)))
;;
;; the year is encoded in the data (and not in the filename as other files)
;;
(defn mx-sase-valid [start-or-end]
  (fn [specs value line]
    (let [;;year (mx-get-value-with-name line "valid-year")
          ;; year* (if (= (class year) java.lang.String)
          ;;         (int (Double/parseDouble year))
          ;;         year
          ;;         )
          valid (get-in specs [:global :file-info :valid])
          year (t/year (t/start valid))
          ]
      (->> (mx-get-value-with-name line "month")
           (index-of mx-month-lst)
           (inc)
           (pax/valid-period-month year)
           (start-or-end)
           (pax/date-to-str)
           ))))

(defn mx-sase-valid-from []
  (mx-sase-valid t/start))

(defn mx-sase-valid-to []
  (mx-sase-valid t/end))
;;
;; parse the excel with city pair data
;;
(def mx-citypair-spec
  {:global {:thousand-separator ""
            :decimal-separator ""
            :output-separator "^"}
   
   :skip [(line-empty?)
          (line-contains? ["ESTADISTICA" "SERVICIO" "CIUDADES" "ORIGEN" "DESTINO" "TOTAL" "T O T A L" "VUELOS"])
          ]
   
   :tokens [{:index 0 :name "origin-city-name"}
            {:index 1 :name "destination-city-name"}
            {:index 15 :name "jan"}
            {:index 16 :name "feb"}
            {:index 17 :name "mar"}
            {:index 18 :name "apr"}
            {:index 19 :name "may"}
            {:index 20 :name "jun"}
            {:index 21 :name "jul"}
            {:index 22 :name "aug"}
            {:index 23 :name "sep"}
            {:index 24 :name "oct"}
            {:index 25 :name "nov"}
            {:index 26 :name "dec"}
            ]
   
   :transpose {:header-column "month"
               :value-column "tottot"
               :tokens mx-month-lst
               }
   
   :columns [{:name "paxtype" :value "citypair"}
             {:name "paxsource" :value "MX"}

;;             {:name "valid-year" :value year}  ;; for mx-sase-valid-from/-to

             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (mx-sase-valid-from)}
             {:name "valid-to" :transform (mx-sase-valid-to)}

             {:name "metric" :value "pax"}
             {:name "segment" :value nil}

             {:name "origin-city-name" :skip-line (mx-skip-if-empty)}
             {:name "destination-city-name" :skip-line (mx-skip-if-empty)}
             {:name "month"}
             {:name "tottot" :transform (core/convert-to-int) :skip-line (mx-skip-if-zero)}
             ]
   
   :output (generic-pax-output)
   }
  )
;;
;; parse the excel that contain the airport data
;; (done in 2 passes, one for domestic and one for international)
;;
(defn mx-airport-spec [motive skip-lines]
  {:global {:token-separator "^"
            :thousand-separator ""
            :decimal-separator ""
            :output-separator "^"}
   
   :skip [(line-empty?)
          (line-contains? skip-lines)
          (line-contains? ["FLIGHTS" "CARGO"])
          ]

   :tokens [
            {:index 2 :name "valid-year"}
            {:index 3 :name "group"}
            {:index 4 :name "origin-airport-name"}
            {:index 5 :name "jan"}
            {:index 6 :name "feb"}
            {:index 7 :name "mar"}
            {:index 8 :name "apr"}
            {:index 9 :name "may"}
            {:index 10 :name "jun"}
            {:index 11 :name "jul"}
            {:index 12 :name "aug"}
            {:index 13 :name "sep"}
            {:index 14 :name "oct"}
            {:index 15 :name "nov"}
            {:index 16 :name "dec"}]
   
   :transpose {:header-column "month"
               :value-column motive
               :tokens mx-month-lst
               }

   :columns [{:name "paxtype" :value "airport"}
             {:name "paxsource" :value "MX"}

             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (mx-sase-valid-from)}
             {:name "valid-to" :transform (mx-sase-valid-to)}

             {:name "metric" :value "pax"}
             {:name "segment" :value nil}

             {:name "origin-airport-name"}
             {:name motive :transform (convert-to-int) :skip-line (mx-skip-if-zero)}
             ]
   
   :output (generic-pax-output)
   }
  )
