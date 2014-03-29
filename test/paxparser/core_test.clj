(ns paxparser.core-test
  (:use clojure.test
        paxparser.core))

(defn get-test-lines []
  (let [
        line1 "Id Country Region"
        line2 "50 -Belgium- Europe"
        line3 ""
        line4 "70 -France- Europe"
        line5 ""
        ]
    [line1 line2 line3 line4 line5]
    ))

(defn test-clean-country []
  (fn [specs value]
    (clojure.string/replace value "-" "")))

(defn get-test-specs []
  {:skip [(line-empty?)]
   :global {:token-separator " "}
   :tokens [ {:index 0 :name "identifier"}
             {:index 1 :name "country"}
             {:index 2 :name "region"}]
   :columns [{:name "country" :transform (test-clean-country)}
             {:name "total"}]
   :output [{:name "filler" :value ""}  ;; add new column with fixed value
            {:name "identifier"}
            {:name "country"}  ;; ignore region
            {:name "total"}]
   }
  )

(deftest test-wrap-lines
  (testing "wrap text lines"
    (let [specs (get-test-specs)
          lines (get-test-lines)
          lines* (wrap-text-lines lines)
          ]
      (is (= {:text (nth lines 0) :line-nb 1}  (nth lines* 0)))
      (is (= {:text (nth lines 1) :line-nb 2}  (nth lines* 1)))
      (is (= {:text (nth lines 2) :line-nb 3}  (nth lines* 2)))
      (is (= {:text (nth lines 3) :line-nb 4}  (nth lines* 3)))
      (is (= {:text (nth lines 4) :line-nb 5}  (nth lines* 4))))))

(deftest test-skip-lines
  (testing "skip empty lines"
    (let [specs (get-test-specs)
          lines (get-test-lines)
          lines* (wrap-text-lines lines)
          lines** (skip-lines (:skip specs) lines*)
          ]
      (is (= {:text (nth lines 0) :line-nb 1 :skip nil}  (nth lines** 0)))
      (is (= {:text (nth lines 1) :line-nb 2 :skip nil}  (nth lines** 1)))
      (is (= {:text (nth lines 2) :line-nb 3 :skip true}  (nth lines** 2)))
      (is (= {:text (nth lines 3) :line-nb 4 :skip nil}  (nth lines** 3)))
      (is (= {:text (nth lines 4) :line-nb 5 :skip true}  (nth lines** 4)))))
  (testing "removing skip lines"
    (let [specs (get-test-specs)
          lines (get-test-lines)
          lines* (->> lines
                      (wrap-text-lines)
                      (skip-lines (:skip specs))
                      (remove-skip-lines)
                      )
          ]
      (is (= {:text (nth lines 0) :line-nb 1 :skip nil}  (nth lines* 0)))
      (is (= {:text (nth lines 1) :line-nb 2 :skip nil}  (nth lines* 1)))
      (is (= {:text (nth lines 3) :line-nb 4 :skip nil}  (nth lines* 2))))))
  

(deftest test-tokenizer-lines
  (testing "tokenize lines"
    (let [specs (get-test-specs)
          lines (get-test-lines)
          lines* (->> lines
                      (wrap-text-lines)
                      (skip-lines (:skip specs))
                      (remove-skip-lines)
                      (tokenize-lines (re-pattern (get-in specs [:global :token-separator])))
                      )
          ]
      (is (= {:text (nth lines 0)
              :line-nb 1
              :skip nil
              :split-line ["Id" "Country" "Region"] }  (nth lines* 0)))
      (is (= {:text (nth lines 1)
              :line-nb 2
              :skip nil
              :split-line ["50" "-Belgium-" "Europe"] }  (nth lines* 1)))
      (is (= {:text (nth lines 3)
              :line-nb 4
              :skip nil
              :split-line ["70" "-France-" "Europe"] }  (nth lines* 2)))
      ))
  (testing "convert the tokens to cells"
    (let [specs (get-test-specs)
          lines (get-test-lines)
          lines* (->> lines
                      (wrap-text-lines)
                      (skip-lines (:skip specs))
                      (remove-skip-lines)
                      (tokenize-lines (re-pattern (get-in specs [:global :token-separator])))
                      (lines-to-cells (:tokens specs))
                      )]
      (is (= {:text (nth lines 0)
              :line-nb 1
              :skip nil
              :split-line ["Id" "Country" "Region"]
              :cells [{:value "Id" :name "identifier" :index 0}
                      {:value "Country" :name "country" :index 1}
                      {:value "Region" :name "region" :index 2}
                      ] }  (nth lines* 0)))
      
      (is (= {:text (nth lines 1)
              :line-nb 2
              :skip nil
              :split-line ["50" "-Belgium-" "Europe"]
              :cells [{:value "50" :name "identifier" :index 0}
                      {:value "-Belgium-" :name "country" :index 1}
                      {:value "Europe" :name "region" :index 2}
                      ] }  (nth lines* 1)))
      (is (= {:text (nth lines 3)
              :line-nb 4
              :skip nil
              :split-line ["70" "-France-" "Europe"]
              :cells [{:value "70" :name "identifier" :index 0}
                      {:value "-France-" :name "country" :index 1}
                      {:value "Europe" :name "region" :index 2}]}  (nth lines* 2))))))

(deftest test-columns
  (testing "test conversion of the columns"
    (let [specs (get-test-specs)
          lines (get-test-lines)
          lines* (->> lines
                      (wrap-text-lines)
                      (skip-lines (:skip specs))
                      (remove-skip-lines)
                      (tokenize-lines (re-pattern (get-in specs [:global :token-separator])))
                      (lines-to-cells (:tokens specs))
                      (merge-lines-with-column-specs (:columns specs))
                      (add-new-column-specs-lines (:columns specs))
                      )
          columns2 (:columns (nth lines* 1))
          ]
      (is (= {:index 0 :name "identifier" :value "50"} (nth columns2 0)))
      (is (true? (contains? (nth columns2 1) :transform)))
      (is (= "-Belgium-" (:value (nth columns2 1))))
      (is (= {:index 2 :name "region" :value "Europe"} (nth columns2 2)))
      (is (= {:name "total"} (nth columns2 3)))
      )
    )
  (testing "test transformation of the columns"
    (let [specs (get-test-specs)
          lines (get-test-lines)
          lines* (->> lines
                      (wrap-text-lines)
                      (skip-lines (:skip specs))
                      (remove-skip-lines)
                      (tokenize-lines (re-pattern (get-in specs [:global :token-separator])))
                      (lines-to-cells (:tokens specs))
                      (merge-lines-with-column-specs (:column specs))
                      (add-new-column-specs-lines (:columns specs))
                      (transform-lines (:columns specs))
                      )
          columns2 (:columns (nth lines* 1))
          ]
      (is (= {:index 0 :name "identifier" :value "50"} (nth columns2 0)))
      (is (true? (contains? (nth columns2 1) :transform)))
      (is (= "Belgium" (:value (nth columns2 1))))
      (is (= {:index 2 :name "region" :value "Europe"} (nth columns2 2)))
      (is (= {:name "total"} (nth columns2 3)))
      )
    )
  )

(defn test-get-transformed-lines []
  (let [specs (get-test-specs)]
    (->> (get-test-lines)
         (wrap-text-lines)
         (skip-lines (:skip specs))
         (remove-skip-lines)
         (tokenize-lines (re-pattern (get-in specs [:global :token-separator])))
         (lines-to-cells (:tokens specs))
         (merge-lines-with-column-specs (:columns specs))
         (add-new-column-specs-lines (:columns specs))
         (transform-lines (:columns specs))
         )))

(deftest test-output
  (testing "test single output"
    (let [specs (get-test-specs)
          lines (test-get-transformed-lines)
          lines* (output-lines specs lines)
          ]
      (println lines*)
      (is (= true true))
      )

    ))

(deftest test-repeat-down-cell
  (testing "repeat-down-cell"
    (let [prev-cell {:value "5"}
          cell      {:repeat-down true}
          new-cell  (new-repeat-down-cell prev-cell cell)
          ]
      (is (= "5" (:value new-cell)))))
  (testing "repeat-down-cell-with-value"
    (let [prev-cell {:value "5"}
          cell {:value "6" :repeat-down true}
          new-cell (new-repeat-down-cell prev-cell cell)]
      (is (= "6" (:value new-cell)))))
  (testing "repeat-down-cell-with-value"
    (let [prev-cell {:value "5"}
          cell {:value "" :repeat-down true}
          new-cell (new-repeat-down-cell prev-cell cell)]
      (is (= "5" (:value new-cell))))))



(deftest repeat-lines
  (let [specs {}
        line1 {:cells [{:value "1.1"} {:value "1.2"} {:value "1.3" :repeat-down true}]}
        line2 {:cells [{:value "2.1"} {:value "2.2"} {:value "" :repeat-down true}]}
        line3 {:cells [{:value "3.1"} {:value "3.2"} {:value "3.3" :repeat-down true}]}
        line4 {:cells [{:value "4.1"} {:value "4.2"} {:value "" :repeat-down false}]}
        lines [line1, line2 line3 line4]
        lines* (new-repeat-down-lines specs lines)
        ]
    (is (= "1.1" (:value (nth (:columns (nth lines* 0))  0 ))))
    (is (= "1.2" (:value (nth (:columns (nth lines* 0))  1 ))))
    (is (= "1.3" (:value (nth (:columns (nth lines* 0))  2 ))))    
    (is (= "2.1" (:value (nth (:columns (nth lines* 1))  0 ))))
    (is (= "2.2" (:value (nth (:columns (nth lines* 1))  1 ))))
    (is (= "1.3" (:value (nth (:columns (nth lines* 1))  2 ))))
    (is (= "3.1" (:value (nth (:columns (nth lines* 2))  0 ))))
    (is (= "3.2" (:value (nth (:columns (nth lines* 2))  1 ))))
    (is (= "3.3" (:value (nth (:columns (nth lines* 2))  2 ))))
    (is (= "4.1" (:value (nth (:columns (nth lines* 3))  0 ))))
    (is (= "4.2" (:value (nth (:columns (nth lines* 3))  1 ))))
    (is (= ""    (:value (nth (:columns (nth lines* 3))  2 ))))
    )
  )
(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
