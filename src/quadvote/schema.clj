(ns quadvote.schema
  (:require
   [clojure.string]))

(def NonBlankString
  [:fn {:error/message {:en "must not be blank"}}
   #(not (clojure.string/blank? %))])

(def Email
  [:re #"^[^\s@]+@[^\s@]+\.[^\s@]+$"])

(def schema
  {:entity/group
   {:group/id {:dat/type :db.type/uuid
               :dat/unique :dat.unique/identity}
    :group/name {:dat/type :db.type/string
                 :dat/spec NonBlankString}
    :group/description {:dat/type :db.type/string}
    :group/open-membership? {:dat/type :db.type/boolean}
    :group/open-topics? {:dat/type :db.type/boolean}
    :group/grant-frequency {:dat/type :db.type/keyword
                            :dat/spec [:enum
                                       :grant-frequency/never
                                       :grant-frequency/daily
                                       :grant-frequency/weekly
                                       :grant-frequency/monthly]}
    :group/grant-amount {:dat/type :db.type/long}}

   :entity/user
   {:user/id {:dat/type :db.type/uuid
              :dat/unique :dat.unique/identity}
    :user/name {:dat/type :db.type/string
                :dat/spec NonBlankString}
    :user/email {:dat/type :db.type/string
                 :dat/spec Email}}

   :entity/membership
   {:membership/id {:dat/type :db.type/uuid
                    :dat/unique :dat.unique/identity}
    :membership/balance {:dat/type :db.type/long}
    :membership/admin? {:dat/type :db.type/boolean}
    :membership/claimable-token-amount {:dat/type :db.type/long}
    :membership/user {:dat/rel [:dat.rel/one
                                :entity/user
                                :user/id]}
    :membership/group {:dat/rel [:dat.rel/one
                                 :entity/group
                                 :group/id]}}

   :entity/claim
   {:claim/id {:dat/type :db.type/uuid
               :dat/unique :dat.unique/identity}
    :claim/amount {:dat/type :db.type/long}
    :claim/timestamp {:dat/type :db.type/instant}
    :claim/membership {:dat/rel [:dat.rel/one
                                 :entity/membership
                                 :membership/id]}}

   :entity/topic
   {:topic/id {:dat/type :db.type/uuid
               :dat/unique :dat.unique/identity}
    :topic/title {:dat/type :db.type/string
                  :dat/spec NonBlankString}
    :topic/description {:dat/type :db.type/string}
    :topic/group {:dat/rel [:dat.rel/one
                            :entity/group
                            :group/id]}
    :topic/user {:dat/rel [:dat.rel/one
                           :entity/user
                           :user/id]}
    :topic/burn {:dat/rel [:dat.rel/one
                           :entity/burn
                           :burn/id]}}

   :entity/vote
   {:vote/id {:dat/type :db.type/uuid
              :dat/unique :dat.unique/identity}
    :vote/voice-amount {:dat/type :db.type/long
                        :dat/spec [:fn {:error/message {:en "must be between 0 and 5"}}
                                   (fn [x]
                                     (<= 0 x 5))]}
    :vote/topic {:dat/rel [:dat.rel/one
                           :entity/topic
                           :topic/id]}
    :vote/user {:dat/rel [:dat.rel/one
                          :entity/user
                          :user/id]}}

   :entity/burn
   {:burn/id {:dat/type :db.type/uuid
              :dat/unique :dat.unique/identity}
    :burn/message {:dat/type :db.type/string}
    :burn/timestamp {:dat/type :db.type/instant}
    :burn/user {:dat/rel [:dat.rel/one
                          :entity/user
                          :user/id]}}})

#_(malli.registry/set-default-registry! (dat.schema/->malli-registry schema))
