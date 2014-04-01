(ns parser.pax.dsl.destatis
  (:use [parser.core])
  (:use [parser.pax.core])
  (:use [parser.pax.dsl.generic-pax-output]))
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


