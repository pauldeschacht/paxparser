(ns parser.pax.dsl.destatis
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))
;;
;; DESTATIS GENERIC FUNCTIONS
;;

(defn destatis-trim []
  (fn [specs value]
    (if (empty? value)
      nil
      (clojure.string/trim value))))

(defn destatis-convert-to-int []
  (fn [ctxt value]
    (do
      (if (not (empty? value))
        (if (= (clojure.string/trim value) "-")
          nil
          (if-let [thousand-separator (:thousand-separator ctxt)]
            (int (Double/parseDouble (clojure.string/replace value thousand-separator "")))
            (int (Double/parseDouble value))))
        nil
        ))))

(defn destatis-skip-empty-pax []
  (fn [specs value]
    (if (nil? value)
      true
      false)))

;; define the list of columns in the Destatis sheet
(def destatis-airport-indexes
  [
   {:index 3  :name "Berlin-Schönefeld"}
   {:index 4  :name "Berlin-Tegel"}
   {:index 5  :name "Bremen"}
   {:index 6  :name "Dortmund"}

   {:index 9  :name "Dresden"}
   {:index 10 :name "Düsseldorf"}
   {:index 11 :name "Erfurt-Weimar"}
   {:index 12 :name "Frankfurt/Main"}
   {:index 13 :name "Friedrichshafen"}
   {:index 14 :name "Hahn"}
   {:index 15 :name "Hamburg"}
   {:index 16 :name "Hannover"}

   {:index 19 :name "Karlsruhe/Baden-Baden"}
   {:index 20 :name "Köln/Bonn"}
   {:index 21 :name "Leipzig/Halle"}
   {:index 22 :name "Lübeck"}
   {:index 23 :name "Memmingen"}
   {:index 24 :name "München"}
   {:index 25 :name "Münster/Osnabrück"}
   {:index 26 :name "Niederrhein"}

   {:index 29 :name "Nürnberg"}
   {:index 30 :name "Paderborn/Lippstadt"}
   {:index 31 :name "Rostock-Laage"}
   {:index 32 :name "Saarbrücken"}
   {:index 33 :name "Stuttgart"}
   {:index 34 :name "Sylt-Westerland"}
   {:index 35 :name "Zweibrücken"}])

(defn destatis-airport-list []
  (map #(:name %) destatis-airport-indexes))
;;
;; DOMESTIC AIRPORTPAIR
;;
(defn destatis-dom-columns []
  (concat [ {:name "paxtype" :value "airportpair"}
            {:name "paxsource" :value "Destatis"}
            {:name "fullname" :transform (get-fullname)}
            {:name "capture-date" :transform (get-capture-date)}
            {:name "valid-from" :transform (get-valid-from)}
            {:name "valid-to" :transform (get-valid-to)}
            {:name "origin-airport-name" :transform (destatis-trim)}
            {:name "metric" :value "pax"}
            {:name "segment" :value false}]

          (map #(hash-map :name %1 :transform (destatis-convert-to-int))
               (destatis-airport-list))))

(defn destatis-dom-outputs []
  (map #(merge-pax-output (generic-pax-output) %)
       (map #(vector
              {:name "destination-airport-name" :value %1}
              {:name "totdom" :source %1 :skip-line (destatis-skip-empty-pax)})
            (destatis-airport-list)
            )))

(def destatis-dom-spec
  {:global {:thousand-separator " "
            :decimal-separator ""
            :output-separator ","
            }
   :skip [(line-contains? ["Gewerblicher" "Passagier" "Verkehr" "Luftverkehr" "Streckenzielflughafen" "von" "Streckenherkunfts-" "flughäfen" "Anzahl" "Deutschland insgesamt" "Hauptverkehrsflughäfen" "Sonstige"])
          (line-empty?)]

   :tokens (concat
            [{:index 0 :name "origin-airport-name" }]
            destatis-airport-indexes)

   :columns (destatis-dom-columns)

   :outputs (destatis-dom-outputs)
   })

;;
;; INTERNATIONAL AIRPORTPAIR
;;
;; define the list of columns in the Destatis sheet
(def destatis-airport-int-indexes
  [
   {:index 2  :name "Berlin-Schönefeld"}
   {:index 3  :name "Berlin-Tegel"}
   {:index 4  :name "Bremen"}
   {:index 5  :name "Dortmund"}
   {:index 6  :name "Dresden"}
   
   {:index 9  :name "Düsseldorf"}
   {:index 10  :name "Erfurt-Weimar"}
   {:index 11 :name "Frankfurt/Main"}
   {:index 12 :name "Friedrichshafen"}
   {:index 13 :name "Hahn"}
   {:index 14 :name "Hamburg"}
   {:index 15 :name "Hannover"}

   {:index 18 :name "Karlsruhe/Baden-Baden"}
   {:index 19 :name "Köln/Bonn"}
   {:index 20 :name "Leipzig/Halle"}
   {:index 21 :name "Lübeck"}
   {:index 22 :name "Memmingen"}
   {:index 23 :name "München"}
   {:index 24 :name "Münster/Osnabrück"}
   
   {:index 27 :name "Niederrhein"}
   {:index 28 :name "Nürnberg"}
   {:index 29 :name "Paderborn/Lippstadt"}
   {:index 30 :name "Rostock-Laage"}
   {:index 31 :name "Saarbrücken"}
   {:index 32 :name "Stuttgart"}
   {:index 33 :name "Sylt-Westerland"}
   {:index 34 :name "Zweibrücken"}])


(defn destatis-airport-int-list []
  (map #(:name %) destatis-airport-int-indexes))

(defn destatis-country-name []
  (fn [specs value]
    (if (= (count value) 3)
      false
      true
      )))

(defn destatis-int-airportpair-columns []
  (concat [ {:name "paxtype" :value "airportpair"}
            {:name "paxsource" :value "Destatis"}
            {:name "fullname" :transform (get-fullname)}
            {:name "capture-date" :transform (get-capture-date)}
            {:name "valid-from" :transform (get-valid-from)}
            {:name "valid-to" :transform (get-valid-to)}
            {:name "origin-airport-iata" :transform (destatis-trim) :skip-line (destatis-country-name)}
            {:name "metric" :value "pax"}
            {:name "segment" :value false}]

          (map #(hash-map :name %1 :transform (destatis-convert-to-int))
               (destatis-airport-int-list))))

(defn destatis-int-outputs []
  (map #(merge-pax-output (generic-pax-output) %)
       (map #(vector
              {:name "destination-airport-name" :value %1}
              {:name "totint" :source %1 :skip-line (destatis-skip-empty-pax)})
            (destatis-airport-int-list)
            )))


(def destatis-int-airportpair-spec
  {:global {:thousand-separator " "
            :decimal-separator ""
            :output-separator ","
            }
   :skip [(line-contains? ["Gewerblicher" "Passagier" "Verkehr" "Luftverkehr" "Streckenzielflughafen" "von" "Streckenherkunfts-" "flughäfen" "Anzahl" "insgesamt" "Hauptverkehrsflughäfen" "Sonstige"])
          (line-empty?)]

   :tokens (concat
            [{:index 0 :name "origin-airport-iata" }]
            destatis-airport-int-indexes)

   :columns (destatis-int-airportpair-columns)

   :outputs (destatis-int-outputs)
   })
;;
;; INTERNATIONAL COUNTRYAIRPORT
;;
(defn destatis-skip-iata-code []
  (fn [specs value]
    (do
      (println value)
      (if (empty? value)
        true
        (if (= (count (clojure.string/trim value)) 3)
          true
          false)))))

(defn destatis-int-countryairport-columns []
  (concat [ {:name "paxtype" :value "airportpair"}
            {:name "paxsource" :value "Destatis"}
            {:name "fullname" :transform (get-fullname)}
            {:name "capture-date" :transform (get-capture-date)}
            {:name "valid-from" :transform (get-valid-from)}
            {:name "valid-to" :transform (get-valid-to)}
            {:name "origin-country-name" :transform (destatis-trim) :skip-line (destatis-skip-iata-code)}
            {:name "metric" :value "pax"}
            {:name "segment" :value false}]

          (map #(hash-map :name %1 :transform (destatis-convert-to-int))
               (destatis-airport-int-list))))

(def destatis-int-countryairport-spec
  {:global {:thousand-separator " "
            :decimal-separator ""
            :output-separator ","
            }
   :skip [(line-contains? ["Gewerblicher" "Passagier" "Verkehr" "Luftverkehr" "Streckenzielflughafen" "von" "Streckenherkunfts-" "flughäfen" "Anzahl" "insgesamt" "Hauptverkehrsflughäfen" "Sonstige"])
          (line-empty?)]

   :tokens (concat
            [{:index 0 :name "origin-country-name" }]
            destatis-airport-int-indexes)

   :columns (destatis-int-countryairport-columns)

   :outputs (destatis-int-outputs)
   })
;;
;;
;;
(defn test-destatis-dom []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis.xlsx"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis_dom.csv"
        sheetname "2.2.1"]
    (convert-pax-file f1 destatis-dom-spec f2 sheetname)
    ))

(defn test-destatis-int []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis.xlsx"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis_int_airportpair.csv"
        sheetname "2.3.2"]
    (convert-pax-file f1 destatis-int-airportpair-spec f2 sheetname)
    ))

(defn test-destatis-int-countryairport []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis.xlsx"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis_int_countrypair.csv"
        sheetname "2.3.2"]
    (convert-pax-file f1 destatis-int-countryairport-spec f2 sheetname)
    ))
