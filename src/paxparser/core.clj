(ns paxparser.core)
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
    (let [_ (println "start process-data")
          processed-data (process ctxt data)
          _ (println "process-data " processed-data )]
      (merge ctxt {:processed-rows
                   (conj (:processed-rows ctxt) processed-data)}))))

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
      ;;(merge ctxt* {:result final-result})
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

(defn do-nothing [text data]
  (do (println text " " data))
  data)


(defn converter-fn [column-specs]
  (fn [ctxt cells] ;; returns a vector of cells
    (->> cells
         (merge-cells-with-column-specs column-specs) ;; enrich cells with config details
         (map #(skip-cell ctxt %)) ;;
         (map #(transform-cell ctxt %))
         (do-nothing "transformed cells")
         (map #(repeat-cell %1 %2) (:prev-row ctxt))
         (do-nothing "repeated cells"))
    

    ))
;;
;; ROW (TOKENIZER and CONVERTER)
;;
(defn process-row-fn [tokenizer converter]
  (fn [ctxt line]
    (->> line
         (tokenizer ctxt) ;; line --> vector of tokens --> vector of basic cells
         (do-nothing "after tokenizer")
         (converter ctxt) ;; vector basic cells --> vector of enriched/transformed cells
         (do-nothing "after converter")
         (conj (:processed-rows ctxt))
         (merge ctxt)
         ))) 
;;
;; OUTPUT
;;
;; merge with {:source n} where n comes from {:name n}
(defn complete-output-specs [output-specs]
  (map #(merge {:source (:name %)} %)
       output-specs)
  )

(defn output-fn [output-specs]
  (let [full-output-specs (complete-output-specs output-specs)])
  (fn [ctxt cells]
    cells))

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
;;ADD EMPTY ROW FOR THE REPEATER --> no longer used
;;
(defn construct-empty-row [spec]
  {:prev-row (map #(merge {:value nil} %) spec)}
  )
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
   :prev-row (cycle [nil])
   }
  )

;; test config
;;
(def config
  {:global { :token-separator ","
            :thousand-separator nil
            :decimal-separator "."}
   :header[(line-contains? ["SCHEDULE" "Inbound" "Outbound"])
           (line-contains? ["NACIONAL"] (set-keyword :traffic "domestic"))
           (line-contains? ["2013"] (set-version "2013"))
           (line-empty?)]
   })
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
;; raw pax spec
;;

(def raw-pax-spec
  {
   :global {:thousand-separator ","
            :decimal-separator "."
             :token-separator ","
            }
   :header [(line-contains? [";"])
            ]
   :input [
           {:index 0 :name "airline"}
           {:index 1 :split (split-into-cells ["year" "month" "date"] "/")}
           {:index 6 :name "arrint"}
           {:index 7 :name "totint"}
           ]
   :columns [{:name "airline" :repeat-down true}
             {:name "year"} ;;splitted should be possible to derive from input-specs
             {:name "month" :transform (convert-to-int)}
             {:name "date" :transform (convert-to-int)}
             ]  
   :output [
            {:name "type" :source "type"} ;; default :source "type"
            {:name "source" :source "source"}
            {:name "filename" :source (get-keyword :filename)}
            {:name "date" :merge (merge-into-cell ["year" "month" "01"] "-")}
            {:name "code"}
            {:name "domarr"}
            ]})

(def raw-pax-lines
  [";this is a sample"
   "airline1,2013/05/18,2,3,4,5,300,600,garbage"
   ",2013/04/17,,,,,1000,2000"]
  )

(defn test-header [file-spec lines]
  (let [header-conditions (:header file-spec)
        header? (header?-fn header-conditions)
        env {:context file-spec :lines lines}]
    (process-lines-while header? nil env)
    ))

(defn test-tokenizer [file-spec lines]
  (let [complete-input-spec (complete-tokenizer-specs (:input file-spec))
        tokenizer (tokenizer-fn complete-input-spec)]
    (map #(tokenizer file-spec %1) lines)))

(defn test-tokenizer-verbose [file-spec line]
  (let [complete-input-spec (complete-tokenizer-specs (:input file-spec))
        _ (println "specs: " complete-input-spec)
        tokenizer (tokenizer-fn complete-input-spec)
        tokens (line-to-tokens #"," line)
        _ (println "tokens: " tokens)
        cells1 (token-to-cells {} (first complete-input-spec) (first tokens))
        _ (println "cells 1: " cells1)
        cells2 (token-to-cells {} (second complete-input-spec) (second tokens))
        _ (println "cells 2: " cells2)
        allcells (map #(token-to-cells {} %1 %2) complete-input-spec tokens)
        _ (println "all cells: " allcells)
        allcells* (flatten allcells)
        _ (println "flatten cells: " allcells*)
        cells* (filter #(not ( nil? (:name %))) allcells*)
        _ (println "result: " cells*)
        ]))

(defn test-converter [file-spec rows]
  (let [complete-converter-specs (complete-column-specs (:input file-spec) (:columns file-spec))
        converter (converter-fn complete-converter-specs)
        ]
    (map #(converter {:prev-row (cycle [nil])} %1) rows))
  )


(defn test-converter-verbose [file-spec row]
  (let [complete-converter-specs (complete-column-specs (:input file-spec) (:columns file-spec))
        converter (converter-fn complete-converter-specs)

        empty-row (construct-empty-row complete-converter-specs)

        ctxt {:prev-row (cycle [nil])}
        
        cells (merge-cells-with-column-specs complete-converter-specs row) ;; enrich cells with config details
;;        _ (println "enriched cells: " cells)
        
        skipped-cells (map #(skip-cell ctxt %) cells)
;;        _ (println "skip cells: " skipped-cells)
        
        transformed-cells (map #(transform-cell ctxt %) skipped-cells)
;;        _ (println "transformed cells: " transformed-cells)

;;        tst1 (repeat-cell (first (:prev-row ctxt)) (first transformed-cells))
;;        _ (println "test: " tst1)
        
;;        tst2 (repeat-cell (second (:prev-row ctxt)) (second transformed-cells))
;;        _ (println "test: " tst2)

        repeat-cells (map #(repeat-cell %1 %2) (:prev-row ctxt) transformed-cells)
;;        _ (println "result: " repeat-cells)

        ]
    repeat-cells
    ))

(defn test-colums [file-spec lines]
  (let [])
  )


(defn test-process-rows [full-spec env]
  (let [header? (header?-fn (:header full-spec))
        tokenizer (tokenizer-fn (:input full-spec))
        converter (converter-fn (:columns full-spec))
        process-row (process-row-fn tokenizer converter)
        footer? (footer?-fn (:footer full-spec))
        ]
    (process-lines-while (fn [ctxt line]
                           (not (or (header? ctxt line)
                                    (footer? ctxt line)))) process-row env))
  
  )

