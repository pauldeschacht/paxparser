(ns parser.pax.dsl.anac-2014
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))
;;
;; ANAC (format starting from 2014)
;
(defn anac-get-airport-icao []
  (fn [specs value]
    (if (substring? "Aeroporto" value)
      (apply str (take 4 (clojure.string/trim value)))
      nil)))

(defn anac-get-airport-name []
  (fn [specs value]
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
  (fn [specs value]
    (if (substring? "TRANSPORTE NÃO REGULAR" value)
      "not-regular"
      (if (substring? "TRANSPORTE REGULAR" value)
        "regular"
        nil
        ))))

(defn anac-is-domestic []
  (fn [specs value]
    (if (substring? "DOMÉSTICO" value)
      "dom"
      (if (substring? "INTERNACIONAL" value)
        "int"
        (if (or (substring? "TRANSPORTE" value)
                (= "NACIONAL" value)
                (= "REGIONAL" value))
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
   
   :columns [{:name "info" :skip-line (cell-contains? ["TRANSPORTE"])}
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

(defn test-anac-2014-core []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2014/01/Jan.xls"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2014/01/test.csv"
        sheetname "Passageiros"]
    (parser.pax.core/convert-pax-file f1 anac-2-spec f2 sheetname)))


(defn test-anac-2014 []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2014/01/Jan.xls"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2014/01/test.csv"
        output-filename f2
        sheetname "Passageiros"
        file-info (extract-file-information f1)
        specs (merge anac-2-spec {:global (merge (:global anac-2-spec) {:file-info file-info})})
        specs* (add-defaults-to-specs specs)
        params {:filename f1 :sheetname sheetname}
        lines (read-lines params)
        ]

        (->> lines
         (wrap-text-lines)
         (skip-lines (:skip specs*))
         (remove-skip-lines)
         (stop-after (:stop specs*))
         (tokenize-lines (get-in specs* [:global :token-separator]) (get-in specs* [:global :quote]))
         (lines-to-cells (:tokens specs*))
         (merge-lines-with-column-specs (:columns specs*))
         (add-new-column-specs-lines (:columns specs*))
         (merge-lines specs*)
         (transform-lines specs*)
         (repeat-down-lines specs*)
         (transform-full-lines specs*)
         (skip-transformed-lines specs*)
         (output-lines specs*)
         (clean-outputs-lines)
         (outputs-to-csv-lines (get-in specs* [:global :output-separator]))
         (csv-outputs-to-file output-filename))))
