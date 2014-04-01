(ns parser.pax.core)

(defn get-filename []
  (fn [specs value]
    (:filename specs)))

(defn get-capture-date []
  (fn [specs value]
    (:capture-date specs)))

(defn get-valid-from []
  (fn [specs value]
    (:valid-from specs)))

(defn get-valid-to []
  (fn [specs value]
    (:valid-to specs)))

