(ns parser.core
  (:use [clojure.java.io])
  (:use [dk.ative.docjure.spreadsheet])
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
  (fn [value]
    (some #(substring? % value) words)))

(defn convert-to-int []
  (fn [value & thousand-separator]
    (if (not (empty? value))
      (if (nil? thousand-separator)
        (read-string (clojure.string/replace value thousand-separator ""))
        (read-string value))
      nil
      )))

(defn split-into-cells [names separator]
  (fn [token]
    (let [values (clojure.string/split token (re-pattern separator))
          cells (map #(hash-map :name %1 :value %2)
                     names
                     (lazy-cat values (cycle [nil])))]
      cells)))

(defn merge-into-cell [names join-separator]
  (fn [cells]
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
  (-> (DataFormatter.)
      (.formatCellValue cell)))
(defn xls-row-to-tokens [^Row row]
  (->> (map #(.getCell row % Row/RETURN_BLANK_AS_NULL) (range 0 (.getLastCellNum row)))
       (map #(if (nil? %) nil (xls-cell-to-value %)))))
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
(defmethod row-to-tokens java.lang.String [data re-separator]
  (clojure.string/split data re-separator))
(defmethod row-to-tokens org.apache.poi.hssf.usermodel.HSSFRow [data & args]
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
(defmulti read-lines
  (fn [params]
    (get-file-extension params) ;;?? pass params and not (:filename params)
    ))
(defmethod read-lines :csv [{:keys [filename max]}]
  (let [lines (clojure.string/split (slurp filename) #"\n")]
    (if (nil? max)
      lines
      (take max lines))))
(defmethod read-lines :xls [{:keys [filename max]}]
  (let [workbook (load-workbook filename)
        sheet (select-sheet 0 workbook)
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
;;
;; TOKENIZER
;; ---------

;;
;; split line into separate tokens
;;
(defn tokenize-line [re-separator line]
  (merge line {:split-line (row-to-tokens (:text line) re-separator)}))

(defn tokenize-lines [re-separator lines]
  (map #(tokenize-line re-separator %) lines))
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

(defn build-token-spec-with-index [index token-specs]
  (if-let [existing-token-spec (first (filter #(= (:index %) index)  token-specs))]
    existing-token-spec
    {:index index :name nil}))

(defn complete-token-specs [token-specs]
  (let [max-index (apply max (map #(:index %) token-specs))]
    (reduce #(conj %1 %2)
            []
            (map #(build-token-spec-with-index % token-specs) (range 0 (inc max-index))))))

(defn tokenizer-lines [specs lines]
  (let [token-specs (:tokens specs)]
    (->> lines
         (tokenize-lines (re-pattern (get-in specs [:global :token-separator])))
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

(defn merge-cell [cells current-cell]
  (if-let [merge-fn (:merge current-cell)]
    (merge current-cell {:value (merge-fn cells)})
    current-cell))

(defn merge-cells [cells]
  (map #(merge-cell cells %) cells))

(defn clean-cell [cell]
  (dissoc cell :index :split :repeat-down :transform))

(defn transform-cell [specs cell]
  (if-let [transform-fn (:transform cell)]
    (merge cell {:value (transform-fn specs (:value cell))})
    cell))

(defn transform-line [specs line]
  (merge line {:columns (map #(transform-cell specs %) (:columns line))})
  )

(defn transform-lines [specs lines]
  (map #(transform-line specs %1) lines))

;; :value must be string type
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
    (merge line {:columns (:cells line)})
    (merge line {:columns (map #(repeat-down-cell %1 %2) (:cells prev-line) (:cells line))})))

(defn repeat-down-lines [specs lines]
  (map #(repeat-down-line specs %1 %2) (cons nil lines) lines))

(defn columns-lines [specs lines]
  (->> lines
       (map #(merge-line-with-column-specs (:columns specs) %1))
       (map #(add-new-column-specs-lines specs %1))
       (map #(transform-line specs %1))
       (repeat-down-lines lines)
       ))
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
               (map #(transform-output-spec-to-output %1 (:columns line)) output-specs)}))

(defn single-output-lines [output-specs lines]
  (let [full-output-specs (add-source-to-output-specs output-specs)]
    (map #(single-output-line full-output-specs %1) lines)))

(defn multi-output-lines [multi-output-specs lines]
  (map #(single-output-lines %1 lines) multi-output-specs))

(defn output-lines [specs lines]
  (if (contains? specs :output)
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
    (doall (map (fn [line]
                  (.write wrtr line)
                  (.write wrtr "\n"))
                csv-lines
                )))
  true)

(defn csv-outputs-to-file [filename outputs-csv-lines]
  (doall (map #(csv-output-to-file filename %1) outputs-csv-lines))
  true)
;;
;; ADD DEFAULT VALUES TO ALL THE SPEC
;;
(defn add-defaults-to-specs [specs]
  {:global (merge  {:token-separator (str (char 31))
                    :thousand-separator nil
                    :decimal-separator "."
                    :header-lines 0
                    :converted-lines 0
                    :footer-lines 0
                    :output-separator "\t"}
                   (:global specs))
   :skip (:skip specs)
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
       (tokenize-lines (re-pattern (get-in specs [:global :token-separator])))
       (lines-to-cells (:tokens specs))
       (merge-lines-with-column-specs (:columns specs))
       (add-new-column-specs-lines (:columns specs))
       (transform-lines (:columns specs))
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
          (tokenize-lines (re-pattern (get-in specs* [:global :token-separator])))
         (lines-to-cells (:tokens specs*))
         (merge-lines-with-column-specs (:columns specs*))
         (add-new-column-specs-lines (:columns specs*))
         (transform-lines (:columns specs*))
         (output-lines specs*)
         (clean-outputs-lines)
         (outputs-to-csv-lines (get-in specs* [:global :output-separator]))
         (csv-outputs-to-file output-filename)
         )))
