(ns parser.pax.dsl.anac-2014
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output])
  (:use [clojure.data.csv])
  (:use [clojure.java.io])
  )
;;
;; ANAC (format starting from 2014)
;
(defn anac-get-airport-icao []
  (fn [specs value & line]
    (if (substring? "Aeroporto" value)
      (apply str (take 4 (clojure.string/trim value)))
      nil)))

(defn anac-get-airport-name []
  (fn [specs value & line]
    (if (substring? "Aeroporto" value)
      (apply str (drop 7 (clojure.string/trim value)))
      nil)))
;;
;; due to parser.core/repeat-down function, the type of :value of a cell must be a STRING !!!
;;
;; if the :value of a cell is nil, the repeat-down function can overwrite the nil with a value that repeated
;; if the :value of a cell is not nil, the repeat-down function will *never* overwrite
;; Therefore, the values "not-regular" and "skip" play a role
;;
(defn anac-is-regular []
  (fn [specs value & line]
    (if (or (substring? "TRANSPORTE NÃO REGULAR" value)
            (substring? "Transporte Não Regular" value))
      "not-regular"
      (if (or (substring? "TRANSPORTE REGULAR" value)
              (substring? "Transporte Regular" value))
        
        "regular"
        nil
        ))))

(defn anac-is-domestic []
  (fn [specs value & line]
    (if (or (substring? "DOMÉSTICO" value)
            (substring? "Doméstico" value))
      "dom"
      (if (or (substring? "INTERNACIONAL" value)
              (substring? "Internacional" value))
        "int"
        (if (or (substring? "TRANSPORTE" value)
                (substring? "Transporte" value)
                (substring? "Cabotagem" value)
                (substring? "Aeroporto" value)
                (= "NACIONAL" value)
                (= "Nacional" value)
                (= "REGIONAL" value)
                (= "Regional" value)
                )
          "skip"
          nil
          )))))

(defn anac-domestic []
  (fn [specs cells value]
    (if (and (not (nil? value))
             (not (nil? (get-named-cell-value "origin-airport-icao" cells)))
             (= "regular" (get-named-cell-value "regular" cells))
             (= "dom" (get-named-cell-value "domestic" cells)))
      value
      nil)))

(defn anac-international []
  (fn [specs cells value]
    (if (and (not (nil? value))
             (not (nil? (get-named-cell-value "origin-airport-icao" cells)))
             (= "regular" (get-named-cell-value "regular" cells))
             (= "int" (get-named-cell-value "domestic" cells)))
      value
      nil)))

(defn anac-cell-empty? []
  (fn [specs value]
    (nil? value)))

(def anac-2014-spec
  {:global {:thousand-separator ""
            :decimal-separator ""
            :output-separator ","
            }

   :skip [(line-contains? ["Discriminação" "INFRAERO" "Movimento" "CABOTAGEM" "SUPERINTENDÊNCIA" "REGIONAL"])]
   
   :tokens [{:index 1 :name "info" :split (copy-into-cells ["info" "origin-airport-icao" "origin-airport-name" "regular" "domestic" ])}
            {:index 2 :name "arrival" :split (copy-into-cells ["arrdom" "arrint"])}
            {:index 4 :name "departures" :split (copy-into-cells ["depdom" "depint"])}
            ]
   
   :columns [{:name "info" :skip-line (cell-contains? ["TRANSPORTE" "Transporte"])}
             {:name "paxtype" :value "airport"}
             {:name "paxsource" :value "ANAC_2"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-to)}
             {:name "metric" :value "pax"}
             {:name "segment" :value nil}
             {:name "origin-airport-icao" :repeat-down true :transform (anac-get-airport-icao) :skip-line (anac-cell-empty?)}
             {:name "origin-airport-name" :repeat-down true :transform (anac-get-airport-name)}
             {:name "regular" :repeat-down true :transform (anac-is-regular) :skip-line (cell-contains? ["skip" "not-regular"])}
             {:name "domestic" :repeat-down true :transform (anac-is-domestic) :skip-line (cell-contains? ["skip"])}
             {:name "arrdom" :transform (convert-to-int) :transform-line (anac-domestic)}
             {:name "arrint" :transform (convert-to-int) :transform-line (anac-international)}
             {:name "depdom" :transform (convert-to-int) :transform-line (anac-domestic)}
             {:name "depint" :transform (convert-to-int) :transform-line (anac-international)}]
   :output (generic-pax-output)
   })
;;
;; 
;; CONVERT TO OLD FORMAT (only keep total domestic and total international)
;;
;;
(defn anac-totdom []
  (fn [specs cells value]
    (if (and (not (nil? value))
             (not (nil? (get-named-cell-value "origin-airport-icao" cells)))
             (= "regular" (get-named-cell-value "regular" cells))
             (= "dom" (get-named-cell-value "domestic" cells)))
      value
      nil)))

(defn anac-totint []
  (fn [specs cells value]
    (if (and (not (nil? value))
             (not (nil? (get-named-cell-value "origin-airport-icao" cells)))
             (= "regular" (get-named-cell-value "regular" cells))
             (= "int" (get-named-cell-value "domestic" cells)))
      value
      nil))  )

(defn anac-tottot []
  (fn [specs cells value]
    (if (and (not (nil? value))
             (not (nil? (get-named-cell-value "origin-airport-icao" cells)))
             (= "TRANSPORTE REGULAR" (get-named-cell-value "info" cells)))
      value
      nil))  )


(defn anac-skip [words]
  (fn [specs value]
    (if (some #(= value %1) words)
      true
      false)))

(def anac-2014-spec-convert-to-old
  {:global {:thousand-separator ","
            :decimal-separator "."
            :output-separator "\t"
            }

   :skip [(line-contains? ["Discriminação" "INFRAERO" "Movimento" "CABOTAGEM" "SUPERINTENDÊNCIA" "REGIONAL"])]
   
   :tokens [{:index 1 :name "info" :split (copy-into-cells ["info" "origin-airport-icao" "origin-airport-name" "regular" "domestic" ])}
            {:index 6 :name "total" :split (copy-into-cells ["totdom" "totint" "tottot"])}
            ]
   
   :columns [{:name "info" :skip-line (anac-skip ["NACIONAL"])}
             {:name "paxtype" :value "airport"}
             {:name "paxsource" :value "ANAC_2"}
             {:name "origin-airport-icao" :repeat-down true :transform (anac-get-airport-icao) :skip-line (anac-cell-empty?)}
             {:name "origin-airport-name" :repeat-down true :transform (anac-get-airport-name)}
             {:name "regular" :repeat-down true :transform (anac-is-regular) :skip-line (cell-contains? ["skip" "not-regular"])}
             {:name "domestic" :repeat-down true :transform (anac-is-domestic) :skip-line (cell-contains? ["skip"])}
             {:name "totdom" :transform (convert-to-int) :transform-line (anac-totdom)}
             {:name "totint" :transform (convert-to-int) :transform-line (anac-totint)}
             {:name "tottot" :transform (convert-to-int) :transform-line (anac-tottot)}]
   :output [{:name "type" :value "airport"}
            {:name "origin-airport-icao"}
            {:name "origin-airport-iata" :value ""}
            {:name "origin-airport-name"}
            {:name "totdom"}
            {:name "totint"}
            {:name "tottot"}
            ]
   })

(defn empty-string? [k s] (or (nil? s) (= 0 (count s))))
(defn valid-string? [k s] (not (empty-string? k s)))
(defn dissoc-with-pred [f m]
  (into {} (filter (fn [[k v]] (f k v)) m)))

(defn group-airports [airports]
  (->> airports 
       (map #(zipmap [:type :icao :iata :name :totdom :totint :tottot] %) )
       (map #(dissoc-with-pred valid-string? %))
       (group-by :icao)
       (map #(reduce merge {} (second %)))
       (map #(merge {:type nil
                     :icao nil
                     :iata nil
                     :name nil
                     :totdom nil
                     :totint nil
                     :tottot nil
                     } %))
       (mapv #(mapv % [:type :icao :iata :name :totdom :totint :tottot]))))

(defn group-airports-in-out [in out]
  (let [airports (with-open [in-file (clojure.java.io/reader in)]
                   (doall
                    (clojure.data.csv/read-csv in-file :separator \tab)))
        grouped (group-airports airports)
        ]
    (with-open [file (clojure.java.io/writer out)]
      (clojure.data.csv/write-csv file grouped :separator \tab))))

(def files [
            "01/anac.xls"
            "02/anac.xls"
            "03/anac.xls"
            "04/anac.xls"
            "05/anac.xls"
            "06/anac.xls"
            ])

(defn process-file [in]

  (let [sheet "Passageiros"
        path "/home/pdeschacht/dev/airtraffic2/pax/download/2014/08/ANAC/2014/06/"
        infile (str path in)
        temp (str infile ".txt")
        dir (first (clojure.string/split in #"/"))
        outfile (str path "01_importAirport.csv")
        _ (println infile)
        _ (println temp)
        _ (println outfile)
        _ (convert-pax-file infile anac-2014-spec-convert-to-old temp sheet)
        _ (println outfile)
        _ (group-airports-in-out temp outfile)]
    ))

(defn process-files [files]
  (doall (map process-file files))
  )
