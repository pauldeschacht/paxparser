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
             {:name "origin-airport-name" :transform (dhmi-trim)}
             {:name "metric" :value "pax"}
             {:name "segment" :value nil}
             {:name "totdom" :transform (convert-to-int)}
             {:name "totint" :transform (convert-to-int)}
             {:name "tottot" :transform (convert-to-int)}
             ]
   
   :output (generic-pax-output)
   })

(defn test-dhmi []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/public-data/2014/05/DHMI/2014/01/airport.xlsx"
        f2 "/home/pdeschacht/dev/paxparser/test/public-data/2014/05/DHMI/2014/01/airport.csv"
        sheetname "TÜM UÇAK"
        ]
    (convert-pax-file f1 dhmi-spec f2 sheetname)))

(defn dhmi [f1 f2 sheetname]
  (convert-pax-file f1 dhmi-spec f2 sheetname))

(defn- get-sheetname [year month]
  (case year
    2009 (case month
           7 "YOLCU"
           "Sayfa2")
    "YOLCU"
    )
  )

(defn dhmi-all []
  (let [folder "/home/pdeschacht/dev/paxparser/test/public-data/2014/05/DHMI"]
    (for [year (range 2010 2015)
          month (range 1 13)]
      (let [input (str folder "/"
                       year "/"
                       (format "%02d" month) "/"
                       "airport.xlsx")
            output (str folder "/"
                        "dhmi.csv")
            sheet (get-sheetname year month)
            ]
        (dhmi input output sheet)))))

