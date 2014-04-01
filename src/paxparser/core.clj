(ns paxparser.core
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


;;
(def albatross-all-spec
  {:global {:token-separator "\\^"
            :thousand-separator nil
            :decimal-separator nil
            :output-separator "\t"
            }
   :header [(line-contains? ["Total" "Other"])
            ]
   :tokens [{:index 1 :name "iata"}
           {:index 2 :name "icao"}
           {:index 3 :name "airportname"}
           {:index 4 :name "country"}
           {:index 5 :name "region"}
           
           {:index 10 :name "2013_tot"}
           {:index 11 :name "2013_dom"}
           {:index 12 :name "2013_int"}

           {:index 14 :name "2012_tot"}
           {:index 15 :name "2012_dom"}
           {:index 16 :name "2012_int"}

           {:index 18 :name "2011_tot"}
           {:index 19 :name "2011_dom"}
           {:index 20 :name "2011_int"}

           {:index 22 :name "2010_tot"}
           {:index 23 :name "2010_dom"}
           {:index 24 :name "2010_int"}

           {:index 26 :name "2009_tot"}
           {:index 27 :name "2009_dom"}
           {:index 28 :name "2009_int"}

           {:index 30 :name "2008_tot"}
           {:index 31 :name "2008_dom"}
           {:index 32 :name "2008_int"}
           ]
   :columns [
             {:index 10 :name "2013_tot" :transform (convert-to-int)}
             {:index 11 :name "2013_dom" :transform (convert-to-int)}
             {:index 12 :name "2013_int" :transform (convert-to-int)}
             
             {:index 14 :name "2012_tot" :transform (convert-to-int)}
             {:index 15 :name "2012_dom" :transform (convert-to-int)}
             {:index 16 :name "2012_int" :transform (convert-to-int)}
             
             {:index 18 :name "2011_tot" :transform (convert-to-int)}
             {:index 19 :name "2011_dom" :transform (convert-to-int)}
             {:index 20 :name "2011_int" :transform (convert-to-int)}
             
             {:index 22 :name "2010_tot" :transform (convert-to-int)}
             {:index 23 :name "2010_dom" :transform (convert-to-int)}
             {:index 24 :name "2010_int" :transform (convert-to-int)}
             
             {:index 26 :name "2009_tot" :transform (convert-to-int)}
             {:index 27 :name "2009_dom" :transform (convert-to-int)}
             {:index 28 :name "2009_int" :transform (convert-to-int)}
             
             {:index 30 :name "2008_tot" :transform (convert-to-int)}
             {:index 31 :name "2008_dom" :transform (convert-to-int)} 
             {:index 32 :name "2008_int" :transform (convert-to-int)}
             ]
   :outputs [
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2013"}
              {:name "2013_tot"}
              {:name "2013_dom"}
              {:name "2013_int"}             
            ]
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2012"}
              {:name "2012_tot"}
              {:name "2012_dom"}
              {:name "2012_int"}             
            ]
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2011"}
              {:name "2011_tot"}
              {:name "2011_dom"}
              {:name "2011_int"}             
            ]
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2010"}
              {:name "2010_tot"}
              {:name "2010_dom"}
              {:name "2010_int"}             
            ]
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2009"}
              {:name "2009_tot"}
              {:name "2009_dom"}
              {:name "2009_int"}             
              ]
             [
              {:name "type" :value "airport"}
              {:name "iata"}
              {:name "icao"}
              {:name "airportname"}
              {:name "country"}
              {:name "region"}
              {:name "period" :value "2008"}
              {:name "2008_tot"}
              {:name "2008_dom"}
              {:name "2008_int"}             
              ]
             ]
   })
;;
;; ANAC
;;
(defn anac-trim []
  (fn [ctxt value]
    (clojure.string/trim value)))

(def anac
  {:global {:thousand-separator "."
            :decimal-separator ""
            }

   :input [{:index 0 :name "icao_name" :split (split-into-cells ["icao" "name"] "-")}
           {:index 4 :name "domtot"}
           {:index 5 :name "inttot"}
           {:index 6 :name "tottot"}
           ]
   :columns [{:name "icao" :skip-row (cell-contains? "Superintendência") :transform (anac-trim)}
             {:name "name" :transform (anac-trim)}]

   :footer [(line-contains? ["INFRAERO"])]
   
   :output [{:name "type" :value "airport"}
            {:name "iata" :value ""}
            {:name "icao"}
            {:name "name"}
            {:name "region" :value ""}
            {:name "domtot"}
            {:name "inttot"}
            {:name "tottot"}]
   })
;;
;; BTRE INTERNATIONAL
;;
(def btre-int-spec
  {:global {:thousand-separator " "
            :decimal-separator ""
            :token-separator "\\^"
            :output-separator "\t"
            }
   :header [(line-contains? ["TABLE" "Passengers" "Freight" "Foreign" "Inbound" "Outbound"])
            (line-empty?)]
   :input [{:index 0 :name "origin" }
           {:index 1 :name "destination" }
           {:index 2 :name "arrint 2012" }
           {:index 3 :name "depint 2012" }
           {:index 4 :name "totint 2012"}
           {:index 6 :name "arrint 2013"}
           {:index 7 :name "depint 2013"}
           {:index 8 :name "totint 2013"}
           ]
   :columns [{:name "origin" :skip-row (cell-contains? ["Total"])}
             {:name "destination" :repeat-down true}
             {:name "arrint" :transform (convert-to-int)}
             {:name "depint" :transform (convert-to-int)}
             {:name "totint" :transform (convert-to-int)}]
   
   :footer [(line-contains? ["Please"])
            (line-empty?)]
   :outputs [
             [{:name "type" :value "citypair"}
              {:name "year" :value "2012"}
              {:name "iata" :value ""}
              {:name "icao" :value ""}
              {:name "origin"}
              {:name "destination"}
              {:name "arrint 2012"}
              {:name "depint 2012"}
              {:name "totint 2012"}
              ]
             [{:name "type" :value "citypair"}
              {:name "year" :value "2013"}
              {:name "iata" :value ""}
              {:name "icao" :value ""}
              {:name "origin"}
              {:name "destination"}
              {:name "arrint 2013"}
              {:name "depint 2013"}
              {:name "totint 2013"}

              ]
             ]
   })
;;
;; BTRE DOMESTIC
;;
(defn btre-trim []
  (fn [ctxt value]
    (clojure.string/trim value)))

(def btre-dom
  {:global {:thousand-separator " "}
   :input [{:index 1 :name "city-pair" :split (split-into-cells ["origin" "destination"] "-")}
           {:index 2 :name "2012"}
           {:index 3 :name "2013"}
           ]
   :columns [{:name "origin" :transform (btre-trim)}
             {:name "destination" :transform (btre-trim)}
             {:name "2012" :transform (convert-to-int)}
             {:name "2013" :transform (convert-to-int)}]
   
   :output [{:name "type" :value "citypair"}
            {:name "origin"}
            {:name "destination"}
            {:name "tottot" :source "2013"}]

   })
;;
;; BTS - DB1B
;;

;;
;; BTS - T100
;;

;;
;; DESTATIS
;;
(defn destatis-convert-to-int []
  (fn [ctxt value]
    (if (not (empty? value))
      (if (= (clojure.string/trim value) "-")
        nil
        (if-let [thousand-separator (:thousand-separator ctxt)]
          (read-string (clojure.string/replace value thousand-separator ""))
          (read-string value)))
      nil
      )))

(defn create-destatis-outputs []
  (map #(vector {:name "type" :value "citypair"}
                {:name "origin" :source "origin"}
                {:name "destination" :value %1}
                {:name "domtot" :source %1})
          ["Berlin_Schonefeld" "Berlin_Tegel" "Bremem" "Dortmund" "Dresden" "Dusseldorf" "Frankfurt" "Friedrichs" "Hahn" "Hamburg" "Hannover" "Karlsruhe" "Koln"
           "Leipzig" "Memmingen" ]))

(def destatis-dom-spec
  {:global {:thousand-separator " "
            :decimal-separator ""
            :output-separator "\t"
            }
   :header [(line-contains? ["Gewerblicher" "Passagier" "Verkehr" "Luftverkehr" "Streckenzielflughafen" "von" "Streckenherkunfts-" "flughäfen" "Anzahl" "Deutschland insgesamt" "Hauptverkehrsflughäfen"])
            (line-empty?)]
   :input [{:index 0 :name "origin" }
           {:index 1 :name "domttot" }
           {:index 2 :name "mainairports" }
           {:index 3 :name "Berlin_Schonefeld" }
           {:index 4 :name "Berlin_Tegel"}
           {:index 5 :name "Bremen"}
           {:index 6 :name "Dortmund"}
           {:index 9 :name "Dresden"}
           {:index 10 :name "Dusseldorf"}
           {:index 11 :name "Erfurt"}
           {:index 12 :name "Frankfurt"}
           {:index 13 :name "Friedrichs"}
           {:index 14 :name "Hahn"}
           {:index 15 :name "Hamburg"}
           {:index 16 :name "Hannover"}
           {:index 19 :name "Karlsruhe"}
           {:index 20 :name "Koln"}
           {:index 21 :name "Leipzig"}
           {:index 22 :name "Memmingen"}
           ]
   :columns [{:name "origin" :skip-row (cell-contains? ["Total"])}
             {:name "Berlin_Schonefeld" :transform (convert-to-int)}
             {:name "Berlin_Tegel" :transform (destatis-convert-to-int)}
             {:name "Bremen" :transform (destatis-convert-to-int)}
             {:name "Dortmund" :transform (destatis-convert-to-int)}
             {:name "Dusseldorf" :transform (destatis-convert-to-int)}
             ]
             
   :footer [(line-empty?)]
   
   :outputs (create-destatis-outputs)
   })

(defn dgca-month? []
  (fn [ctxt value]
    (some #(substring? % value) ["JAN" "FEB" "MAR" "APR" "MAY" "JUN" "JUL" "AUG" "SEP" "OCT" "NOV" "DEC"]))
  )

(defn dgca-month []
  (fn [ctxt value]
    (-> value
        (clojure.string/trim)
        (clojure.string/replace "(R)" "")
        (clojure.string/replace "(P)" ""))))

(def dgca-india-spec
  {:global {:thousand-separator ","
            :decimal-separator ""
            :output-separator "\t"}

   :header []

   :input [ {:index 0 :name "month"}
            {:index 4 :name "pax"}
            ]

   :columns [ {:name "month" :skip-row (not dgca-month?) :transform (dgca-month)}]

   :outputs [{:name "type" :value "airline"}
             {:name "year" :value "2013"}
             {:name "iata" :value ""}
             {:name "icao" :value ""}
             ]
   
   :footer [(line-contains? ["SOURCE" "ICAO"])]

   })
;;
;;
;; VM
(def aci-international "/home/pdeschacht/dev/paxparser/data/ACI/aci_international.csv")
(def aci-worldwide "/home/pdeschacht/dev/paxparser/data/ACI/aci_worldwide.csv")
(def albatross "/home/pdeschacht/dev/paxparser/data/Albatross/01_2008_importAirport.csv")
(def albatross-all-xls "/home/pdeschacht/dev/paxparser/data/Albatross/Albatross all.xlsx")
(def btre-int-xls "/home/pdeschacht/MyDocuments/prepax-2014-01/BTRE/2013/10/International_airline_activity_1309_Tables.xls")
(def destatis-xls "/home/pdeschacht/MyDocuments/prepax-2014-01/Destatis/2013/09/Luftverkehr2080600131095.xls")

;;home

;;(def albatross-all-xls "/Users/pauldeschacht/Dropbox/dev/paxparser/data/prepax-2014-01/Albatross/2014/01/Albatross all.xlsx")
;;(def btre-int-xls "/Users/pauldeschacht/Dropbox/dev/paxparser/data/prepax-2014-01/BTRE_Australia/2013/10/International_airline_activity_1309_Tables.xls")
;;(def destatis-xls "/Users/pauldeschacht/Dropbox/dev/paxparser/data/prepax-2014-01/Destatis/2013/09/Luftverkehr2080600131095_small.xlsx")

