(ns shopdb.menus
    (:require [clj-time.core :as t]
              [clj-time.coerce :as c]
              [clj-time.periodic :as p]
              [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [clojure.set :as set]
              [monger.operators :refer :all]
              [monger.joda-time :refer :all]
              [mongolib.core :as db]
              [shopdb.recipes :refer :all]))

;;-----------------------------------------------------------------------------

(defn- fix-date
	[m]
	(update m :date #(->> % c/to-date c/from-date)))

(defn time-range
    "Return a lazy sequence of DateTime's from start to end, incremented
    by 'step' units of time."
    [start end step]
    (let [inf-range (p/periodic-seq start step)
          below-end? (fn [t] (t/within? (t/interval start end) t))]
        (take-while below-end? inf-range)))

(defn-spec add-menu :shop/menu
	[entry :shop/menu]
	(db/mc-insert "add-menu" "menus" entry)
	entry)

(defn-spec update-menu any?
	[entry :shop/menu]
	(db/mc-replace-by-id "update-menu" "menus" entry))

(defn-spec add-recipe-to-menu any?
	[menu-dt :shop/date, recipe-id :shop/_id]
	(let [recipe (get-recipe recipe-id)]
		(db/mc-update "add-recipe-to-menu" "menus" {:date menu-dt}
			{$set {:recipe (select-keys recipe [:_id :entryname])}} {:upsert true})))

(defn-spec remove-recipe-from-menu any?
	[menu-dt :shop/date]
	(db/mc-update "remove-recipe-from-menu" "menus" {:date menu-dt} {$unset {:recipe nil}}))

(defn-spec get-menu (s/nilable :shop/menu)
    [date :shop/date]
    (some->> (db/mc-find-one-as-map "get-menu" "menus" {:date date})
             (fix-date)))

(defn-spec get-menus :shop/x-menus
           [from :shop/date, to :shop/date]
           (let [db-menus (->> (db/mc-find-maps "get-menus" "menus" {:date {$gte from $lt to}})
                               (map fix-date))
                 new-menus (set/difference (set (time-range from to (t/days 1)))
                                           (set (map :date db-menus)))]
               (sort-by :date (concat db-menus (map (fn [dt] {:date dt}) new-menus)))))

(st/instrument)
