(ns shopdb.items
    (:require [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [shopdb.misc :refer :all]
              [mongolib.core :as db]
              [shopdb.tags :refer :all]))

;;-----------------------------------------------------------------------------

(defn-spec create-item-obj :shop/item
    "create a new item"
    [iname :shop/entryname
     parent :shop/parent
     tag :item/tag
     proj :item/project
     url :shop/url
     price :item/price
     oneshot :item/oneshot]
    (-> (create-entity iname)
        (assoc :parent  parent
               :tag     tag
               :project proj
               :url     url
               :price   price
               :oneshot oneshot)))

(defn-spec add-item-usage any?
           [list-id (s/nilable :shop/_id), item-id :shop/_id, action keyword?, numof integer?]
           (db/mc-insert "add-item-usage" "item-usage"
                      (assoc (db/mk-std-field)
                          :listid list-id
                          :itemid item-id
                          :action action
                          :numof numof)))

;;-----------------------------------------------------------------------------

(defn-spec ^:private upd-item :shop/item
    [itm any?]
    (if (nil? (:project itm))
        itm
        (update-in itm [:project :deadline] #(if % % nil))))

(defn-spec get-item-names (s/coll-of (s/keys :req-un [:shop/_id :shop/entryname]))
	[]
	(db/mc-find-maps "get-item-names" "items" {} {:_id true :entryname true}))

(defn-spec get-items :shop/items
	[]
    (map upd-item (db/mc-find-maps "get-items" "items" {})))

(defn get-raw-items
    []
    (db/mc-find-maps "get-raw-items" "items" {}))

(defn-spec get-item :shop/item
	[id :shop/_id]
    (upd-item (db/mc-find-one-as-map "get-item" "items" {:_id id})))

(defn-spec item-id-exists? boolean?
	[id :shop/_id]
	(= (get (db/mc-find-map-by-id "item-id-exists?" "items" id {:_id true}) :_id) id))

(defn-spec add-item :shop/item
	[entry :shop/item]
	(when (:tag entry)
        (add-tag (-> entry :tag :entryname)))
	(add-item-usage nil (:_id entry) :create 0)
	(db/mc-insert "add-item" "items" entry)
	entry)

(defn-spec update-item any?
	[entry :shop/item]
	(add-item-usage nil (:_id entry) :update 0)
    (db/mc-replace-by-id "update-item" "items" entry))

(defn-spec delete-item any?
	[item-id :shop/_id]
	(add-item-usage nil item-id :delete 0)
	(db/mc-remove-by-id "delete-item" "items" item-id))

(st/instrument)
