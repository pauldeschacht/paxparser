(ns parser.pax.dsl.dsl-test
  (:use clojure.test
        [parser.core :as core]
        [parser.pax.core :as parser]
        [parser.pax.dsl.acsa :as acsa]
        [parser.pax.dsl.anac :as anac]
        [parser.pax.dsl.anac-2014 :as anac-2014]
        [parser.pax.dsl.btre :as btre]
        [parser.pax.dsl.clac :as clac]
        [parser.pax.dsl.destatis :as destatis]
        [parser.pax.dsl.dhmi :as dhmi]
        [parser.pax.dsl.mx-sase :as mx]
        ))

(defn test-file [in specs out sheet]
  (let [specs* (core/add-defaults-to-specs (parser/add-pax-info-to-specs specs in))
        lines (core/read-file in sheet)
        tokens (core/process-lines specs* lines)
        outputs (core/lines-to-outputs specs* tokens)
        all-processed (flatten outputs)
        ref-lines (core/read-file out nil)
        ]
    (= all-processed ref-lines)))

(deftest test-pax-parsing
  (testing "ACSA"
    (is (= true (test-file         
               "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ACSA/2014/01/acsa.xlsx"
               acsa/acsa-spec
               "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ACSA/2014/01/acsa.csv"
               "Sheet1"))))

  (testing "ANAC pre-2014 format"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2013/12/ANAC.xlsx"
                 anac/anac-spec
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2013/12/ANAC.csv"
                 "Sheet1"))))
  
  (testing "ANAC 2014 format"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2014/03/Mar.xls"
                 anac-2014/anac-2014-spec
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/ANAC/2014/03/Mar.csv"
                 "Passageiros"))))

  (testing "BTRE domestic country pair"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTRE_Australia/2013/10/BTRE_Domestic_airlines_Nov_2013.xls"
                 btre/btre-dom-spec
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTRE_Australia/2013/10/BTRE_Domestic_CityPair.csv"
                 "Passengers"))))

  (testing "BTRE international country pair"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTRE_Australia/2013/10/BTRE_International_airline_activity_1310_Tables.xls"
                 btre/btre-int-countrypairairline-spec
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTRE_Australia/2013/10/BTRE_Internaional_CountryPairAirline.csv"
                 "Table_3"))))

  (testing "BTRE airportpair"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTRE_Australia/2013/10/BTRE_International_airline_activity_1310_Tables.xls"
                 btre/btre-int-airportpair-spec
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/BTRE_Australia/2013/10/BTRE_International_AirportportPair.csv"
                 "Table_5"))))

  (testing "CLAC South America"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/CLAC/2013/04/CLAC_2013.xls"
                 clac/clac-spec
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/CLAC/2013/04/CLAC_2013.csv"
                 "Registro Datos"))))

  (testing "Destatis Domestic"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis.xlsx"
                 destatis/destatis-dom-spec
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis_dom.csv"
                 "2.2.1"))))

  (testing "Destatis International AirportPair"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis.xlsx"
                 destatis/destatis-int-airportpair-spec
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis_int_airportpair.csv"
                 "2.3.2"))))

  (testing "Destatis International Country Airport"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis.xlsx"
                 destatis/destatis-int-countryairport-spec
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/02/Destatis/2014/01/Destatis_int_countrypair.csv"
                 "2.3.2"))))

  (testing "DHMI (Turkey)"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/05/DHMI/2014/01/airport.xlsx"
                 dhmi/dhmi-spec
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/05/DHMI/2014/01/airport.csv"
                 "YOLCU"))))

  (testing "Mexico Domestic CityPair"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/04/MX/2014/04/SASE_ABRIL.xlsx"
                 (mx/mx-citypair-spec 2014 "totdom")
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/04/MX/2014/04/SASE_ABRIL_dom.csv"
                 "REG NAC"))))
  
  (testing "Mexico International CityPair"
    (is (= true (test-file
               "/home/pdeschacht/dev/paxparser/test/public-data/2014/04/MX/2014/04/SASE_ABRIL.xlsx"
               (mx/mx-citypair-spec 2014 "totint")
               "/home/pdeschacht/dev/paxparser/test/public-data/2014/04/MX/2014/04/SASE_ABRIL_int.csv"
               "REG INT"))))

  (testing "Mexico Domestic Airport"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/04/MX/2014/04/AEROPUERTOS.xls"
                 (mx/mx-airport-spec "totdom" ["YEAR" "INTERNATIONAL"])
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/04/MX/2014/04/AEROPUERTOS_dom.csv"
                 "Details"))))

  (testing "Mexico International Airport"
    (is (= true (test-file
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/04/MX/2014/04/AEROPUERTOS.xls"
                 (mx/mx-airport-spec "totint" ["YEAR" "DOMESTIC"])
                 "/home/pdeschacht/dev/paxparser/test/public-data/2014/04/MX/2014/04/AEROPUERTOS_int.csv"
                 "Details"))))
  )


