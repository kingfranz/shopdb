(ns shopdb.notes
    (:require [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [monger.operators :refer :all]
              [shopdb.misc :refer :all]
              [mongolib.core :as db]))

;;-----------------------------------------------------------------------------

(defn-spec create-note-obj :shop/note
           [nname :shop/entryname, text string?]
           (-> (create-entity nname)
               (assoc :text text)))

(defn-spec get-note-names (s/coll-of (s/keys :req-un [:shop/_id :shop/entryname]))
           []
           (db/mc-find-maps "get-note-names" "notes" {} {:_id true :entryname true}))

(defn-spec get-notes :shop/notes
           []
           (db/mc-find-maps "get-notes" "notes"))

(defn-spec get-note :shop/note
           [id :shop/_id]
           (db/mc-find-one-as-map "get-note" "notes" {:_id id}))

(defn-spec add-note :shop/note
           [entry :shop/note]
           (db/mc-insert "add-note" "notes" entry)
           entry)

(defn-spec delete-note any?
           [id :shop/_id]
           (db/mc-remove-by-id "delete-note" "notes" id))

(defn-spec update-note :shop/note
           [note :shop/note]
           (db/mc-replace-by-id "update-note" "notes" note)
           note)

;;-----------------------------------------------------------------------------

(st/instrument)
