(ns parser.core-test
  (:use clojure.test)
  (:use parser.core))

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
  (fn [specs value & line]
    (clojure.string/replace value "-" "")))

(defn get-test-specs []
  {:skip [(line-empty?)]
   :global {:token-separator " "
            :quote "\""
            :output-separator "\t"}
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
                      (tokenize-lines (get-in specs [:global :token-separator])
                                      (get-in specs [:global :quote]))
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
                      (tokenize-lines (get-in specs [:global :token-separator])
                                      (get-in specs [:global :quote]))
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
                      (tokenize-lines (get-in specs [:global :token-separator])
                                      (get-in specs [:global :quote]))
                      (lines-to-cells (:tokens specs))
                      (transpose-lines (:transpose specs))
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
                      (tokenize-lines (get-in specs [:global :token-separator])
                                      (get-in specs [:global :quote]))
                      (lines-to-cells (:tokens specs))
                      (transpose-lines (:transpose specs))
                      (merge-lines-with-column-specs (:columns specs))
                      (add-new-column-specs-lines (:columns specs))
                      (transform-lines specs)
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
         (tokenize-lines (get-in specs [:global :token-separator])
                         (get-in specs [:global :quote]))
         (lines-to-cells (:tokens specs))
         (transpose-lines (:transpose specs))
         (merge-lines-with-column-specs (:columns specs))
         (add-new-column-specs-lines (:columns specs))
         (transform-lines specs)
         )))

(deftest test-output
  (testing "test single output"
    (let [specs (get-test-specs)
          lines (test-get-transformed-lines)
          csv (->> lines
                   (output-lines specs)
                   (clean-outputs-lines)
                   (outputs-to-csv-lines ",")
                   (first)
                   )
          
          ]
      (is (= (first csv) ",Id,Country," ))
      (is (= csv '(",Id,Country," ",50,Belgium," ",70,France,") ))
      )

    ))

(deftest test-repeat-down-cell
  (testing "repeat-down-cell"
    (let [prev-cell {:value "5"}
          cell      {:repeat-down true}
          new-cell  (repeat-down-cell prev-cell cell)
          ]
      (is (= "5" (:value new-cell)))))
  (testing "repeat-down-cell-with-value"
    (let [prev-cell {:value "5"}
          cell {:value "6" :repeat-down true}
          new-cell (repeat-down-cell prev-cell cell)]
      (is (= "6" (:value new-cell)))))
  (testing "repeat-down-cell-with-value"
    (let [prev-cell {:value "5"}
          cell {:value "" :repeat-down true}
          new-cell (repeat-down-cell prev-cell cell)]
      (is (= "5" (:value new-cell))))))


(defn get-repeat-lines []
  (let [
        line1 {:columns [{:value "1.1"} {:value "1.2"} {:value "1.3" :repeat-down true}]}
        line2 {:columns [{:value "2.1"} {:value "2.2"} {:value "" :repeat-down true}]}
        line3 {:columns [{:value "3.1"} {:value "3.2"} {:value "3.3" :repeat-down true}]}
        line4 {:columns [{:value "4.1"} {:value "4.2"} {:value "" :repeat-down false}]}
        lines [line1 line2 line3 line4]

        ]
    lines
    )
  )

(deftest repeat-lines
  (let [specs {}
        lines (get-repeat-lines)
        lines* (repeat-down-lines specs lines)
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


(defn transpose-get-lines []
  (let [
        line1 "Id Country Jan Feb Mar Apr"
        line2 "50 Belgium 11 12 13"
        line3 "60 France 21 22 23"
        line4 "70 Germany 31 32 33"
        lines [line1 line2 line3 line4]
        ]
    lines
    ))

(defn transpose-get-specs []
  {:skip [(line-empty?)
          (line-contains? ["Id"])]
   :global {:token-separator " "
            :quote "\""
            }
   :tokens [ {:index 0 :name "identifier"}
             {:index 1 :name "country"}
             {:index 2 :name "jan"}
             {:index 3 :name "feb"}
             {:index 4 :name "mar"}
             {:index 5 :name "apr"}
             {:index 6 :name "may"}
             ]
   :transpose {:header-column "month"
               :value-column "pax"
               :tokens ["jan" "feb" "mar" "apr" "may"]
               }
   
   :columns [{:name "country"}
             {:name "month"}
             {:name "pax"}]
   :output [{:name "filler" :value ""}  ;; add new column with fixed value
            {:name "country"}  ;; ignore region
            {:name "month"}
            {:name "pax"}]
   }
  )

(defn get-value-for-column [name line]
  (:value (first (filter #(= name (:name %1)) (:columns line)))))

(deftest transpose-test
  ( testing "Transpose columns"
    (let [specs (transpose-get-specs)
          lines (transpose-get-lines)
          result (->> lines
                      (wrap-text-lines)
                      (skip-lines (:skip specs))
                      (remove-skip-lines)
                      (tokenize-lines (get-in specs [:global :token-separator])
                                      (get-in specs [:global :quote]))
                      (lines-to-cells (:tokens specs))
                      (transpose-lines (:transpose specs))
                      (merge-lines-with-column-specs (:columns specs))
                      (add-new-column-specs-lines (:columns specs))
                      (transform-lines specs))
          ]
      (is (= 9 (count result)))
      (is (= "Belgium" (get-value-for-column "country" (nth result 0))))
      (is (= "11" (get-value-for-column "pax" (nth result 0))))
      (is (= "jan" (get-value-for-column "month" (nth result 0))))

      (is (= "Belgium" (get-value-for-column "country" (nth result 1))))
      (is (= "feb" (get-value-for-column "month" (nth result 1))))
      (is (= "12" (get-value-for-column "pax" (nth result 1))))
      
      (is (= "Belgium" (get-value-for-column "country" (nth result 2))))
      (is (= "13" (get-value-for-column "pax" (nth result 2))))
      (is (= "mar" (get-value-for-column "month" (nth result 2))))

      (is (= "France" (get-value-for-column "country" (nth result 3))))
      (is (= "21" (get-value-for-column "pax" (nth result 3))))
      (is (= "jan" (get-value-for-column "month" (nth result 3))))

      (is (= "France" (get-value-for-column "country" (nth result 4))))
      (is (= "feb" (get-value-for-column "month" (nth result 4))))
      (is (= "22" (get-value-for-column "pax" (nth result 4))))
      
      (is (= "France" (get-value-for-column "country" (nth result 5))))
      (is (= "23" (get-value-for-column "pax" (nth result 5))))
      (is (= "mar" (get-value-for-column "month" (nth result 5))))

      ))
  )
