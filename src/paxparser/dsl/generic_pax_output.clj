(ns paxparser.dsl.generic-pax-output)

(def pax-output
  [{:name "paxtype"}
   {:name "paxsource"}
   {:name "fullname"}
   {:name "capture-date"}
   {:name "valid-from"}
   {:name "valid-to"}

   {:name "origin-airport-id"}
   {:name "origin-airport-name"}
   {:name "origin-airport-iata"}
   {:name "origin-airport-icao"}
   {:name "origin-airport-faa"}

   {:name "origin-city-id"}
   {:name "origin-city-name"}
   {:name "origin-city-iata"}
   {:name "origin-city-icao"}
   {:name "origin-city-faa"}

   {:name "origin-country-id"}
   {:name "origin-country-name"}
   {:name "origin-country-iata"}
   {:name "origin-country-icao"}
   {:name "origin-country-faa"}

   {:name "destination-airport-id"}
   {:name "destination-airport-name"}
   {:name "destination-airport-iata"}
   {:name "destination-airport-icao"}
   {:name "destination-airport-faa"}

   {:name "destination-city-id"}
   {:name "destination-city-name"}
   {:name "destination-city-iata"}
   {:name "destination-city-icao"}
   {:name "destination-city-faa"}

   {:name "destination-country-id"}
   {:name "destination-country-name"}
   {:name "destination-country-iata"}
   {:name "destination-country-icao"}
   {:name "destination-country-faa"}

   {:name "airline-id"}
   {:name "airline-name"}
   {:name "airline-iata"}
   {:name "airline-icao"}
   {:name "airline-faa"}

   {:name "metric"}
   {:name "segment"}

   {:name "depdom"}
   {:name "depint"}
   {:name "deptot"}

   {:name "arrdom"}
   {:name "arrint"}
   {:name "arrtot"}

   {:name "totdom"}
   {:name "totint"}
   {:name "tottot"}
   ])
