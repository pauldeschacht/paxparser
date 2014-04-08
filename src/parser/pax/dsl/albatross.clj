(ns parser.pax.dsl.albatross
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))

(defn albatross-convert-to-int []
  (fn [specs value]
    (if (empty? (clojure.string/trim value))
      nil
      (try
        (Integer/parseInt value)
        (catch Exception e (println (.getMessage e)))))))

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


             ]
   })


(defn test-albatross []
  (let [f1 "/home/pdeschacht/dev/paxparser/test/private-data/2014/02/Albatross/2014/02/Albatross.xlsx"
        f2 "/home/pdeschacht/dev/paxparser/test/private-data/2014/02/Albatross/2014/02/test.csv"
        sheetname "Sheet1"
        file-info (extract-file-information f1)
        specs (merge albatross-spec {:global (merge (:global albatross-spec) {:file-info file-info})})
        specs* (add-defaults-to-specs specs)
        params {:filename f1 :sheetname sheetname :max 200}
        lines (read-lines params)
        ]

    (->> lines
         (wrap-text-lines)
         (skip-lines (:skip specs*))
         (remove-skip-lines)
         (stop-after (:stop specs*))
         (tokenize-lines (re-pattern (get-in specs* [:global :token-separator])))
         (lines-to-cells (:tokens specs*))
         (merge-lines-with-column-specs (:columns specs*))
         (add-new-column-specs-lines (:columns specs*))
         (transform-lines specs*)
         (repeat-down-lines specs*)
         (output-lines specs*)
         (clean-outputs-lines)
         (outputs-to-csv-lines (get-in specs* [:global :output-separator]))
         (csv-outputs-to-file f2)
         )
    ))
