(ns parser.pax.dsl.albatross
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))

(defn albatross-convert-to-int []
  (fn [specs value & line]
    
    (if (or (nil? value)
            (empty? (clojure.string/trim value)))
      nil
      (try
        (int (Double/parseDouble value))
        (catch Exception e (println (.getMessage e)))))))

;;
;; input format
;; 00 Internal<br>ID^
;; 01 IATA
;; 02 ICAO
;; 03 Airport
;; 04 Country
;; 05 Region
;; 06 Latest av.Total^
;; 07 Latest av. Dom^
;; 08 Latest av. Int^
;; 09 Latest av. Other^
;; 10 2013 Total
;; 11 2013 Dom
;; 12 2013 Int
;; 13 2013 Other
;; [14-17] 2012 Total^2012 Dom^2012 Int^2012 Other^
;; [18-21] 2011 Total^2011 Dom^2011 Int^2011 Other^
;; [22-25] 2010 Total^2010 Dom^2010 Int^2010 Other^
;; [26-29] 2009 Total^2009 Dom^2009 Int^2009 Other^
;; [30-33] 2008 Total^2008 Dom^2008 Int^2008 Other^
;; [34-27] 2007 Total^2007 Dom^2007 Int^2007 Other^
;; 2006 Total^2006 Dom^2006 Int^2006 Other^
;; 2005 Total^2005 Dom^2005 Int^2005 Other^
;; 2004 Total^2004 Dom^2004 Int^2004 Other^
;; 2003 Total^2003 Dom^2003 Int^2003 Other^
;; 2002 Total^2002 Dom^2002 Int^2002 Other
;;
(def albatross-spec
  {:global {
            :thousand-separator nil
            :decimal-separator nil
            :output-separator ","
            }
   :skip [(line-contains? ["Total" "Other"])]
   
   :tokens [{:index 1 :name "origin-airport-iata"}
            {:index 2 :name "origin-airport-icao"}
            {:index 3 :name "origin-airport-name"}
            {:index 4 :name "origin-country-name"}
            {:index 5 :name "region"}
            
            {:index 10 :name "2013_tottot"}
            {:index 11 :name "2013_totdom"}
            {:index 12 :name "2013_totint"}
            
            {:index 14 :name "2012_tottot"}
            {:index 15 :name "2012_totdom"}
            {:index 16 :name "2012_totint"}
            
            {:index 18 :name "2011_tottot"}
            {:index 19 :name "2011_totdom"}
            {:index 20 :name "2011_totint"}
            
            {:index 22 :name "2010_tottot"}
            {:index 23 :name "2010_totdom"}
            {:index 24 :name "2010_totint"}
            
            {:index 26 :name "2009_tottot"}
            {:index 27 :name "2009_totdom"}
            {:index 28 :name "2009_totint"}
            
            {:index 30 :name "2008_tottot"}
            {:index 31 :name "2008_totdom"}
            {:index 32 :name "2008_totint"}
           ]
   :columns [
             {:name "paxtype"   :value "airport"}
             {:name "paxsource" :value "Albatross"}
             {:name "fullname"  :transform (get-fullname)}
             {:name "capture-date"   :transform (get-capture-date)}

             {:name "metric" :value "pax"}
             {:name "segment" :value nil}

             {:name "2013_tottot" :transform (albatross-convert-to-int)}
             {:name "2013_totdom" :transform (albatross-convert-to-int)}
             {:name "2013_totint" :transform (albatross-convert-to-int)}
             
             {:name "2012_tottot" :transform (albatross-convert-to-int)}
             {:name "2012_totdom" :transform (albatross-convert-to-int)}
             {:name "2012_totint" :transform (albatross-convert-to-int)}
             
             {:name "2011_tottot" :transform (albatross-convert-to-int)}
             {:name "2011_totdom" :transform (albatross-convert-to-int)}
             {:name "2011_totint" :transform (albatross-convert-to-int)}
             
             {:name "2010_tottot" :transform (albatross-convert-to-int)}
             {:name "2010_totdom" :transform (albatross-convert-to-int)}
             {:name "2010_totint" :transform (albatross-convert-to-int)}
             
             {:name "2009_tottot" :transform (albatross-convert-to-int)}
             {:name "2009_totdom" :transform (albatross-convert-to-int)}
             {:name "2009_totint" :transform (albatross-convert-to-int)}
             
             {:name "2008_tottot" :transform (albatross-convert-to-int)}
             {:name "2008_totdom" :transform (albatross-convert-to-int)} 
             {:name "2008_totint" :transform (albatross-convert-to-int)}
             ]
   :outputs [(merge-pax-output (generic-pax-output)
                               [{:name "valid-from" :value "2013-01-01"}
                                {:name "valid-to"   :value "2013-12-31"}
                                {:name "tottot" :source "2013_tottot"}
                                {:name "totdom" :source "2013_totdom"}
                                {:name "totint" :source "2013_totint"}])
             (merge-pax-output (generic-pax-output)
                               [{:name "valid-from" :value "2012-01-01"}
                                {:name "valid-to"   :value "2012-12-31"}
                                {:name "tottot" :source "2012_tottot"}
                                {:name "totdom" :source "2012_totdom"}
                                {:name "totint" :source "2012_totint"}])
             (merge-pax-output (generic-pax-output)
                               [{:name "valid-from" :value "2011-01-01"}
                                {:name "valid-to"   :value "2011-12-31"}
                                {:name "tottot" :source "2011_tottot"}
                                {:name "totdom" :source "2011_totdom"}
                                {:name "totint" :source "2011_totint"}])
             (merge-pax-output (generic-pax-output)
                               [{:name "valid-from" :value "2010-01-01"}
                                {:name "valid-to"   :value "2010-12-31"}
                                {:name "tottot" :source "2010_tottot"}
                                {:name "totdom" :source "2010_totdom"}
                                {:name "totint" :source "2010_totint"}])
             (merge-pax-output (generic-pax-output)
                               [{:name "valid-from" :value "2009-01-01"}
                                {:name "valid-to"   :value "2009-12-31"}
                                {:name "tottot" :source "2009_tottot"}
                                {:name "totdom" :source "2009_totdom"}
                                {:name "totint" :source "2009_totint"}])
             (merge-pax-output (generic-pax-output)
                               [{:name "valid-from" :value "2008-01-01"}
                                {:name "valid-to"   :value "2008-12-31"}
                                {:name "tottot" :source "2008_tottot"}
                                {:name "totdom" :source "2008_totdom"}
                                {:name "totint" :source "2008_totint"}])
             (merge-pax-output (generic-pax-output)
                               [{:name "valid-from" :value "2007-01-01"}
                                {:name "valid-to"   :value "2007-12-31"}
                                {:name "tottot" :source "2007_tottot"}
                                {:name "totdom" :source "2007_totdom"}
                                {:name "totint" :source "2007_totint"}])
             ]
})

(defn albatross-spec-old-output [year]
  {:global {:token-separator "^"
            :thousand-separator nil
            :decimal-separator nil
            :output-separator ";"
            }
   
   :tokens [{:index 0 :name "ID"}
            {:index 1 :name "origin-airport-iata"}
            {:index 2 :name "origin-airport-icao"}
            {:index 3 :name "origin-airport-name"}
            {:index 4 :name "origin-country-name"}
            {:index 5 :name "region"}

            {:index 6 :name "Latest av. Total"}
            {:index 7 :name "Latest av. Dom"}
            {:index 8 :name "Latest av. Int"}
            {:index 9 :name "Latest av. Other"}
            
            {:index 10 :name "2013_tottot"}
            {:index 11 :name "2013_totdom"}
            {:index 12 :name "2013_totint"}
            {:index 13 :name "2013_other"}
            
            {:index 14 :name "2012_tottot"}
            {:index 15 :name "2012_totdom"}
            {:index 16 :name "2012_totint"}
            {:index 17 :name "2012_other"}
            
            {:index 18 :name "2011_tottot"}
            {:index 19 :name "2011_totdom"}
            {:index 20 :name "2011_totint"}
            {:index 21 :name "2011_other"}
            
            {:index 22 :name "2010_tottot"}
            {:index 23 :name "2010_totdom"}
            {:index 24 :name "2010_totint"}
            {:index 25 :name "2010_other"}
            
            {:index 26 :name "2009_tottot"}
            {:index 27 :name "2009_totdom"}
            {:index 28 :name "2009_totint"}
            {:index 29 :name "2009_other"}
            
            {:index 30 :name "2008_tottot"}
            {:index 31 :name "2008_totdom"}
            {:index 32 :name "2008_totint"}
            {:index 33 :name "2008_other"}
            
            {:index 34 :name "2007_tottot"}
            {:index 35 :name "2007_totdom"}
            {:index 36 :name "2007_totint"}
            {:index 37 :name "2007_other"}
           ]
   :columns []

   :output [{:name "ID"}
            {:name "origin-airport-iata"}
            {:name "origin-airport-icao"}
            {:name "origin-airport-name"}
            {:name "origin-country-name"}
            {:name "region"}
            
            {:name "Latest av. Total"}
            {:name "Latest av. Dom"}
            {:name "Latest av. Int"}
            {:name "Latest av. Other"}
            
            {:name (str year "_tottot")}
            {:name (str year "_totdom")}
            {:name (str year "_totint")}
            {:name (str year "_other")}]
   })

(def albatross-spec-old-output-2013 (albatross-spec-old-output 2013))
(def albatross-spec-old-output-2012 (albatross-spec-old-output 2012))
(def albatross-spec-old-output-2011 (albatross-spec-old-output 2011))
(def albatross-spec-old-output-2010 (albatross-spec-old-output 2010))
(def albatross-spec-old-output-2009 (albatross-spec-old-output 2009))
(def albatross-spec-old-output-2008 (albatross-spec-old-output 2008))
(def albatross-spec-old-output-2007 (albatross-spec-old-output 2007))


(defn test-albatross []
  (let [f1 "/home/pdeschacht/2014/11/Albatross/2013/Albatross.csv"
        f2 "/home/pdeschacht/2014/11/Albatross/2013/01_importAirport.csv"
        sheetname nil
        ]
    (convert-pax-file f1 albatross-spec-old-output f2 sheetname)))
