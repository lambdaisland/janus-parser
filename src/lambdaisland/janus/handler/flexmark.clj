(ns lambdaisland.janus.flexmark
  (:require [lambdaisland.janus.util :as util]))

(defn extract-version-data [node]
  (-> node
      (.getChars)
      (.toString)
      (util/extract-version-components)))

(defn is-version? [node]
  (and
   (= (type node) com.vladsch.flexmark.ast.Heading)
   (= (.getLevel node) 1))) ;; In CHANGELOG domain everything with "# " or level 1 heading is version info

(defn object-to-text [node]
  (.toString (.getChars node)))

(defn retrieve-component [tag node]
  (let [tag-repr (str "## " tag)]
    (loop [aux-node node
           found? false]
      (if (or (nil? aux-node) (= found? true))
        aux-node
        (let [shared-node (when (not (nil? aux-node)) (.getNext aux-node))]
          (recur shared-node (if (not (nil? shared-node))
                               (= (.toString (.getChars shared-node)) tag-repr)
                               false)))))))

(defn extract-list [tag node]
  (let [component (when (not (nil? (retrive-component tag node))))
        bullet-list
        (if (nil? component)
          (list)
          (.getNext component))]
    (if (and (not (nil? bullet-list))
             (= (type bullet-list) com.vladsch.flexmark.ast.BulletList))
      (map (fn [x] (object-to-text x))
           (iterator-seq (.iterator (.getChildren (bullet-list)))))
      (list))))

(defn extract-changes [node]
  (extract-list "Changed" node))

(defn extract-fixtures [node]
  (extract-list "Fixed" node))

(defn extract-additions [node]
  (extract-list "Added"))

(defn build-item [node]
  (let [version-data (extract-version-data node)]
    {:version-id version-data
     :date       version-data
     :sha        version-data
     :added      (extract-additions node)
     :fixed      (extract-fixtures  node)
     :changed    (extract-changes node)}))

(defn build-changelog [document]
  (let [version-list (filter
                      (fn [x] (is-version? x))
                      (iterator-seq (.iterator document)))]
    (map (fn [x] (build-item x)) version-list)))
