(ns parser.pax.core-test
  (:use clojure.test
        parser.core
        parser.pax.core
        [clj-time.core :exclude (second extend) :as t]
        ))

(deftest test-file-information
  (testing "simple year month"
    (let [info (extract-file-information "2014/02/ACSA/2013/10/data.csv")]
      (is (= (:source info) "ACSA"))
      (is (= (:capture info) (t/date-time 2014 02)))
      (is (= (:valid info) (t/interval (t/date-time 2013 10 01) (t/date-time 2013 10 31))))
      (is (= (:filename info) "data.csv"))
      ))
  (testing "simple year"
    (let [info (extract-file-information "2014/02/China/2013/data.csv")]
      (is (= (:source info) "China"))
      (is (= (:capture info) (t/date-time 2014 02)))
      (is (= (:valid info) (t/interval (t/date-time 2013 01 01) (t/date-time 2013 12 31))))
      (is (= (:filename info) "data.csv"))
      )
    )
  (testing "extra folder year month"
    (let [info (extract-file-information "2014/02/ACI/international/2013/10/data.csv")]
      (is (= (:source info) "ACI"))
      (is (= (:capture info) (t/date-time 2014 02)))
      (is (= (:valid info) (t/interval (t/date-time 2013 10 01) (t/date-time 2013 10 31))))
      (is (= (:filename info) "data.csv"))
      ))
  (testing "extra folder year"
    (let [info (extract-file-information "2014/02/BTS/DB1B/2013/data.csv")]
      (is (= (:source info) "BTS"))
      (is (= (:capture info) (t/date-time 2014 02)))
      (is (= (:valid info) (t/interval (t/date-time 2013 01 01) (t/date-time 2013 12 31))))
      (is (= (:filename info) "data.csv"))
      )
    )
  (testing "extra folder year quarter"
    (let [info (extract-file-information "2014/02/BTS/DB1B/2011/Q3/data.csv")]
      (is (= (:source info) "BTS"))
      (is (= (:capture info) (t/date-time 2014 02)))
      (is (= (:valid info) (t/interval (t/date-time 2011 7 01) (t/date-time 2011 9 30))))
      (is (= (:filename info) "data.csv"))
      )
    )
  )
