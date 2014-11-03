(ns parser.pax.dsl.referential
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output])
  (:use [clojure.data.csv])
  (:use [clojure.java.io])
  (:use [clj-time.core :exclude (second extend) :as t])
  (:use [clj-time.format :as f])
)

;; INPUT FIELDS
;;iata_code^icao_code^faa_code^is_geonames^geoname_id^envelope_id^name^asciiname^latitude^longitude^fclass^fcode^page_rank^date_from^date_until^comment^country_code^cc2^country_name^continent_name^adm1_code^adm1_name_utf^adm1_name_ascii^adm2_code^adm2_name_utf^adm2_name_ascii^adm3_code^adm4_code^population^elevation^gtopo30^timezone^gmt_offset^dst_offset^raw_offset^moddate^city_code_list^city_name_list^city_detail_list^tvl_por_list^state_code^location_type^wiki_link^alt_name_section
;;

;; OUTPUT FIELDS
;;'iata_code', 'icao_code', 'faa_code', 'geoname_id', 'asciiname', 'latitude', 'longitude', 'country_code', 'timezone', 'gmt_offset', 'dst_offset', 'page_rank', 'fclass', 'fcode', 'location_type'
;;

(def other-formatter (f/formatter "yyyy-dd-MM"))

  
(defn convert-date [s]
  (if (or (= s "1998-31-12")
          (= s "2013-31-12"))
    (f/parse other-formatter s) 
    (f/parse (f/formatters :date) s))
  )

(defn ref-date? [^org.joda.time.DateTime d fn-cmp] ;; date is DateTime instance
  (fn [specs value & line]
    (if (empty? value)
      value ;; empty string or nil
      (let [value-date (convert-date value)]
        (if (false? (fn-cmp value-date d))
          false
          value)))))

;; return the value from the cell if the value if after date s, otherwise false
(defn ref-date-after? [^org.joda.time.DateTime d]
  (ref-date? d t/after?))

;; return the value of the cell if the value if before date s, otherwise false
(defn ref-date-before? [^org.joda.time.DateTime d]
  (ref-date? d t/before?))

(defn ref-cell-false? []
  (fn [specs value]
    (false? value)
    ))

;; transform-line 
;; fcode == 'AIRP') | (por.fcode == 'AIRB') | (por.fcode == 'AIRS') | (por.fcode == 'AIRF') | ((por.fclass == 'P') & (por.location_type == 'CA'))] #CA = city airport
(defn ref-airport? []
  (fn [specs cells value]
    (if (or (= "AIRP" value)
            (= "AIRB" value)
            (= "AIRS" value)
            (= "AIRF" value)
            (let [location_type (get-named-cell-value "location_type" cells)
                  fclass (get-named-cell-value "fclass" cells)
                  ]
              (and (= "P" fclass)
                   (= "CA" location_type)) ;; city_airport
              ))
      value
      false)))

;; transform
(defn ref-city? []
  (fn [specs value & line]
    (= "P" value)))

(defn ref-specs
  ([fn-filter]
     (let [n (t/now)
           first-day-current-month (t/first-day-of-the-month n)
           last-day-current-month (t/last-day-of-the-month n)]
       (ref-specs fn-filter first-day-current-month last-day-current-month )))
  
  ([fn-filter ^org.joda.time.DateTime valid-from ^org.joda.time.DateTime valid-to]
     {:global {:token-separator "^"
               :thousand-separator ""
               :decimal-separator ""
               :output-separator "^"
               }
      :skip [(line-contains? ["iata_code"])]
      :tokens [{:index 0 :name "iata_code"}
               {:index 1 :name "icao_code"}
               {:index 2 :name "faa_code" }
               {:index 4 :name "geoname_id"}
               {:index 7 :name "asciiname"}
               {:index 8 :name "latitude"}
               {:index 9 :name "longitude"}
               {:index 10 :name "fclass"}
               {:index 11 :name "fcode"}
               {:index 12 :name "page_rank"}
               {:index 13 :name "date_from"}
               {:index 14 :name "date_to"}
               {:index 16 :name "country_code"}
               {:index 31 :name "timezone"}
               {:index 32 :name "gmt_offset"}
               {:index 33 :name "dst_offset"}
               {:index 41 :name "location_type"}
               ]
      :columns [{:name "fcode" :transform-line fn-filter :skip-line (ref-cell-false?)}
                {:name "date_from" :transform (ref-date-after? valid-from) :skip-line (ref-cell-false?)}
                {:name "date_to" :transform (ref-date-before? valid-to) :skip-line (ref-cell-false?)}
                ]
      :output [{:name "iata_code"}
               {:name "icao_code"}
               {:name "faa_code" }
               {:name "geoname_id"}
               {:name "asciiname"}
               {:name "latitude"}
               {:name "longitude"}
               {:name "country_code"}
               {:name "timezone"}
               {:name "gmt_offset"}
               {:name "dst_offset"}
               {:name "page_rank"}
               {:name "fclass"}
               {:name "fcode"}
               {:name "location_type"}
               ]
      }))

;;pk^env_id^validity_from^validity_to^3char_code^2char_code^num_code^name^name2^alliance_code^alliance_status^type^wiki_link^flt_freq
(defn ref-airline-specs
  ([]
     (let [n (t/now)
           first-day-current-month (t/first-day-of-the-month n)
           last-day-current-month (t/last-day-of-the-month n)]
       (ref-airline-specs first-day-current-month last-day-current-month)))
  ([valid-from valid-to]
     {:global {:token-separator "^"
               :thousand-separator ""
               :decimal-separator ""
               :output-separator "^"
               }
      :skip [(line-contains? ["3char_code"])]
      :tokens [{:index 2 :name "date_from" }
               {:index 3 :name "date_to"}
               {:index 4 :name "icao_code"}
               {:index 5 :name "iata_code"}
               {:index 6 :name "num_code"}
               {:index 7 :name "name"}
               ]
      :columns [{:name "date_from" :transform (ref-date-after? valid-from) :skip-line (ref-cell-false?)}
                {:name "date_to" :transform (ref-date-before? valid-to) :skip-line (ref-cell-false?)}
                ]
      :output [{:name "iata_code"}
               {:name "icao_code"}
               {:name "num_code" }
               {:name "name"}
               ]
      }))

;;
;; country_code^country_name^continent_code^continent_name
;;
(def ref-country-specs
  {:global {:token-separator "^"
            :thousand-separator ""
            :decimal-separator ""
            :output-separator "^"
            }
   :tokens [{:index 0 :name "country_code" }
            {:index 1 :name "country_name"}
            {:index 2 :name "continent_code"}
            {:index 3 :name "continent_name"}
            ]
   :output [{:name "country_code" }
            {:name "country_name"}
            {:name "continent_code"}
            {:name "continent_name"}
            ]})

(def ref-airline-specs (ref-airline-specs))
(def ref-airport-specs (ref-specs (ref-airport?)))
(def ref-city-specs (ref-specs (ref-city?)))



