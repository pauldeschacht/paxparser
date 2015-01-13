(ns parser.pax.dsl.kac
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))

(defn remove-empty []
  (fn [specs value & line]
    (if (or (nil? value)
            (empty? value)
            (empty? (clojure.string/trim value)))
      nil
      value)))

(defn- kac-convert-to-int [specs value]
  (if (not (empty? value))
    (try
      (let [thousand-separator (get-in specs [:global :thousand-separator])
            ]
        (if (empty? thousand-separator)
          (int (Double/parseDouble value))
          (int (Double/parseDouble (clojure.string/replace value thousand-separator "")))))
      (catch Exception e (println (str "Error convert to int: " value  (.getMessage e))))
      )
    
    nil
    ))

(defn take-int-from-field [field]
  (fn [specs value line]
    (if-let [cell (find-cell-by-name field (:columns line))]
      (let [value* (:value cell)]
        (kac-convert-to-int specs value*)
        )
      (throw (Exception. (str "KAC take-int-from-field: " field " not found in row: " (str line)))))))

(defn take-str-from-field [field]
  (fn [specs value line]
    (if-let [cell (find-cell-by-name field (:columns line))]
      (let [value* (:value cell)]
        (if (or (nil? value*)
                (empty? value*)
                (empty? (clojure.string/trim value*)))
          nil
          value*))
      (throw (Exception. (str "KAC take-str-from-field: " field " not found in row: " (str line)))))))

(defn kac-total-empty? []
  (fn [specs value]
    (nil? value)))

;;
;; input format KAC (domestic)
;; origin - destination - airline - arrdom - depdom - totdom 
;; needs to be transformed to
;; origin - destination - airline -        -        - arrdom
;; destination - source - airline -        -        - depdom
;;
;; the KAC input format uses the arrival and departure fields to indicate the direction
;; the Traffic Analysis airportpairairline uses the origin and destination to indicate the direction
;;
(defn kac-spec [origin-iata origin-name destination-iata destination-name total]
  {:global {:thousand-separator ","
            :decimal-separator nil
            :output-separator "^"
            :token-separator "^"
            }

   :skip [(line-contains? ["total"])]

   :tokens [{:index 0 :name "type"}
            {:index 1 :name "year"}
            {:index 2 :name "month"}
            {:index 3 :name "kac-origin-airport-iata"}
            {:index 4 :name "kac-origin-airport-name"}
            {:index 5 :name "kac-destination-airport-iata"}
            {:index 6 :name "kac-destination-airport-name"}
            {:index 7 :name "airline-icao"}
            {:index 8 :name "kac-arrdom"}
            {:index 9 :name "kac-depdom"}
            {:index 10 :name "kac-totdom"}
            {:index 11 :name "kac-arrint"}
            {:index 12 :name "kac-depint"}
            {:index 13 :name "kac-totint"}
            ]

   :columns [{:name "paxtype" :value "airportpairairline"}
             {:name "paxsource" :value "KAC"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-from)}

             {:name "origin-airport-iata" :transform (take-str-from-field origin-iata) :repeat-down true}
             {:name "origin-airport-name" :transform (take-str-from-field origin-name) :repeat-down true}
             {:name "destination-airport-iata" :transform (take-str-from-field destination-iata) :repeat-down true}
             {:name "destination-airport-name" :transform (take-str-from-field destination-name) :repeat-down true}
             
             {:name "metric" :value "pax"}
             {:name "segment" :value nil}
             
;;             {:name "arrdom" :transform (convert-to-int)}
;;             {:name "depdom" :transform (convert-to-int)}
;;             {:name "totdom" :transform (convert-to-int)}
;;             {:name "arrint" :transform (convert-to-int)}
;;             {:name "depint" :transform (convert-to-int)}
;;             {:name "totint" :transform (convert-to-int)}

             {:name "tottot" :transform (take-int-from-field total) :skip-line (kac-total-empty?)}
             ]


   :output (generic-pax-output)
   }
  )

;;
;; KAC domestic kac-origin-airport-name = jeju
;;              kac-destination-airport-name = gimpo
;;              kac-arrival = X
;;              kac-departure = Y
;; 
;; jeju - gimpo = X
;; gimpo - jeju = Y
;;
(def kac-spec-arr-dom (kac-spec "kac-origin-airport-iata" "kac-origin-airport-name" "kac-destination-airport-iata" "kac-destination-airport-name" "kac-arrdom"))
(def kac-spec-dep-dom (kac-spec "kac-destination-airport-iata" "kac-destination-airport-name" "kac-origin-airport-iata" "kac-origin-airport-name" "kac-depdom"))

;; hard to find out which is source and destination
;; KAC international: kac-origin-airport-name = SONGSAN
;;                    kac-destination-airport-iata = GMP
;;                    kac-arrival = X
;;                    kac-departure = Y
;;
;; Probably the reference for the kac-arrival is the Korean airport (GMP in example)
;; GMP - SONGSAN = Y
;; SONGSAN - GMP = X
(def kac-spec-arr-int (kac-spec "kac-origin-airport-iata" "kac-origin-airport-name" "kac-destination-airport-iata" "kac-destination-airport-name" "kac-depint"))
(def kac-spec-dep-int (kac-spec "kac-destination-airport-iata" "kac-destination-airport-name" "kac-origin-airport-iata" "kac-origin-airport-name" "kac-arrint"))

