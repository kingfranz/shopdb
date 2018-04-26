(ns shopdb.spec
    (:require [clojure.spec.alpha :as s]))

;;-----------------------------------------------------------------------------

; "242be596-a391-4405-a1c0-fa7c3a1aa5c9"
(defonce uuid-regex #"\p{XDigit}{8}-\p{XDigit}{4}-\p{XDigit}{4}-\p{XDigit}{4}-\p{XDigit}{12}")
(defonce tag-name-regex #"[a-zA-ZåäöÅÄÖ0-9_-]+")
(defonce lcname-regex #"[a-zåäö0-9]+")
(defonce anti-lcname-regex #"[^a-zåäö0-9]+")

(s/def :shop/string    		(s/and string? seq))
(s/def :shop/strings   		(s/coll-of :shop/string))
(s/def :shop/date      		#(instance? org.joda.time.DateTime %))
(s/def :shop/_id       		#(and (string? %) (re-matches uuid-regex %)))
(s/def :shop/created   		:shop/date)
(s/def :shop/entryname 		:shop/string)
(s/def :shop/entrynamelc 	#(and (string? %) (re-matches lcname-regex %)))
(s/def :shop/parent    		(s/nilable :shop/_id))
(s/def :shop/finished  		(s/nilable :shop/date))
(s/def :shop/url       		(s/nilable string?))

;;-----------------------------------------------------------------------------

(s/def :shop/tag   (s/keys :req-un [:shop/_id :shop/created
                                    :tags/entryname :shop/entrynamelc
                                    :shop/parent]))
(s/def :shop/tags  (s/coll-of :shop/tag))

(s/def :tags/entryname #(and (string? %) (re-matches tag-name-regex %)))

;;-----------------------------------------------------------------------------

(s/def :shop/list  (s/keys :req-un [:shop/_id :shop/created
                                    :shop/entryname :shop/entrynamelc
                                    :list/items :list/parent :list/last]))
(s/def :shop/lists (s/coll-of :shop/list))

(s/def :list/item  (s/merge :shop/item (s/keys :req-un [:list/numof :shop/finished])))
(s/def :list/items (s/coll-of :list/item))
(s/def :list/last  boolean?)
(s/def :list/parent (s/nilable (s/keys :req-un [:shop/_id :shop/entryname :list/parent])))
(s/def :list/numof  (s/and int? pos?))

;;-----------------------------------------------------------------------------

(s/def :shop/item    (s/and (s/keys :req-un [:shop/_id :shop/created
                                      :shop/entryname :shop/entrynamelc
                                      :item/tag
                                      :shop/url :item/price
                                      :item/project
                                      :shop/parent
                                      :item/oneshot])
                            #(not (and (some? (:tag %)) (some? (:project %))))))
(s/def :shop/items   (s/coll-of :shop/item))

(s/def :item/oneshot  boolean?)
(s/def :item/price    (s/nilable number?))
(s/def :item/tag      (s/nilable :shop/tag))
(s/def :item/project  (s/nilable :shop/project))

;;-----------------------------------------------------------------------------

(s/def :shop/menu      (s/keys :req-un [:shop/_id :shop/created
                                        :shop/entryname :shop/entrynamelc
                                        :shop/date :menu/recipe]))

(s/def :shop/menus     (s/coll-of :shop/menu))
(s/def :shop/fill-menu (s/keys :req-un [:shop/date]))
(s/def :shop/x-menu    (s/or :full :shop/menu :fill :shop/fill-menu))
(s/def :shop/x-menus   (s/coll-of :shop/x-menu))
(s/def :menu/recipe    (s/nilable (s/keys :req-un [:shop/_id :shop/entryname])))

;;-----------------------------------------------------------------------------

(s/def :shop/project    (s/keys :req-un [:shop/_id :shop/created
                                         :shop/entryname :shop/entrynamelc
                                         :shop/parent
                                         :project/priority :project/deadline
                                         :shop/finished :project/cleared]))

(s/def :shop/projects   (s/coll-of :shop/project))

(s/def :project/priority (s/int-in 1 6))
(s/def :project/deadline (s/nilable :shop/date))
(s/def :project/cleared  (s/nilable :shop/date))

;;-----------------------------------------------------------------------------

(s/def :shop/note   (s/keys :req-un [:shop/_id :shop/created
                                     :shop/entryname :shop/entrynamelc
                                     :note/text]))
(s/def :shop/notes  (s/coll-of :shop/note))

(s/def :note/text   string?)

;;-----------------------------------------------------------------------------

(s/def :shop/recipe   (s/merge :shop/note (s/keys :req-un [:recipe/items :shop/url])))
(s/def :shop/recipes  (s/coll-of :shop/recipe))

(s/def :recipe/text   string?)
(s/def :recipe/unit   string?)
(s/def :recipe/amount string?)
(s/def :recipe/item   (s/keys :req-un [:recipe/text :recipe/unit :recipe/amount]))
(s/def :recipe/items  (s/coll-of :recipe/item))

;;-----------------------------------------------------------------------------

(s/def :shop/user    (s/keys :req-un [:shop/_id :user/created
                                      :user/username :user/password
                                      :user/roles :user/properties]))
(s/def :shop/users   (s/coll-of :shop/user))

(s/def :user/username  	:shop/string)
(s/def :user/roles     	(s/every keyword? :kind set?))
(s/def :user/password   (s/nilable :shop/string))
(s/def :user/properties (s/or :empty (s/and map? empty?) :full (s/map-of keyword? map?)))
(s/def :user/created    (s/or :str string? :dt :shop/created))

(s/def :shop/dd (s/coll-of (s/cat :str string? :id :shop/_id)))
