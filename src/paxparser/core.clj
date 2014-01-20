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
          (fun ctxt));;        (merge ctxt {:captain "Bleeksheet"})
        (merge ctxt {:result true}))
      (merge ctxt {:result false}))))

(defn line-empty? [& body]
  (fn [ctxt line]
    (if (empty? line)
      (merge ctxt {:result true})
      (merge ctxt {:result false}))))

(defn split-into-cells [names separator]
  (fn [ctxt cell]
    (let [value (:value cell)
          values (clojure.string/split value (re-pattern separator))
          cell* (map #(hash-map :name %1 :value %2)
                     names
                     (lazy-cat values (cycle [nil])))]
      (merge cell cell*)
      )))

(defn merge-into-cell [names join-separator]
  (fn [ctxt cells]
    (let [values (map (fn [name]
                        (filter #(= (:name %) name) cells))
                      names)])
    ))

;;
;; generic function that processes lines while (condition? ctxt line) is true
;;
(defn process-data [process ctxt data]
  (if (nil? process)
    ctxt
    (process ctxt data)))

(defn process-lines-while [condition? process {:keys [lines ctxt]}]
  (if (empty? lines)
    {:lines lines :ctxt ctxt}
    (let [ctxt* (condition? (first lines))]
      (if (false? (:result ctxt*))
        {:lines lines :ctxt ctxt}
        (let [ctxt** (process-data ctxt* (first lines))]
          (recur process condition? {:lines (rest lines) :ctxt ctxt**}))
        ))))
;;
;; HEADER
;;
(defn header?-fn [conditions]
  (fn [ctxt line]
    (let [ctxts (map #(% ctxt line) conditions) ;list of separate context'
          final-result (some #(true? (:result %)) ctxts)
          ctxt* (apply merge {} ctxts)
          ]
      (merge ctxt* {:result final-result}))))
;;
;; FOOTER
;;
(defn footer?-fn [conditions]
  (fn [ctxt line]
    (merge ctxt {:result false})))
;;
;; TOKENIZER
;;
(defn line-to-tokens [re-separator line]
  (clojure.string/split line re-separator))

(defn token-to-cells [ctxt input-spec token]
  (if-let [split-fn (:split input-spec)]
    (map #(merge %1 %2) (cycle [input-spec] (split-fn ctxt token)))
    [(merge input-spec {:value token})]))

(defn tokenizer-fn [input-specs]
  (fn[ctxt line]
    (->> line
         (line-to-tokens (re-pattern (:token-separator ctxt))) ;; vector of tokens (token is just a string)
         (map #(token-to-cells ctxt %1 %2) input-specs) ;; vector of cells (cell is a hashmap)
         (flatten)
         )))
;;
;; COLUMN CONVERTERS
;;
(defn find-column-spec-by-name [column-specs name]
  (let [specs (filter (= (:name column-specs) name) column-specs)]
    (if (nil? (first specs))
      {}
      (first specs))))

(defn merge-cells-with-column-specs [column-specs cells]
  (map #(merge (find-column-spec-by-name column-specs (:name %)) %)
       cells))

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

(defn converter-fn [column-specs tokenizer]
  (fn [ctxt cells] ;; returns a vector of cells
    (->> cells
         (filter #(not (nil? (:name %)))) ;; remove cells without a name
         (merge-cells-with-column-specs column-specs) ;; enrich cells with config details
         (map #(skip-cell ctxt %)) ;;
         (map #(transform-cell ctxt %))
         (map #(repeat-cell %1 %2) (:prev-row ctxt)))))
;;
;; ROW (TOKENIZER and CONVERTER)
;;
(defn process-row-fn [tokenizer converter]
  (fn [ctxt line]
    (->> line
         (tokenizer ctxt) ;; line --> vector of tokens --> vector of basic cells
         (converter ctxt) ;; vector basic cells --> vector of enriched/transformed cells
         (conj (:processed-rows ctxt))
         (merge ctxt)
         ))) 
;;
;; OUTPUT
;;
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
;; ROW TOKENIZER
;;
(defn tokenizer-spec-at-index [index tokenizer-specs]
  (let [tokenizer-spec-from-config (first (filter #(= (:index %) index)
                                                  tokenizer-specs))]
    (if (nil? tokenizer-spec-from-config)
      {:index index :name nil}
      tokenizer-spec-from-config)))
;;
;; return vector[0..max-index] with tokenizer-specs
;;
(defn complete-tokenizer-specs [tokenizer-specs]
  (let [max-index (apply max (map #(:index %) tokenizer-specs))]
    (reduce #(conj %1 %2)
            []
            (map #(tokenizer-spec-at-index % tokenizer-specs) (range 0 (inc max-index))))))

;;
;; some default functions for the configuration
;;
(defn cell-contains? [& args]
  (fn [ctxt cell]
    false))
;;
;;
;;
(defn convert-to-int [& args]
  (fn [ctxt cell]
    0))

(defn add-defaults-to-file-spec [file-spec]
  (merge {:token-separator ","
          :thousand-separator nil
          :decimal-separator "."
          :header []
          :processed-rows []
          }
         file-spec))
;;
;; test config
;;
(def config
  {:token-separator ","
   :thousand-separator nil
   :decimal-separator "."
   :header
   [(line-contains? ["SCHEDULE" "Inbound" "Outbound"])
    (line-contains? ["NACIONAL"] (set-keyword :traffic "domestic"))
    (line-contains? ["2013"] (set-version "2013"))
    (line-empty?)]
   })
;;
;; yet another config
;;
(def btre-int-file-spec
  {
   :header
   [(line-contains? ["SCHEDULED" "Inbound" "Outbound"])
    (line-empty?)]
   :input
   [{:index 0 :name "airline" }
    {:index 1 :name "country" }
    {:index 2 :name "arrint" }
    {:index 6 :name "depint" }
    ]
   :columns
   [{:name "airline" :repeat-down true}
    {:name "country" :skip-row (cell-contains? "ALL SERVICES")}
    {:name "arrint" :transform (convert-to-int ".")}
    {:name "depint" :transform (convert-to-int ".")}
    ]
   :output
   [{:name "code" :source "airline"}
    ]
   })

;;
;; configuration to process raw pax data
;;

(def raw-pax-data
  {:header [(line-contains? ";")]
   :input [{:index 0 :name "airline"}
           {:index 1 :split (split-into-cells ["year" "month" "date"] "/")}
           {:index 6 :name "arrint"}
           {:index 7 :name "totint"}
           ]
   :columns [{:name "airline" :repeat-down true}
             ]
   
   :output [{:name "type" :source "type"} ;; default :source "type"
            {:name "source" :source "source"}
            {:name "filename" :source (get-keyword :filename)}
            {:name "date" :merge (merge-into-cell ["year" "month" "01"] "-")}
            {:name "code"}
            {:name "domarr"}
            ]})
