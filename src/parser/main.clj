(ns parser.main
  (:use [clojure.tools.cli :refer [parse-opts]])
  (:use [parser.pax.core :as parser]
        [parser.pax.dsl.aci :as aci]
        [parser.pax.dsl.acsa :as acsa]
        [parser.pax.dsl.albatross :as albatross]
        [parser.pax.dsl.anac :as anac]
        [parser.pax.dsl.anac-2014 :as anac-2014]
        [parser.pax.dsl.btre :as btre]
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

(defn- substring*? [s sub]
  (cond
   (empty? s) false
   (empty? sub) false
   :else (not= (.indexOf s sub) -1)))

(defn- contains-all? [s subs]
  (every? true? (map #(substring*? s %1) subs)))

(defn- get-spec-for-source [in sheet hint]
  (let [s (clojure.string/lower-case (str in " " sheet " " hint))]
    (cond
     (contains-all? s ["aci" "international"]) aci/aci-spec
     (contains-all? s ["acsa"]) acsa/acsa-spec
     (contains-all? s ["albatross" "2013!"]) albatross/albatross-spec-old-output-2013
     (contains-all? s ["albatross" "2012!"]) albatross/albatross-spec-old-output-2012
     (contains-all? s ["albatross" "2011!"]) albatross/albatross-spec-old-output-2011
     (contains-all? s ["albatross" "2010!"]) albatross/albatross-spec-old-output-2010
     (contains-all? s ["albatross" "2009!"]) albatross/albatross-spec-old-output-2009
     (contains-all? s ["albatross" "2008!"]) albatross/albatross-spec-old-output-2008
     (contains-all? s ["albatross" "2007!"]) albatross/albatross-spec-old-output-2007
     (contains-all? s ["albatross"]) albatross/albatross-spec
     ;;     (contains-all? s ["anac_brasil_2"]) anac/anac-spec
     ;;     (contains-all? s ["anac_brasil_3"]) anac-2014/anac-2014-spec
     ;;     (contains-all? s ["anac"]) anac-2014/anac-2014-spec
     (contains-all? s ["anac"]) anac-2014/anac-2014-spec-convert-to-old
     (contains-all? s ["btre" "domestic"]) btre/btre-dom-spec
     (contains-all? s ["btre" "international" "Table_3"]) btre/btre-int-countrypairairline-spec
     (contains-all? s ["btre" "international" "Table_5"]) btre/btre-int-airportpair-spec
     (contains-all? s ["clac"]) clac/clac-spec
     (contains-all? s ["destatis" "2.2.1"]) destatis/destatis-dom-spec
     (contains-all? s ["destatis" "2.3.1"]) destatis/destatis-int-airportpair-spec
     (contains-all? s ["destatis" "2.2.2"]) destatis/destatis-int-countryairport-spec
     (contains-all? s ["dhmi"]) dhmi/dhmi-spec
     (contains-all? s ["icao"]) icao-dataplus/icao-airport-spec
     (contains-all? s ["indonesia"]) indonesia/indonesia-spec
     (contains-all? s ["mx" "reg int"]) mx/mx-citypair-spec
     (contains-all? s ["mx" "reg nac"]) mx/mx-citypair-spec
     (contains-all? s ["mx" "regnal"]) mx/mx-citypair-spec
     (contains-all? s ["mx" "regint"]) mx/mx-citypair-spec
     (contains-all? s ["mx" "regnac"]) mx/mx-citypair-spec
     (contains-all? s ["mx" "details"]) [(mx/mx-airport-spec "totdom" ["YEAR" "INTERNATIONAL"]) (mx/mx-airport-spec "totdom" ["YEAR" "DOMESTIC"]) ]
     (contains-all? s ["kac"]) kac/kac-spec
     (contains-all? s ["ori_por_public" "airport"]) ref/ref-airport-specs
     (contains-all? s ["ori_por_public" "city"]) ref/ref-city-specs
     (contains-all? in ["ori_airlines"]) ref/ref-airline-specs
     (contains-all? in ["ori_countries"]) ref/ref-country-specs
     :else (throw (Exception. "Cannot determine the specs for the input (forget the name of the Excel sheet?) ")))
    ))

(defn process [in out sheet hint]
  (let [spec (get-spec-for-source in sheet hint)]
    (if (seq? spec)
      (dorun (map #(convert-pax-file in %1 out sheet) spec))
      (convert-pax-file in spec out sheet))))

(def cli-options
  [
;;   ["-i" "--input" "input file name" :default "" ]
   ["-i" "--input INPUT" "input file name"]
   ["-s" "--sheet SHEET" "name of the sheet"]
   ["-o" "--output OUTPUT" "output file name"]
   ["-h" "--hint HINT" "give hint for specifications"]
   ])

(defn -main[& args]
  (let [options (parse-opts args cli-options)
        in (:input (:options options))
        sheet (:sheet (:options options))
        out (:output (:options options))
        hint (:hint (:options options))
        ]
    (do
      (println (str "Parsing " in " into " out))
      (process in out sheet hint)
      )))
