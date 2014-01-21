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
    (let [values (map (fn [name]
                        (filter #(= (:name %) name) cells))
                      names)])))
;;
;; generic function that processes lines while (condition? ctxt line) is true
;;
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
         (map #(token-to-cells ctxt %1 %2) input-specs) ;; vector of cells (cell is a hashmap)

         (flatten)
         (filter #(not (nil? (:name %)))) ;; remove cells without a name
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

(defn clean-cell [cell]
  (dissoc cell :index :split :repeat-down :transform))

(defn converter-fn [column-specs]
  (fn [ctxt cells] ;; returns a vector of cells
    (->> cells
         (merge-cells-with-column-specs column-specs) ;; enrich cells with config details
         (map #(skip-cell ctxt %)) ;;
         (map #(transform-cell ctxt %))
         (map #(repeat-cell %1 %2) (:prev-row ctxt))
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
(defn complete-output-spec [{:keys [name source value] output-spec}]
  (if (nil? value)
    (if (nil? source)
      (merge {:source name} output-spec)
      output-spec)
    output-spec)
  )

(defn complete-output-specs [output-specs]
  (map #(complete-output-spec %) output-specs))

(defn copy-value-into-output [ctxt cells {:keys [name source value]}]
  (if (not (nil? value))
    {:name name :value value}
    (if (not (nil? source))
      (let [source-cell (find-spec-by-name cells name)]
        {:name name (:value source-cell)}
        )
      ))
  )

(defn copy-values-into-output [ctxt cells {:keys [name merge]}]
  (if (not (nil? name))
    (if-let [cell (find-cell-by-name name cells)]
      (copy-value-into-output ctxt cells output-spec)
      {:name name :value nil})
    (if (not (nil? merge))
      (merge-output-values ctxt cells merge)
      {:name name :value nil}))
  )

(defn output-fn [output-specs]
  (let [full-output-specs (complete-output-specs output-specs)])
  (fn [ctxt cells]
    (map #(copy-values-into-output ctxt cells %) cells)))

;;
;; PROCESS THE LINES
;;
(defn process [config lines]
  (let [
        header? (header?-fn (:header config))
        
        tokenizer (tokenizer-fn (:input config))
        converter (converter-fn (:columns config))
        process-row (process-row-fn tokenizer converter)
        
        output (output-fn (:output config))

        footer? (footer?-fn (:footer config))
        ]

    (->> {:context config :lines lines}
         (process-lines-while header? nil)
         (process-lines-while (not (or (header? footer?))) process-row )
         (process-lines-while footer? nil)
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
             {:name "country" :transform (aci-trim)}]
   :output [{:name "type" :value "airport"}
            {:name "code"}
            {:name "name"}
            {:name "country" ;; :merge (merge-cells ["name" "country"] "-")
             }
            {:name "tottot"}
            {:name "increase"}]
   })

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
(defn read-lines [filename]
  (clojure.string/split (slurp filename) #"\n"))

(defn process-aci [lines]
  (let [full-spec (add-defaults-to-spec aci-international-spec)
        header? (header?-fn (:header full-spec))
        tokenizer (tokenizer-fn (:input full-spec))
        converter (converter-fn (:columns full-spec))
        process-row (process-row-fn tokenizer converter)
        footer? (footer?-fn (:footer full-spec))]
    
    (->> {:context full-spec :lines lines}
         (process-lines-while header? nil)
         (process-lines-while (fn [ctxt line]
                                (not (or (header? ctxt line)
                                         (footer? ctxt line)))) process-row)
         (process-lines-while footer? nil))))