(ns parser.core
  (:use [clojure.java.io])
  (:use [dk.ative.docjure.spreadsheet])
  (:use [clojure.data.csv :as csv])
  (:import (org.apache.poi.ss.usermodel Row Cell DataFormatter DateUtil)))
;;
;; debug function
;;
(defn- do-nothing [text data]
  (do (println text " " data)
      (println "type = " (type data)))
  data)
;;
;; default function for the configuration
;;
(defn substring? [word line]
  (let [str-word (str word)
        str-line (str line)]
    (cond
     (empty? str-word) nil
     (nil? str-line) nil
     :else (not (= -1 (.indexOf (str str-line) (str str-word)))))))

(defn line-contains? [words & body]
  (fn [line]
    (if (some #(substring? % line) words)
      (if (not (nil? body))
        (let [[fun & args] body]
          (fun))
        true)
      false)))

(defn line-empty? [& body]
  (fn [line]
    (empty? (clojure.string/trim line))))

(defn cell-contains? [words & body]
  (fn [specs value]
    (some #(substring? % value) words)))

(defn convert-to-int []
  (fn [specs value]
    (if (not (empty? value))
      (try
        (let [thousand-separator (get-in specs [:global :thousand-separator])]
          (if (empty? thousand-separator)
            (int (Double/parseDouble value))
            (int (Double/parseDouble (clojure.string/replace value thousand-separator "")))))
        (catch Exception e (println (.getMessage e)))
        )
      
      nil
      )))

(defn convert-to-double []
  (fn [specs value]
    (if (not (empty? value))
      (try
        (let [thousand-separator (get-in specs [:global :thousand-separator])]
          (if (empty? thousand-separator)
            (Double/parseDouble value)
            (Double/parseDouble (clojure.string/replace value thousand-separator ""))))
        (catch Exception e (println (.getMessage e)))
        )
      
      nil
      )))

(defn copy-into-cells [names]
  (fn [token]
    (let [cells (map #(hash-map :name %1 :value token)
                     names)]
      cells)))

(defn split-into-cells [names separator]
  (fn [token]
    (let [values (clojure.string/split token (re-pattern separator))
          cells (map #(hash-map :name %1 :value %2)
                     names
                     (lazy-cat values (cycle [nil])))]
      cells)))

(defn merge-from [names join-separator]
  (fn [specs cells]
    (->> names
         (map (fn [name] (filter #(= (:name %) name) cells)))
         (flatten)
         (map #(str (:value %1)))
         (clojure.string/join join-separator)
         )))
;;
;; READ LINES FROM CSV/XLS/XLSX
;;
;;
;; extract values from XLS(X)
;;
(defn xls-cell-to-value [^Cell cell]
  (read-cell cell)
  ;; (-> (DataFormatter.)
  ;;     (.formatCellValue cell))
  )
(defn xls-row-to-tokens [^Row row]
  (->> (map #(.getCell row % Row/RETURN_BLANK_AS_NULL) (range 0 (.getLastCellNum row)))
       (map #(if (nil? %) "" (xls-cell-to-value %)))))
(defn xls-row-to-line [^Row row]
  (->> row
       (xls-row-to-tokens)
       (filter #(not (nil? %)))
       (clojure.string/join (char 31))))
;;
;; multi method row-to-string
;;
(defmulti row-to-string (fn [data] (class data)))
(defmethod row-to-string java.lang.String [data]
  (str data))
(defmethod row-to-string org.apache.poi.hssf.usermodel.HSSFRow [data]
  (xls-row-to-line data))
(defmethod row-to-string org.apache.poi.xssf.usermodel.XSSFRow [data]
  (xls-row-to-line data))
(defmethod row-to-string :default [data]
  "")
;;
;; multi method row-to-tokens
;;
(defmulti row-to-tokens (fn [data & args] (class data)))
(defmethod row-to-tokens java.lang.String [data separator quote]
  (first (csv/read-csv data :separator (first separator) :quote (first quote))))
(defmethod row-to-tokens org.apache.poi.hssf.usermodel.HSSFRow [data & args]
  (xls-row-to-tokens data))
(defmethod row-to-tokens org.apache.poi.xssf.usermodel.XSSFRow [data]
  (xls-row-to-tokens data))
(defmethod row-to-tokens :default [data & args]
  [])

(defn get-file-extension [params]
  (let [filename (:filename params)
        extension (last (clojure.string/split filename #"\."))]
    (cond
     (= extension "xlsx") :xlsx
     (= extension "xls")  :xlsx
     (= extension "csv")  :csv
     (= extension "tsv")  :csv
     (= extension "txt")  :csv
     :else :error
     )))
(defn excel-sheet-to-lines [sheet]
  (->> sheet
       (row-seq)
       ))

(defn lazy-file-lines [filename]
  (letfn [(helper [rdr]
            (lazy-seq
             (if-let [line (.readLine rdr)]
               (cons line (helper rdr))
               (do (.close rdr) nil))))]
    (helper (clojure.java.io/reader filename))))

(defmulti read-lines
  (fn [params]
    (get-file-extension params) ;;?? pass params and not (:filename params)
    ))
(defmethod read-lines :csv [{:keys [filename max]}]
  (let [
        ;lines (clojure.string/split (slurp filename) #"\n")
        ]
    (if (nil? max)
      (lazy-file-lines filename)
      (take max (lazy-file-lines filename)))))

(defmethod read-lines :xls [{:keys [filename sheetname max]}]
  (let [workbook (load-workbook filename)
        sheet (select-sheet sheetname workbook)
        lines (excel-sheet-to-lines sheet)
        lines* (map #(row-to-string %) lines)
        ]
    (if (nil? max)
      lines*
      (take max lines*))))
(defmethod read-lines :xlsx [params]
  (let [{:keys [filename sheetname max]} params
        workbook (load-workbook filename)
        sheet (select-sheet sheetname workbook)
        lines (excel-sheet-to-lines sheet)
        lines* (map #(row-to-string %) lines)
        ]
    (if (nil? max)
      lines*
      (take max lines*))))
(defmethod read-lines :default [& args]
  [])
;;
;; WRAP LINES
;; ----------
;;
(defn wrap-text-lines [lines]
  (map #(hash-map :line-nb %1 :text %2) (range 1 java.lang.Integer/MAX_VALUE) lines))
;;
;; SKIP LINES
;; ----------
;;
(defn skip-line? [skip-fns text]
  (some true?
        (map #(%1 text) skip-fns)))

(defn skip-line [skip-fns line]
  (merge line {:skip (skip-line? skip-fns (:text line))}))

(defn skip-lines [skip-fns lines]
  (map #(skip-line skip-fns %1) lines))

(defn remove-skip-lines [lines]
  (filter #(not (true? (:skip %1))) lines))

(defn take-line [stop-fns line]
  (not (some true? (map #(%1 (:text line)) stop-fns))))

(defn stop-after [stop-fns lines]
  (let [pred (partial take-line stop-fns)]
    (take-while pred lines))
  )
;;
;; TOKENIZER
;; ---------

;;
;; split line into separate tokens
;;
(defn tokenize-line [separator quote line]
  (merge line {:split-line (row-to-tokens (:text line) separator quote)}))

(defn tokenize-lines [separator quote lines]
  (map #(tokenize-line separator quote %) lines))
;;
;; merge each substring with corresponding token specification
;;
(defn token-to-cells [token-specs token]
  (if-let [split-fn (:split token-specs)]
    (map #(merge token-specs %1) (split-fn token))
    [(merge token-specs {:value token})]))

(defn line-to-cells [token-specs line]
  (merge line {:cells 
               (->> (:split-line line)
                    (map #(token-to-cells %1 %2) token-specs)
                    (flatten)
                    (filter #(not (nil? (:name %))))
                    (map #(dissoc % :split))
                    )}))

(defn lines-to-cells [token-specs lines]
  (map #(line-to-cells token-specs %) lines))

(defn tokenizer-lines [specs lines]
  (let [token-specs (:tokens specs)]
    (->> lines
         (tokenize-lines (get-in specs [:global :token-separator]) (get-in specs [:global :quote]))
         (lines-to-cells token-specs))))
;;
;; COLUMNIZER
;;
(defn find-spec-by-name [specs name]
  (let [specs-with-name (filter #(= (:name %) name) specs)]
    (if (nil? (first specs-with-name))
      {}
      (first specs-with-name))))
;; for each cell, find column-spec with the same :name
;;   if such column-spec exists, merge the cell and column-spec
;;   otherwise, just keep the cell
;;
;; :columns-spec [ {:name "new" :transform true} {:name "country" :transform true} ]
;; :cells        [ {:name "country" :value "BE } {:name "region" :value "EUR" } ]
;; :columns      [ {:name "country" :value "BE" :transform true} {:name "region" :value "EUR"} ]
(defn merge-cells-with-column-specs [columns-specs cells]
  (map #(merge (find-spec-by-name columns-specs (:name %1)) %1)
       cells))

(defn merge-line-with-column-specs [columns-specs line]
  (merge line {:columns (merge-cells-with-column-specs columns-specs (:cells line))}))

(defn merge-lines-with-column-specs [column-specs lines]
  (map #(merge-line-with-column-specs column-specs %1) lines))
;;
;; for any column-spec that has no corresponding cell (not the same :name)
;; add the  column-spec as cell
;;
;; :columns-spec [ {:name "new" :transform true} {:name "country" :transform true} ]
;; :cells        [ {:name "country" :value "BE } {:name "region" :value "EUR" } ]
;; :columns      [ {:name "country" :value "BE" :transform true} {:name "region" :value "EUR"} {:name "new" :transform true} ]
(defn new-cell? [columns column-spec]
  (empty? (find-spec-by-name columns (:name column-spec))))

(defn add-new-column-spec [column-specs columns]
  ;; column-specs [ {:name a_very_special_name :transform fn } { ... } ... ]
  ;; cells        [ {:name region :value "EUR" } {:name country ... }
  ;; result       [ {:name a_very_special_name .. } {:name region :value "EUR" } {:name country ... }
  (concat
   columns
   (filter #(new-cell? columns %) column-specs))
  )

(defn add-new-column-specs-line [column-specs line]
  (merge line {:columns (add-new-column-spec column-specs (:columns line))})
  )

(defn add-new-column-specs-lines [column-specs lines]
  (map #(add-new-column-specs-line column-specs %1) lines))

(defn merge-cell [specs columns current-column]
  (if-let [merge-fn (:merge current-column)]
    (merge current-column {:value (merge-fn specs columns)})
    current-column))

(defn merge-line [specs line]
  (merge line
         {:columns (map #(merge-cell specs (:columns line) %) (:columns line))}))

(defn merge-lines [specs lines]
  (map #(merge-line specs %) lines))

(defn clean-cell [cell]
  (dissoc cell :index :split :repeat-down :transform))

;; transform only the value
(defn transform-cell [specs cell]
  (if-let [transform-fn (:transform cell)]
    (merge cell {:value (transform-fn specs (:value cell))})
    cell))

(defn transform-line [specs line]
  (merge line {:columns (map #(transform-cell specs %) (:columns line))}))

(defn transform-lines [specs lines]
  (map #(transform-line specs %1) lines))

;; pass all columns to user function
(defn find-cell-by-name [name cells]
  (first (filter #(= name (:name %1)) cells)))

(defn get-named-cell-value [name cells]
  (if-let [cell (find-cell-by-name name cells)]
    (:value cell)
    nil)
  ) 

(defn transform-full-line-cell [specs cells cell]
  (if-let [transform-fn (:transform-line cell)]
    (merge cell {:value (transform-fn specs cells (:value cell))})
    cell))

(defn transform-full-line [specs line]
  (merge line {:columns (map #(transform-full-line-cell specs (:columns line) %) (:columns line))}))

(defn transform-full-lines [specs lines]
  (map #(transform-full-line specs %1) lines))

(defn skip-transformed-cell? [specs cell]
  (if-let [skip-fn (:skip-line cell)]
    (skip-fn specs (:value cell))
    false))

(defn skip-transformed-line? [specs cells]
  (some true?
        (map #(skip-transformed-cell? specs %) cells)))

(defn skip-transformed-line [specs line]
  (merge line
         {:skip (skip-transformed-line? specs (:columns line))}))

(defn skip-transformed-lines [specs lines]
  (->> lines
       (map #(skip-transformed-line specs %1))
       (filter #(not (true? (:skip %))))
       ))


;; :value must be string type (:value must support nil? and empty? function)
(defn repeat-down-cell [previous-cell current-cell]
  (if (or (nil? (:repeat-down current-cell))
          (false? (:repeat-down current-cell))
          (nil? previous-cell))
    current-cell
    (if (or (nil? (:value current-cell))
            (empty? (:value current-cell)))
      (merge current-cell {:value (:value previous-cell)})
      current-cell)))

(defn repeat-down-line [specs prev-line line]
  (if (nil? prev-line)
    line
    (merge line {:columns (map #(repeat-down-cell %1 %2) (:columns prev-line) (:columns line))})))

(defn repeat-down-lines
  ([specs lines]
     (repeat-down-lines specs nil lines))
  ([specs prev-line lines]
     (lazy-seq
      (when-let [s (seq lines)]
        (let [repeat-line (repeat-down-line specs prev-line (first s))]
          (cons repeat-line
                (repeat-down-lines specs repeat-line (rest s))))))))

;; (defn repeat-down-lines [specs lines]
;;   (let [acc {:prev nil :result []}]
;;     (:result (reduce (fn [acc line]
;;                        (let [repeated-line (repeat-down-line specs (:prev acc) line)]
;;                          {:prev repeated-line
;;                           :result  (conj (:result acc) repeated-line)
;;                           })
;;                        ) acc lines))))

;;
;; OUTPUT
;; ------
;;
;; merge with {:source n} where n comes from {:name n}
;; result: output-spec has either a :value or a :source
(defn add-source-to-output-spec [output-spec]
  (let [{:keys [name source value]} output-spec]
    (if (nil? value)
      (if (nil? source)
        (merge {:source name} output-spec)
        output-spec)
      output-spec)))

(defn add-source-to-output-specs [output-specs]
  (map #(add-source-to-output-spec %) output-specs))

(defn get-value-from-output-spec [output-spec columns]
  (let [{:keys [value source]} output-spec]
    (if (not (nil? value))
      value
      (if-let [source-cell (find-spec-by-name columns source)]
        (:value source-cell)
        nil))))

(defn transform-output-spec-to-output [output-spec columns]
  (if-let [value (get-value-from-output-spec output-spec columns)]
    (merge output-spec {:value value})
    output-spec))

(defn single-output-line [output-specs line]
  (merge line {:output 
               (map #(transform-output-spec-to-output %1 (:columns line)) output-specs)
               }))

(defn skip-single-output-line [output-specs line]
  (merge line {:skip (skip-transformed-line? output-specs (:output line))}))

(defn single-output-lines [output-specs lines]
  (let [full-output-specs (add-source-to-output-specs output-specs)]
    (->> lines
         (map #(single-output-line full-output-specs %1))
         (map #(skip-single-output-line full-output-specs %1))
         (filter #(not (true? (:skip %))))
         )))

(defn multi-output-lines [multi-output-specs lines]
  (map #(single-output-lines %1 lines) multi-output-specs))

(defn output-lines [specs lines]
  (if (and (contains? specs :output) (not (empty? (:output specs))))
    (multi-output-lines [(:output specs)] lines)
    (multi-output-lines (:outputs specs)  lines)))

(defn clean-output [output]
  (map #(dissoc %1 :source) output))

(defn clean-output-line [line]
  (merge (dissoc line :output)
         {:output  (clean-output (:output line))}))

(defn clean-output-lines [lines]
  (map #(clean-output-line %1) lines))

(defn clean-outputs-lines [outputs]
  (map #(clean-output-lines %1) outputs))

(defn output-to-csv-line [separator output]
  (clojure.string/join separator
                       (map #(:value %1) output)))

(defn output-to-csv-lines [separator lines]
  (let [outputs (map #(:output %1) lines)]
    (map #(output-to-csv-line separator %) outputs)))

(defn outputs-to-csv-lines [separator lines]
  (map #(output-to-csv-lines separator %1) lines))

(defn csv-output-to-file [filename csv-lines]
  (with-open [wrtr (writer filename :append true)]
    (dorun (map (fn [line]
                  (.write wrtr line)
                  (.write wrtr "\n"))
                csv-lines
                )))
  true)

(defn csv-outputs-to-file [filename outputs-csv-lines]
  (dorun (map #(csv-output-to-file filename %1) outputs-csv-lines))
  true)
;;
;; create one token for every possible index 
;;
(defn build-token-spec-with-index [index token-specs]
  (if-let [existing-token-spec (first (filter #(= (:index %) index)  token-specs))]
    existing-token-spec
    {:index index :name nil}))

(defn complete-token-specs [token-specs]
  (let [max-index (apply max (map #(:index %) token-specs))]
    (reduce #(conj %1 %2)
            []
            (map #(build-token-spec-with-index % token-specs) (range 0 (inc max-index))))))
;;
;; ADD DEFAULT VALUES TO ALL THE SPEC
;;
(defn add-defaults-to-specs [specs]
  {:global (merge  {:token-separator (str (char 31))
                    :quote "\""
                    :thousand-separator nil
                    :decimal-separator "."
                    :output-separator "\t"}
                   (:global specs))
   :skip (:skip specs)
   :stop (:stop specs)
   :tokens (complete-token-specs (:tokens specs))
   :columns (:columns specs)
   :output (:output specs)
   :outputs (:outputs specs)
   })

(defn process-lines [specs lines]
  (->> lines
       (wrap-text-lines)
       (skip-lines (:skip specs))
       (remove-skip-lines)
       (tokenize-lines (get-in specs [:global :token-separator]) (get-in specs [:global :quote]))
       (lines-to-cells (:tokens specs))
       (merge-lines-with-column-specs (:columns specs))
       (add-new-column-specs-lines (:columns specs))
       (merge-lines specs)
       (transform-lines specs)
       (repeat-down-lines specs)
       (output-lines specs)
       (clean-outputs-lines)
       (outputs-to-csv-lines (get-in specs [:global :output-separator]))
       ))

(defn convert-file [input-filename specs output-filename sheetname]
  (let [params {:filename input-filename :sheetname sheetname}
        specs* (add-defaults-to-specs specs)
        lines (read-lines params)]
    (->> lines
         (wrap-text-lines)
         (skip-lines (:skip specs*))
         (remove-skip-lines)
         (stop-after (:stop specs*))
         (tokenize-lines (get-in specs* [:global :token-separator]) (get-in specs* [:global :quote]))
         (lines-to-cells (:tokens specs*))
         (merge-lines-with-column-specs (:columns specs*))
         (add-new-column-specs-lines (:columns specs*))
         (merge-lines specs*)
         (transform-lines specs*)
         (repeat-down-lines specs*)
         (transform-full-lines specs*)
         (skip-transformed-lines specs*)
         (output-lines specs*)
         (clean-outputs-lines)
         (outputs-to-csv-lines (get-in specs* [:global :output-separator]))
         (csv-outputs-to-file output-filename)
         )))
