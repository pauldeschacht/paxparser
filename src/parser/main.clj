(ns parser.main
  (:use [clojure.tools.cli :refer [parse-opts]])
  (:require [parser.pax.core :as parser]
            [parser.pax.dsl.aci :as aci]
            [parser.pax.dsl.acsa :as acsa]
            [parser.pax.dsl.albatross :as albatross]
            [parser.pax.dsl.anac :as anac]
            [parser.pax.dsl.anac-2014 :as anac-2014]
            [parser.pax.dsl.btre :as btre]
            [parser.pax.dsl.bts-db1b :as bts]
            [parser.pax.dsl.clac :as clac]
            [parser.pax.dsl.destatis :as destatis]
            [parser.pax.dsl.dhmi :as dhmi]
            [parser.pax.dsl.icao-dataplus :as icao-dataplus]
            [parser.pax.dsl.indonesia :as indonesia]
            [parser.pax.dsl.kac :as kac]
            [parser.pax.dsl.mx-sase :as mx]
            [parser.pax.dsl.referential :as ref]
            )
  (:gen-class))

(def spec-details
  [{:code "ACI" :input "raw_aci_int.csv" :specname "aci-spec" :output "pax_aci_int.csv"}
   {:code "ACI" :input "raw_aci_ww.csv" :specname "aci-spec" :output "pax_aci_int.csv"}
   {:code "Albatross" :input "raw_albatross.csv" :specname "albatross-2008-spec" :output "pax_albatross_2008.csv"}
   {:code "Albatross" :input "raw_albatross.csv" :specname "albatross-2009-spec" :output "pax_albatross_2009.csv"}
   {:code "Albatross" :input "raw_albatross.csv" :specname "albatross-2010-spec" :output "pax_albatross_2010.csv"}
   {:code "Albatross" :input "raw_albatross.csv" :specname "albatross-2011-spec" :output "pax_albatross_2011.csv"}
   {:code "Albatross" :input "raw_albatross.csv" :specname "albatross-2012-spec" :output "pax_albatross_2012.csv"}
   {:code "Albatross" :input "raw_albatross.csv" :specname "albatross-2013-spec" :output "pax_albatross_2013.csv"}
   {:code "ANAC" :input "raw_anac.xlsx" :specname "anac-2014-spec" :output "pax_anac.csv" :sheet "(?i)passa"}
   {:code "BTRE" :input "raw_btre_dom.xlsx" :specname "btre-dom-spec" :output "pax_btre_dom.csv" :sheet "Passengers"}
   {:code "BTRE" :input "raw_btre_int.xlsx" :specname "btre-int-countrypairairline-spec" :output "pax_btre_int.csv" :sheet "Table_3"}
   {:code "BTRE" :input "raw_btre_int.xlsx" :specname "btre-int-airportpair-spec" :output "pax_btre_int.csv" :sheet "Table_5"}
   {:code "DESTATIS" :input "raw_destatis.xlsx" :specname "destatis-dom-spec" :output "pax_destatis_dom.csv" :sheet "2.2.1"}
   {:code "DESTATIS" :input "raw_destatis.xlsx" :specname "destatis-int-dep-spec" :output "pax_destatis_int.csv" :sheet "2.3.1"}
   {:code "DESTATIS" :input "raw_destatis.xlsx" :specname "destatis-int-arr-spec" :output "pax_destatis_int.csv" :sheet "2.3.2"}
   {:code "DHMI" :input "raw_dhmi.xlsx" :specname "dhmi-spec" :output "pax_dhmi.csv" :sheet "(?i)yolcu"}
   {:code "INDONESIA" :input "raw_indonesia.csv" :specname "indonesia-spec" :output "pax_indonesia.csv"}
   {:code "MX" :input "raw_mx.xlsx" :specname "mx-spec" :output "pax_mx_dom.csv" :sheet "(?i)REG[ ]*NAC"}
   {:code "MX" :input "raw_mx.xlsx" :specname "mx-spec" :output "pax_mx_int.csv" :sheet "(?i)REG[ ]*INT"}
   {:code "MX" :input "raw_mx_airports.xlsx" :specname "mx-airport-spec" :output "pax_mx_airports.csv" }
   {:code "KAC" :input "raw_kac_dom.csv" :specname "kac-spec" :output "pax_kac_dom.csv"}
   {:code "KAC" :input "raw_kac_int.csv" :specname "kac-spec" :output "pax_kac_int.csv"}
   {:code "POR" :input "ori_por_public.csv" :specname "ref-airport-spec" :output "airlines.csv"}
   {:code "POR" :input "ori_por_public.csv" :specname "ref-city-spec" :output "cities.csv"}
   {:code "POR" :input "ori_airlines.csv" :specname "ref-airline-spec" :output "airlines.csv"}
   {:code "POR" :input "ori_countries.csv" :specname "ref-country-spec" :output "countries.csv"}
   ])


(defn- get-spec [specname]
  (case (clojure.string/lower-case specname)
    "aci-spec" aci/aci-spec
    "acsa" acsa/acsa-spec
    "albatross-2007-spec" albatross/albatross-spec-old-output-2007
    "albatross-2008-spec" albatross/albatross-spec-old-output-2008
    "albatross-2009-spec" albatross/albatross-spec-old-output-2009
    "albatross-2010-spec" albatross/albatross-spec-old-output-2010
    "albatross-2011-spec" albatross/albatross-spec-old-output-2011
    "albatross-2012-spec" albatross/albatross-spec-old-output-2012
    "albatross-2013-spec" albatross/albatross-spec-old-output-2013
    "anac-2014-spec" anac-2014/anac-2014-spec-convert-to-old
    "btre-dom-spec" btre/btre-dom-spec
    "btre-int-countrypairairline-spec" btre/btre-int-countrypairairline-spec
    "btre-int-airportpair-spec" btre/btre-int-airportpair-spec
    "bts-db1b-spec" bts/bts-db1b-spec
    "bts-t100-spec" bts/bts-t100-spec
    "clac-spec" clac/clac-spec
    "destatis-dom-spec" destatis/destatis-dom-spec
    "destatis-int-dep-spec" destatis/destatis-countryairport-dep-spec
    "destatis-int-arr-spec" destatis/destatis-countryairport-arr-spec
;;    "destatis-int-countryairport-spec" destatis/destatis-int-countryairport-spec
    "dhmi-spec" dhmi/dhmi-spec
    "icao-spec" icao-dataplus/icao-airport-spec
    "indonesia-spec" indonesia/indonesia-spec
    "mx-spec" mx/mx-citypair-spec
    "mx-airport-spec" [(mx/mx-airport-spec "totdom" ["YEAR" "INTERNATIONAL"]) (mx/mx-airport-spec "totdom" ["YEAR" "DOMESTIC"]) ]
    "kac-spec" [kac/kac-spec-arr-dom kac/kac-spec-arr-int kac/kac-spec-dep-dom kac/kac-spec-dep-int]
    "ref-airport-spec" ref/ref-airport-specs
    "ref-city-spec" ref/ref-city-specs
    "ref-airline-spec" ref/ref-airline-specs
    "ref-country-spec" ref/ref-country-specs
    :else (throw (Exception. (str "Unknown spec name "  specname))))
    )

(defn find-file-in-folder [folder filename]
  (let [f (clojure.java.io/file folder)
        files (file-seq f)
        ]
    (->> files
         (filter #(= filename (.getName %)))
         (first)
         )))


(defn process [spec in out sheetname]
  (if (seq? spec)
    (do
      (doall (map #(parser/convert-pax-file in %1 out sheetname) spec)))
    (do
      (println (str "convert-pax-file " in))
      (parser/convert-pax-file in spec out sheetname)))
    )

(defn process-file [in-folder spec-detail]
  (let [spec (get-spec (:specname spec-detail))]
    (if-let [file (find-file-in-folder (str in-folder "/" (:code spec-detail)) (:input spec-detail))]
      (let [folder (.getParent file)
            in (str folder "/" (:input spec-detail))
            out (str folder "/" (:output spec-detail))
            ]
        (process spec in out (:sheet spec-detail)))
      (println (str "File " (:input spec-detail) " not found in folder " in-folder))
      )
    )
  )

(defn process-all [in-folder]
  (println (str "process ALL " in-folder))
  (doall (map #(process-file in-folder %) spec-details))
  )

(def cli-options
  [
   ["-c" "--spec SPEC" "specification name"]
   ["-i" "--input INPUT" "input file name"]
   ["-o" "--output OUTPUT" "output file name"]
   ["-s" "--sheet SHEET" "name of the sheet"]
   ])

(defn -main[& args]
  (let [options (parse-opts args cli-options)
        in (:input (:options options))
        specname (:spec (:options options))
        out (:output (:options options))
        sheetname (:sheet (:options options))
        ]
    (if (= "all" (clojure.string/lower-case specname))
      (process-all in)
      (process (get-spec specname) in out sheetname))))
