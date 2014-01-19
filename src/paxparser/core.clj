(ns paxparser.core)

(defn set-keyword [key value]
  (fn [ctxt]
    (merge ctxt {key value :result true})))

(defn set-version [value]
  (fn [ctxt]
    (merge ctxt {:version value :result true})))

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
    
(defn header [fns]
  (fn [ctxt line]
    (let [ctxts (map #(% ctxt line) fns) ;list of separate context'
          final-result (some #(true? (:result %)) ctxts)
          ctxt* (apply merge {} ctxts)
          ]
      (merge ctxt* {:result final-result}))))

;; use some function to process the lines until a line returns false ?

(def config
  {:header
   [(line-contains? ["SCHEDULE" "Inbound" "Outbound"])
    (line-contains? ["NACIONAL"] (set-keyword :traffic "domestic"))
    (line-contains? ["2013"] (set-version "2013"))
    (line-empty?)]})
 ;header is a simple datastructure

