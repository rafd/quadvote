(ns quadvote.schema)

(def schema
  {:entity/group
   {:group/id {:dat/spec :uuid
               :dat/unique :dat.unique/identity}
    :group/name {:dat/spec :string}
    :group/description {:dat/spec :string}
    :group/open-membership? {:dat/spec :boolean}
    :group/open-topics? {:dat/spec :boolean}
    :group/grant-frequency {:dat/spec :keyword
                            :dat/malli [:enum
                                        :grant-frequency/never
                                        :grant-frequency/daily
                                        :grant-frequency/weekly
                                        :grant-frequency/monthly]}
    :group/grant-amount {:dat/spec :int}}

   :entity/user
   {:user/id {:dat/spec :uuid
              :dat/unique :dat.unique/identity}
    :user/name {:dat/spec :string}
    :user/email {:dat/spec :string}}

   :entity/membership
   {:membership/id {:dat/spec :uuid
                    :dat/unique :dat.unique/identity}
    :membership/balance {:dat/spec :int}
    :membership/admin? {:dat/spec :boolean}
    :membership/claimable-token-amount {:dat/spec :int}
    :membership/user {:dat/rel [:dat.rel/one
                                :entity/user
                                :user/id]}
    :membership/group {:dat/rel [:dat.rel/one
                                 :entity/group
                                 :group/id]}}

   :entity/claim
   {:claim/id {:dat/spec :uuid
               :dat/unique :dat.unique/identity}
    :claim/amount {:dat/spec :int}
    :claim/timestamp {:dat/spec :inst}
    :claim/membership {:dat/rel [:dat.rel/one
                                 :entity/membership
                                 :membership/id]}}

   :entity/topic
   {:topic/id {:dat/spec :uuid
               :dat/unique :dat.unique/identity}
    :topic/title {:dat/spec :string}
    :topic/description {:dat/spec :string}
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
   {:vote/id {:dat/spec :uuid
              :dat/unique :dat.unique/identity}
    :vote/voice-amount {:dat/spec :int}
    :vote/topic {:dat/rel [:dat.rel/one
                           :entity/topic
                           :topic/id]}
    :vote/user {:dat/rel [:dat.rel/one
                          :entity/user
                          :user/id]}}

   :entity/burn
   {:burn/id {:dat/spec :uuid
              :dat/unique :dat.unique/identity}
    :burn/message {:dat/spec :string}
    :burn/timestamp {:dat/spec :inst}
    :burn/user {:dat/rel [:dat.rel/one
                          :entity/user
                          :user/id]}}})
