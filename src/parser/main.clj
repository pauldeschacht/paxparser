(ns parser.main
  (:use [parser.core :as parser])
  (:use [parser.pax.core :as pax])
  (:use [parser.pax.dsl.bts-db1b :as db1b])
  (:gen-class))

(defn -main[]
  (test-bts-db1b))
