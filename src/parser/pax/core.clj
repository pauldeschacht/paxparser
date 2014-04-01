(ns parser.pax.core
  (:use [clj-time.core :as t])
  (:use [clj-time.format :as f]))

(defn capture-date-str [year month]
  (t/date-time (read-string year) (read-string month)))

(defn valid-period-month
  ([^long year ^long month]
     (let [f (t/first-day-of-the-month year month)
           l (t/last-day-of-the-month  year month)]
       (t/interval f l)))
  ([^long year ^long m1 ^long m2]
     (let [f (t/first-day-of-the-month year m1)
           l (t/last-day-of-the-month  year m2)]
       (t/interval f l))))

(defn valid-period-month-str
  ([^String year ^String month]
   (valid-period-month (read-string year) (read-string month)))
  ([^String year ^String m1 ^String m2]
     (valid-period-month (read-string year) (read-string m1) (read-string m2)))
  )


(defn valid-period-quarter-str [year quarter]
  (condp = (read-string quarter)
    1 (valid-period-month (read-string year) 1 3)
    2 (valid-period-month (read-string year) 4 6)
    3 (valid-period-month (read-string year) 7 9)
    4 (valid-period-month (read-string year) 10 12)
    nil
    ))

(defn valid-period-year-str [year]
  (valid-period-month (read-string year) 1 12))

(defn extract-file-information-re [file-info-matcher]
  (let [re (:result file-info-matcher)]
    (condp = (:type file-info-matcher)
      :month {:fullname (get re 0)
              :capture (capture-date-str (get re 1) (get re 2))
              :source (get re 3)
              :valid (valid-period-month-str (get re 4) (get re 5))
              :filename (get re 6)}
      
      :year  {:fullname (get re 0)
              :capture (capture-date-str (get re 1) (get re 2))
              :source (get re 3)
              :valid (valid-period-year-str (get re 4))
              :filename (get re 5)}
      
      :month* {:fullname (get re 0)
               :capture (capture-date-str (get re 1) (get re 2))
               :source (get re 3)
               :folder (get re 4)
               :valid (valid-period-month-str (get re 5) (get re 6))
               :filename (get re 7)}
      
      :year* {:fullname (get re 0)
              :capture (capture-date-str (get re 1) (get re 2))
              :source (get re 3)
              :folder (get re 4)
              :valid (valid-period-year-str (get re 5))
              :filename (get re 6)}
      
      :quar* {:fullname (get re 0)
              :capture (capture-date-str (get re 1) (get re 2))
              :source (get re 3)
              :folder (get re 4)
              :valid (valid-period-quarter-str (get re 5) (get re 6))
              :filename (get re 7)}
      
      nil)))

(defn get-re-list []
(let [p-source "([a-zA-Z0-9]*)"
      p-file "([a-zA-Z0-9-_.]*)$"
      p-folder "([a-zA-Z0-9-_]*)"
      p-capture "(\\d{4})/(\\d{2})"
      p-yyyy "(\\d{4})"
      p-mm   "(\\d{2})"
      p-q    "Q(\\d)"
      re-month (re-pattern (str p-capture "/" p-source "/" p-yyyy "/" p-mm "/" p-file))
      re-year  (re-pattern (str p-capture "/" p-source "/" p-yyyy "/" p-file))
      re-month* (re-pattern (str p-capture "/" p-source "/" p-folder "/" p-yyyy "/" p-mm "/" p-file))
      re-year* (re-pattern (str p-capture "/" p-source "/" p-folder "/" p-yyyy "/" p-file))
      re-quar* (re-pattern (str p-capture "/" p-source "/" p-folder "/" p-yyyy "/" p-q "/" p-file))
        re-list [{:type :month :re re-month}
                 {:type :year :re re-year}
                 {:type :month* :re re-month*}
                 {:type :year* :re re-year*}
                 {:type :quar* :re re-quar*}]

      ]
  re-list
  ))

(defn file-info-matcher [re s]
  (merge {:type (:type re)
          :result (re-find (:re re) s)}))

(defn extract-file-information [filename]
  (let []
    (->> (get-re-list)
         (map #(file-info-matcher % filename))
         (filter #(not (nil? (:result %))))
         (first)
         (extract-file-information-re)
         )))

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


