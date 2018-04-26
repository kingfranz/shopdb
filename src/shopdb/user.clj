(ns shopdb.user
    (:require [slingshot.slingshot :refer [throw+ try+]]
              [clojure.spec.alpha :as s]
              [orchestra.core :refer [defn-spec]]
              [orchestra.spec.test :as st]
              [clojure.string :as str]
              [monger.operators :refer :all]
              [shopdb.spec :refer :all]
              [mongolib.core :as db]))

;;-----------------------------------------------------------------------------

(defn- fix-user
    [user]
    (some-> user
        (update :roles #(->> % (map keyword) set))
        (update :created str)))

(defn-spec get-user (s/nilable :shop/user)
    [uname :user/username]
    (some-> (db/mc-find-one-as-map "get-user" "users"
                 {:username {$regex (str "^" (str/trim uname) "$") $options "i"}})
            (fix-user)))

;;-----------------------------------------------------------------------------

(defn-spec get-user-by-id (s/nilable :shop/user)
    [uid :shop/_id]
    (some-> (db/mc-find-map-by-id "get-user-by-id" "users" uid)
            (fix-user)))

;;-----------------------------------------------------------------------------

(defn-spec get-users :shop/users
    []
    (some->> (db/mc-find-maps "get-user" "users" {})
             (map fix-user)))

;;-----------------------------------------------------------------------------

(defn- count-chars
    [pw c-class]
    (if (nil? (re-find c-class pw))
        (throw+ {:type :db :src "count-chars" :cause (str "PW must contain at least one of " c-class)})
        pw))

(defn- verify-passwd
    [pw]
    (if (not= (str/trim pw) pw)
        (throw+ {:type :db :src "verify-password" :cause "PW can't begin or end with space"})
        (if (< (count pw) 8)
            (throw+ {:type :db :src "verify-password" :cause "PW must be 8 chars or more"})
            (-> pw
                (count-chars #"[a-zåäö]")
                (count-chars #"[A-ZÅÄÖ]")
                (count-chars #"[0-9]")
                (count-chars #"[.*!@#$%^&()=+-]")
                ))))

(defn-spec create-user :shop/user
    [username :shop/username, passwd :shop/password, roles :shop/roles]
    (when (some? (get-user username))
        (throw+ {:type :db :src "create-user" :cause "duplicate username"}))
    (let [user (assoc (db/mk-std-field)
                   :username (str/trim username)
                   ;:password (creds/hash-bcrypt (verify-passwd passwd))
                   :password (verify-passwd passwd)
                   :roles    roles)]
        (db/mc-insert "create-user" "users" user)
        user))

(defn-spec delete-user any?
    [userid :shop/_id]
    (db/mc-remove-by-id "delete-user" "users" userid))

;;-----------------------------------------------------------------------------

(defn-spec set-user-name any?
    [uid :shop/_id, name :shop/string]
    (db/mc-update-by-id "set-user-name" "users" uid {$set {:username name}}))

(defn-spec set-user-password any?
    [uid :shop/_id passwd :shop/password]
    (db/mc-update-by-id "set-user-password" "users" uid
                     {$set {:password passwd}}))
; {$set {:password (creds/hash-bcrypt (verify-passwd passwd))}}))

;;-----------------------------------------------------------------------------

(defn-spec set-user-roles any?
    [uid :shop/_id roles :shop/roles]
    (db/mc-update-by-id "set-user-roles" "users" uid {$set {:roles roles}}))

;;-----------------------------------------------------------------------------

(defn-spec set-user-property any?
    [uid :shop/_id, prop-key keyword?, prop-val map?]
    (let [props (:properties (get-user-by-id uid))]
        (db/mc-update-by-id "set-user-property" "users" uid
                         {$set {:properties (assoc props prop-key prop-val)}})))

;;-----------------------------------------------------------------------------

(defn-spec update-user any?
    [user :shop/user]
    (db/mc-replace-by-id "update-user" "users" user))

(st/instrument)
