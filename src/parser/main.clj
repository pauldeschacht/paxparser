(ns parser.main
  (:use [parser.pax.dsl.bts-db1b :as db1b])
  (:use [parser.pax.dsl.anac-2 :as anac-2014])
  (:gen-class))

(defn -main[]
  (test-anac-2014-core))
