(ns shopdb.misc
    (:require [mongolib.core :as db]
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [shopdb.spec :refer :all]
              [clojure.string :as str]))

(defonce no-id "00000000-0000-0000-0000-000000000000")

(defn-spec mk-enlc :shop/entrynamelc
           [en :shop/entryname]
           (-> en str/trim str/lower-case (str/replace anti-lcname-regex "")))

(defn-spec update-enlc (s/keys :req-un [:shop/entryname :shop/entrynamelc])
           [entity (s/keys :req-un [:shop/entryname])]
           (assoc entity :entrynamelc (mk-enlc (:entryname entity))))

(defn-spec set-name (s/keys :req-un [:shop/entryname :shop/entrynamelc])
           [entity map?, ename :shop/entryname]
           (->> ename
                str/trim
                (assoc entity :entryname)
                (update-enlc)))

(defn-spec create-entity (s/keys :req-un [:shop/_id :shop/created :shop/entryname :shop/entrynamelc])
           [ename :shop/string]
           (-> (db/mk-std-field)
               (set-name ename)))

(defn-spec get-by-enlc (s/nilable map?)
           [tbl :shop/string, en :shop/string]
           (db/mc-find-one-as-map "get-by-enlc" tbl {:entrynamelc en}))

;;-----------------------------------------------------------------------------

(st/instrument)
