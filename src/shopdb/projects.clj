(ns shopdb.projects
    (:require [clj-time.local :as l]
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [monger.operators :refer :all]
              [shopdb.misc :refer :all]
              [mongolib.core :as db]
              [shopdb.tags :refer :all]))

;;-----------------------------------------------------------------------------

(defn-spec create-project-obj :shop/project
    [pname :shop/entryname
     parent :shop/parent
     priority :project/priority
     deadline :project/deadline]
    (-> (create-entity pname)
        (assoc :parent   parent
               :priority priority
               :deadline deadline
               :finished nil
               :cleared  nil)))

(defn-spec ^:private upd-proj :shop/project
           [db-proj any?]
           (update db-proj :deadline #(if % % nil)))

(defn-spec get-projects :shop/projects
           []
           (map upd-proj (db/mc-find-maps "get-projects" "projects" {:cleared nil})))

(defn-spec project-id-exist? boolean?
           [id :shop/_id]
           (some? (db/mc-find-one-as-map "get-project-id-exist?" "projects" {:_id id} {:_id true})))

(defn-spec get-active-projects :shop/projects
           []
           (map upd-proj (db/mc-find-maps "get-active-projects" "projects" {:finished nil})))

(defn-spec get-finished-projects :shop/projects
           []
           (map upd-proj (db/mc-find-maps "get-finished-projects" "projects" {:finished {$ne nil} :cleared nil})))

(defn-spec get-project (s/nilable :shop/project)
	[id :shop/_id]
    (upd-proj (db/mc-find-one-as-map "get-project" "projects" {:_id id})))

(defn-spec get-project-names (s/coll-of (s/keys :req-un [:shop/_id :shop/entryname]))
    []
    (->> (db/mc-find-maps "get-project-names" "projects" {:finished nil} {:_id true :entryname true})
         (sort-by :entryname)))

(defn-spec add-project :shop/project
	[entry :shop/project]
	(db/mc-insert "add-project" "projects" entry)
	entry)

(defn-spec finish-project any?
	[project-id :shop/_id]
	(db/mc-update-by-id "finish-project" "projects" project-id {$set {:finished (l/local-now)}}))

(defn-spec unfinish-project any?
	[project-id :shop/_id]
	(db/mc-update-by-id "unfinish-project" "projects" project-id {$set {:finished nil}}))

(defn-spec update-project any?
	[proj :shop/project]
	(db/mc-replace-by-id "update-project" "projects" proj))

(defn clear-projects
	[]
	(db/mc-update clear-projects "projects"
		{:finished {$type "date"}}
		{$set {:cleared (l/local-now)}}
		{:multi true}))

(st/instrument)
