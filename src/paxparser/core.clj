(ns paxparser.core)
;;
;; helper function
;;
(defn- do-nothing [text data]
  (do (println text " " data)
      (println "type = " (type data)))
  data)
;;
;; default function for the configuration
;;
(defn set-keyword [key value]
  (fn [ctxt]
    (merge ctxt {key value :result true})))

(defn get-keyword [key]
  (fn [ctxt]
     (key ctxt)))

(defn set-version [value]
  (fn [ctxt]
    (merge ctxt {:version value :result true})))

(defn get-version []
  (fn [ctxt]
    (:version ctxt)))

(defn substring? [word line]
  (not (= -1 (.indexOf line word))))

(defn line-contains? [words & body]
  (fn [ctxt line]
    (if (some #(substring? % line) words)
      (if (not (nil? body))
        (let [[fun & args] body]
          (fun ctxt))
        (merge ctxt {:result true}))
      (merge ctxt {:result false}))))

(defn line-empty? [& body]
  (fn [ctxt line]
    (if (empty? line)
      (merge ctxt {:result true})
      (merge ctxt {:result false}))))

(defn cell-contains? [words]
  (fn [ctxt value]
    (some #(substring? % value) words)))

(defn convert-to-int []
  (fn [ctxt value]
    (if-let [thousand-separator (:thousand-separator ctxt)]
      (read-string (clojure.string/replace value thousand-separator ""))
      (read-string value))))

(defn split-into-cells [names separator]
  (fn [ctxt value]
    (let [values (clojure.string/split value (re-pattern separator))
          cells (map #(hash-map :name %1 :value %2)
                     names
                     (lazy-cat values (cycle [nil])))]
      cells)))

(defn merge-into-cell [names join-separator]
  (fn [ctxt cells]
    (->> names
         (map (fn [name] (filter #(= (:name %) name) cells)))
         (flatten)
         (map #(str (:value %1)))
         (clojure.string/join join-separator)
         )))
;;
;; generic function that processes lines while (condition? ctxt line) is true
;;
(defn true-condition? [ctxt & args]
  (merge ctxt {:result true}))

(defn process-data [process ctxt data]
  (if (nil? process)
    ctxt
    (let [processed-data (process ctxt data)]
      (merge ctxt {:processed-rows (conj (:processed-rows ctxt) processed-data)
                   :prev-row processed-data}))))

(defn process-lines-while [condition? process {:keys [lines context]}]
  (if (empty? lines)
    {:lines lines :context context}
    (let [context* (condition? context (first lines))]
      (if (false? (:result context*))
        {:lines lines :context context}
        (let [context** (process-data process context (first lines))]
          (recur condition? process {:lines (rest lines) :context context**}))
        ))))
;;
;; HEADER
;;
(defn header?-fn [conditions]
  (fn [ctxt line]
    (let [ctxts (map #(% ctxt line) conditions) ;list of separate context'
          final-result (true? (some #(true? (:result %)) ctxts)) ;; true or false
          ctxt* (apply merge {} ctxts)
          ]
      (update-in
       (merge ctxt* {:result final-result})
        [:global :header-lines]
        inc
        )
      )))
;;
;; FOOTER
;;
(defn footer?-fn [conditions]
  (fn [ctxt line]
    (let [ctxts (map #(% ctxt line) conditions) ;list of separate context'
          final-result (true? (some #(true? (:result %)) ctxts)) ;; true or false
          ctxt* (apply merge {} ctxts)
          ]
      (update-in
       (merge ctxt* {:result final-result})
       [:global :footer-lines]
       inc
       ))))
;;
;; TOKENIZER
;;
(defn line-to-tokens [re-separator line]
  (clojure.string/split line re-separator))

(defn token-to-cells [ctxt input-spec token]
  (if-let [split-fn (:split input-spec)]
    (map #(merge input-spec %1) (split-fn ctxt token))
    [(merge input-spec {:value token})]))

(defn tokenizer-fn [input-specs]
  (fn[ctxt line]
    (->> line
         (line-to-tokens (re-pattern (get-in ctxt [:global :token-separator]))) ;; vector of tokens (token is just a string)
         (map #(token-to-cells ctxt %1 %2) input-specs)
         ;; vector of cells
         ;; cell is a hashmap with :index :name :value :split (and others specified in input-spec)
         

         (flatten)
         (filter #(not (nil? (:name %)))) ;;remove cells without a name
         (map #(dissoc % :split))
         )))
;;
;; COLUMN CONVERTERS
;;
(defn find-spec-by-name [specs name]
  (let [specs-with-name (filter #(= (:name %) name) specs)]
    (if (nil? (first specs-with-name))
      {}
      (first specs-with-name))))

(defn merge-cells-with-column-specs [column-specs cells]
  (map #(merge (find-spec-by-name column-specs (:name %)) %)
       cells))

(defn complete-column-specs [specs1 specs2]
  (let [all-specs (concat specs1 specs2)
        clean-specs (filter #(not (nil? (:name %))) all-specs)]
    (for [ [name specs] (group-by :name clean-specs)]
      (apply merge specs)))
  )

(defn new-cell? [input-cells column-spec]
  (empty? (find-spec-by-name input-cells (:name column-spec))))

(defn add-new-cells [column-specs input-cells]
  (concat
   input-cells
   (filter #(new-cell? input-cells %) column-specs)) ;; add column-spec if not yet a cell
  )

(defn skip-cell [ctxt cell]
  (if-let [skip-fn (:skip-row cell)]
    (merge cell {:skip (skip-fn ctxt (:value cell))})
    cell))

(defn transform-cell [ctxt cell]
  (if-let [transform-fn (:transform cell)]
    (merge cell {:value (transform-fn ctxt (:value cell))})
    cell))

(defn repeat-cell [previous-cell current-cell]
  (if (or (nil? (:repeat-down current-cell))
          (false? (:repeat-down current-cell))
          (nil? previous-cell))
    current-cell
    (if (or (nil? (:value current-cell))
            (empty? (:value current-cell)))
      (merge current-cell {:value (:value previous-cell)})
      current-cell)))

(defn merge-cell [ctxt cells current-cell]
  (if-let [merge-fn (:merge current-cell)]
    (let [value (merge-fn ctxt cells)]
      (do
        (merge current-cell {:value value})))
    current-cell
    ))

(defn merge-cells [ctxt cells]
  (map #(merge-cell ctxt cells %) cells))

(defn clean-cell [cell]
  (dissoc cell :index :split :repeat-down :transform))

(defn converter-fn [column-specs]
  (fn [ctxt cells] ;; returns a vector of cells
    (->> cells
         (merge-cells-with-column-specs column-specs) ;; enrich cells with config details
         (add-new-cells column-specs)
;;         (do-nothing "enriched cells")
         (map #(skip-cell ctxt %)) ;;
         (map #(transform-cell ctxt %))
         (map #(repeat-cell %1 %2) (:prev-row ctxt))
         (merge-cells ctxt)
         (map #(clean-cell %1))
         )))
;;
;; ROW (TOKENIZER and CONVERTER)
;;
(defn process-row-fn [tokenizer converter]
  (fn [ctxt line]
    (->> line
         (tokenizer ctxt) ;; line --> vector of tokens --> vector of basic cells
         (converter ctxt) ;; vector basic cells --> vector of enriched/transformed cells
         ))) 
;;
;; OUTPUT
;;
;; merge with {:source n} where n comes from {:name n}
(defn complete-output-spec [output-spec]
  (let [{:keys [name source value]} output-spec]
    (if (nil? value)
      (if (nil? source)
        (merge {:source name} output-spec)
        output-spec)
      output-spec)))

(defn complete-output-specs [output-specs]
  (map #(complete-output-spec %) output-specs))

(defn get-value-for-output-cell [ctxt cells output-cell]
  (let [{:keys [value source]} output-cell]
    (if (not (nil? value))
      value
      (if-let [source-cell (find-spec-by-name cells source)]
        (:value source-cell)
        nil))))

(defn copy-value-into-output-cell [ctxt cells output-cell]
  (if-let [value (get-value-for-output-cell ctxt cells output-cell)]
    (merge output-cell {:value value})
    output-cell))

;; cells is a row
(defn output-row-fn [output-specs]
  (let [full-output-specs (complete-output-specs output-specs)]
    (fn [ctxt cells]
      (->>
       (map #(copy-value-into-output-cell ctxt cells %) full-output-specs)
       (map #(dissoc % :source))
       ))))

;;
;; PROCESS THE LINES
;;
(defn move-lines-in-env [env]
  (update-in
   (merge env {:lines (get-in env [:context :processed-rows])})
   [:context :processed-rows]
   empty
   )
  )
(defn process [config lines]
  (let [
        header? (header?-fn (:header config))
        
        tokenizer (tokenizer-fn (:input config))
        converter (converter-fn (:columns config))
        process-row (process-row-fn tokenizer converter)
        
        output-row (output-fn (:output config))

        footer? (footer?-fn (:footer config))
        ]

    (->> {:context config :lines lines}
         (process-lines-while header? nil)
         (process-lines-while (not (or (header? footer?))) process-row )
         (process-lines-while footer? nil)
         (move-lines-in-env)
         (process-lines-while true-condition? output-row)
         )
    
    ))
;;
;; COMPLETE THE INPUT SPECS
;;
(defn input-spec-at-index [index input-specs]
  (let [input-spec-from-config (first (filter #(= (:index %) index)
                                                  input-specs))]
    (if (nil? input-spec-from-config)
      {:index index :name nil}
      input-spec-from-config)))
;;
;; return vector[0..max-index] with tokenizer-specs
;;
(defn complete-input-specs [input-specs]
  (let [max-index (apply max (map #(:index %) input-specs))]
    (reduce #(conj %1 %2)
            []
            (map #(input-spec-at-index % input-specs) (range 0 (inc max-index))))))

;;
;; ADD DEFAULT VALUES TO ALL THE SPEC
;;
(defn add-defaults-to-spec [spec]
  {:global (merge  {:token-separator ","
                    :thousand-separator nil
                    :decimal-separator "."
                    :header-lines 0
                    :converted-lines 0
                    :footer-lines 0}
                   (:global spec))
   :header (:header spec)
   :input (complete-input-specs (:input spec))
   :columns (:columns spec)
   :output (:output spec)
   :processed-rows []
   ;;   :prev-row (cycle [nil])
   :prev-row [nil nil nil nil nil nil nil nil nil nil nil nil ]
   })

;;
;; ACI functions and specifications
;;
(defn aci-pax-to-int []
  (fn [ctxt value]
    (if (= value "*****")
      nil
      (read-string (clojure.string/replace value " " ""))
      )))

(defn aci-trim []
  (fn [ctxt value]
    (clojure.string/trim value)))

(def aci-spec
  {:global {:token-separator ";"
            :thousand-separator " "
            :decimal-separator "."}
   :header[(line-contains? ["CODE" "COUNTRY"])]
   :input [{:index 0 :name "region"}
           {:index 1 :name "city-country-code" :split (split-into-cells ["name" "country"] ",")}
           {:index 2 :name "code"}
           {:index 3 :name "tottot"}
           {:index 4 :name "increase"}
           ]
   :columns [{:name "tottot" :transform (aci-pax-to-int)}
             {:name "code" :transform (aci-trim)}
             {:name "country" :transform (aci-trim)}
             {:name "name" :transform (aci-trim)}
             {:name "merged" :merge (merge-into-cell ["name" "country" "code"] "*")}
             ]
   :output [{:name "type" :value "airport"}
            {:name "code"}
            {:name "name"}
            {:name "country"}
            {:name "tottot"}
            {:name "increase"}
            {:name "merged"}]
   })

(def albatross-single-year
  {:global {:token-separator ";"
            :thousand-separator nil
            :decimal-separator nil         
            }
   :header [(line-contains? ["2008 Total"] (set-version "2008"))
            (line-contains? ["2009 Total"] (set-version "2009"))
            (line-contains? ["2010 Total"] (set-version "2010"))
            (line-contains? ["2010 Total"] (set-version "2011"))
            (line-contains? ["2010 Total"] (set-version "2012"))
            (line-contains? ["2010 Total"] (set-version "2013"))
            ]
   :input [{:index 1 :name "iata"}
           {:index 2 :name "icao"}
           {:index 3 :name "airportname"}
           {:index 4 :name "country"}
           {:index 5 :name "region"}
           {:index 10 :name "tottot"}
           {:index 11 :name "domtot"}
           {:index 11 :name "inttot"}
           ]
   :columns [{:name "tottot" :transform (convert-to-int)}
             {:name "domtot" :transform (convert-to-int)}
             {:name "inttot" :transform (convert-to-int)}
             ]
   :output [{:name "type" :value "airport"}
            {:name "iata"}
            {:name "icao"}
            {:name "airportname"}
            {:name "country"}
            {:name "region"}
            {:name "tottot"}
            {:name "domtot"}
            {:name "inttot"}
            ]   })

;; CONVERTER
;; :name mandatory, must exist in the input cells
;; :transform; function that will transform the value (defined as string)
;; :skip; if this function returns true, the complete row will be ignored
;; :repeat: if field is true and the value is empty in the cell, the value will be copied from previous row
 
;; OUTPUT
;;  :name mandatory, used to respect sequence in the output file
;;  :value val  --> hardcoded value
;;  :source s   --> find value in converted cell with the name s
;;  :merge      --> function that combines different converter cells
;;

;;
;; btre international config
;;
(def btre-int-file-spec
  {:global {:thousand-separator ","
            :decimal-separator "."
            :token-separator ","
            }
   :header [(line-contains? ["Monthly" "Yearly" "YoY"])
            (line-contains? ["Inbound"] (set-keyword :traffic "inbound"))
            (line-contains? ["Outbound"] (set-keyword :traffic "outbound"))
            (line-empty?)]
   :input [{:index 0 :name "airline" }
           {:index 1 :name "country" }
           {:index 2 :name "arrint" }
           {:index 6 :name "depint" }
           ]
   :columns [{:name "airline" :repeat-down true}
             {:name "country" :skip-row (cell-contains? "ALL SERVICES")}
             {:name "arrint" :transform (convert-to-int)}
             {:name "depint" :transform (convert-to-int)}
             ]
   :output [{:name "code" :source "airline"}
            ]
   })
;;
;;
;;
(def aci-international "/home/pdeschacht/dev/paxparser/data/ACI/aci_international.csv")
(def aci-worldwide "/home/pdeschacht/dev/paxparser/data/ACI/aci_worldwide.csv")
(def albatross "/home/pdeschacht/dev/paxparser/data/Albatross/01_2008_importAirport.csv")

(defn read-lines [filename & [max]] 
  (let [lines (clojure.string/split (slurp filename) #"\n")]
    (if (nil? max)
      lines
      (take max lines))))

(defn process-lines [spec lines]
  (let [full-spec (add-defaults-to-spec spec)
        
        header? (header?-fn (:header full-spec))
        tokenizer (tokenizer-fn (:input full-spec))
        converter (converter-fn (:columns full-spec))
        process-row (process-row-fn tokenizer converter)

        output-row (output-row-fn (:output full-spec))
        
        footer? (footer?-fn (:footer full-spec))]
    
    (->> {:context full-spec :lines lines}
         (process-lines-while header? nil)
         (process-lines-while (fn [ctxt line]
                                (not (or (header? ctxt line)
                                         (footer? ctxt line)))) process-row)
         (process-lines-while footer? nil)
         (move-lines-in-env)
         (process-lines-while true-condition? output-row)
         )))
