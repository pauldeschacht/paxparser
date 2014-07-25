(ns parser.pax.dsl.dhmi
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))

;;
;; DHMI
;;
(defn dhmi-trim []
  (fn [specs value & line]
    (if (empty? value)
      nil
      (-> value
           (clojure.string/replace "\t" "")
           (clojure.string/replace "(*)" "")
           (clojure.string/trim))
      )))

(defn dhmi-cell-empty? []
  (fn [specs value]
    (nil? value)))

(def dhmi-spec
  {:global {:thousand-separator ""
            :output-separator ","}

   :skip [(line-contains? ["YILI" "TÜM UÇAK TRAFİĞİ" "YILI OCAK AYI" "Havalimanları" "İç Hat" "Dış Hat" "Toplam" "Kesin Olmayan" "Hava Alanları" "Havaalanları" "Gelen-Giden" "Passenger Traffic" "Yolcu Trafiği"])]
   :stop [(line-contains? ["DHMİ TOPLAMI"])]
   :tokens [{:index 0 :name "origin-airport-name"}
            {:index 4 :name "totdom"}
            {:index 5 :name "totint"}
            {:index 6 :name "tottot"}
           ]
   :columns [{:name "paxtype" :value "airport"}
             {:name "paxsource" :value "DHMI"}
             {:name "fullname" :transform (get-fullname)}
             {:name "capture-date" :transform (get-capture-date)}
             {:name "valid-from" :transform (get-valid-from)}
             {:name "valid-to" :transform (get-valid-to)}
             {:name "origin-airport-name" :transform (dhmi-trim) :skip-line (dhmi-cell-empty?)}
             {:name "metric" :value "pax"}
             {:name "segment" :value nil}
             {:name "totdom" :transform (convert-to-int)}
             {:name "totint" :transform (convert-to-int)}
             {:name "tottot" :transform (convert-to-int)}
             ]
   
   :output (generic-pax-output)
   })

(defn- get-sheetname [year month]
  (case year
    2009 (case month
           7 "YOLCU"
           "Sayfa2")
    "YOLCU"
    )
  )

(defn dhmi [in out sheet]
  (convert-pax-file in dhmi-spec out sheet))

(defn dhmi-all []
  (let [folder "/home/pdeschacht/dev/paxparser/test/public-data/2014/05/DHMI"]
    (for [year (range 2010 2015)
          month (range 1 13)]
      (let [input (str folder "/"
                       year "/"
                       (format "%02d" month) "/"
                       "airport.xlsx")
            output (str folder "/"
                        year "/"
                        (format "%02d" month) "/"
                        "dhmi.csv")
            sheet (get-sheetname year month)
            ]
        (dhmi input output sheet)))))

