(ns shopdb.tags
    (:require [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [clojure.string :as str]
              [monger.operators :refer :all]
              [shopdb.misc :refer :all]
              [mongolib.core :as db]))

;;-----------------------------------------------------------------------------

(defn-spec get-tags :shop/tags
	[]
	(db/mc-find-maps "get-tags" "tags"))

(defn-spec get-tag :shop/tag
	[id :shop/_id]
	(db/mc-find-map-by-id "get-tag" "tags" id))

(defn-spec get-tag-names (s/coll-of (s/keys :req-un [:shop/_id :shop/entryname]))
           []
           (db/mc-find-maps "get-tag-names" "tags" {} {:_id true :entryname true}))

(defn-spec get-listid-by-name (s/nilable :shop/_id)
    [list-name :shop/string]
    (db/mc-find-one-as-map "get-list" "lists" {:entryname list-name} {:_id true}))

(defn-spec fix-list-ref (s/nilable :shop/_id)
    [lst (s/nilable string?)]
    (cond
        (str/blank? lst)         nil
        (s/valid? :shop/_id lst) lst
        :else                    (get-listid-by-name lst)))

(defn-spec update-tag any?
    ([tag :shop/tag]
     (db/mc-replace-by-id "update-tag" "tags" tag))
    ([tag-id :shop/_id, tag-name* :tags/entryname, parent* :shop/parent]
	(let [tag-name   (->> tag-name* str/trim str/capitalize)
		  tag-namelc (mk-enlc tag-name)
		  db-tag     (get-by-enlc "tags" tag-namelc)
          parent     (fix-list-ref parent*)]
        (when (or (str/blank? tag-name) (str/includes? tag-name " "))
            (throw+ {:type :db :src "update-tag" :cause "invalid name"}))
		(when (and (some? db-tag) (not= (:_id db-tag) tag-id))
			(throw+ {:type :db :src "update-tag" :cause "duplicate name"}))
        (db/mc-update-by-id "update-tag" "tags" tag-id
			{$set {:entryname tag-name :entrynamelc tag-namelc :parent parent}}))))

(defn-spec add-tag :shop/tag
    ([tag-name :shop/entryname]
        (add-tag tag-name nil))
    ([tag-name :shop/entryname, parent (s/nilable string?)]
    (let [new-tag (assoc (create-entity (str/capitalize tag-name))
                            :parent (fix-list-ref parent))
          db-tag  (get-by-enlc "tags" (:entrynamelc new-tag))]
		(if (some? db-tag)
			db-tag
			(do
				(db/mc-insert "add-tag" "tags" new-tag)
				new-tag)))))

(defn-spec delete-tag any?
	[id :shop/_id]
	(db/mc-remove-by-id "delete-tag" "tags" id))

(defn-spec delete-tag-all any?
	[id :shop/_id]
	(delete-tag id)
	(db/mc-update "delete-tag-all" "lists"    {} {$pull {:tag {:_id id}}} {:multi true})
	(db/mc-update "delete-tag-all" "recipes"  {} {$pull {:tag {:_id id}}} {:multi true})
	(db/mc-update "delete-tag-all" "menus"    {} {$pull {:tag {:_id id}}} {:multi true})
	(db/mc-update "delete-tag-all" "projects" {} {$pull {:tag {:_id id}}} {:multi true})
	(db/mc-update "delete-tag-all" "items"    {} {$pull {:tag {:_id id}}} {:multi true})
	)

(st/instrument)
