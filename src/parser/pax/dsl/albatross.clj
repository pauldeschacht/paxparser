(ns parser.pax.dsl.albatross
  (:use [parser.pax.core])
  (:use [parser.core])
  (:use [parser.pax.dsl.generic-pax-output]))

(def albatross-spec
  {:global {
            :thousand-separator nil
            :decimal-separator nil
            :output-separator "\t"
            }
   :header [(line-contains? ["Total" "Other"])
            ]
   :tokens [{:index 1 :name "iata"}
           {:index 2 :name "icao"}
           {:index 3 :name "airportname"}
           {:index 4 :name "country"}
           {:index 5 :name "region"}
           
           {:index 10 :name "2013_tot"}
           {:index 11 :name "2013_dom"}
           {:index 12 :name "2013_int"}

           {:index 14 :name "2012_tot"}
           {:index 15 :name "2012_dom"}
           {:index 16 :name "2012_int"}

           {:index 18 :name "2011_tot"}
           {:index 19 :name "2011_dom"}
           {:index 20 :name "2011_int"}

           {:index 22 :name "2010_tot"}
           {:index 23 :name "2010_dom"}
           {:index 24 :name "2010_int"}

           {:index 26 :name "2009_tot"}
           {:index 27 :name "2009_dom"}
           {:index 28 :name "2009_int"}

           {:index 30 :name "2008_tot"}
           {:index 31 :name "2008_dom"}
           {:index 32 :name "2008_int"}
           ]
   :columns [
             {:index 10 :name "2013_tot" :transform (convert-to-int)}
             {:index 11 :name "2013_dom" :transform (convert-to-int)}
             {:index 12 :name "2013_int" :transform (convert-to-int)}
             
             {:index 14 :name "2012_tot" :transform (convert-to-int)}
             {:index 15 :name "2012_dom" :transform (convert-to-int)}
             {:index 16 :name "2012_int" :transform (convert-to-int)}
             
             {:index 18 :name "2011_tot" :transform (convert-to-int)}
             {:index 19 :name "2011_dom" :transform (convert-to-int)}
             {:index 20 :name "2011_int" :transform (convert-to-int)}
             
             {:index 22 :name "2010_tot" :transform (convert-to-int)}
             {:index 23 :name "2010_dom" :transform (convert-to-int)}
             {:index 24 :name "2010_int" :transform (convert-to-int)}
             
             {:index 26 :name "2009_tot" :transform (convert-to-int)}
             {:index 27 :name "2009_dom" :transform (convert-to-int)}
             {:index 28 :name "2009_int" :transform (convert-to-int)}
             
             {:index 30 :name "2008_tot" :transform (convert-to-int)}
             {:index 31 :name "2008_dom" :transform (convert-to-int)} 
             {:index 32 :name "2008_int" :transform (convert-to-int)}
             ]
   :outputs [
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2013"}
              {:name "2013_tot"}
              {:name "2013_dom"}
              {:name "2013_int"}             
            ]
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2012"}
              {:name "2012_tot"}
              {:name "2012_dom"}
              {:name "2012_int"}             
            ]
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2011"}
              {:name "2011_tot"}
              {:name "2011_dom"}
              {:name "2011_int"}             
            ]
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2010"}
              {:name "2010_tot"}
              {:name "2010_dom"}
              {:name "2010_int"}             
            ]
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2009"}
              {:name "2009_tot"}
              {:name "2009_dom"}
              {:name "2009_int"}             
              ]
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2008"}
              {:name "2008_tot"}
              {:name "2008_dom"}
              {:name "2008_int"}             
              ]
             ]
   })
