(ns dat.schema
  (:require
   [malli.core :as m]))

;; https://docs.datomic.com/schema/schema-reference.html
;; https://github.com/metosin/malli?tab=readme-ov-file#built-in-schemas
(def malli-type->datalog-type
  {:uuid :db.type/uuid
   :int :db.type/long
   :string :db.type/string
   :float :db.type/float
   :keyword :db.type/keyword
   :boolean :db.type/boolean
   :inst :db.type/instant})

(def Schema
  [:map-of
   :keyword
   [:map-of
    :keyword
    [:map
     [:dat/spec
      {:optional true}
      (into [:enum] (keys malli-type->datalog-type))]
     [:dat/unique {:optional true}
      [:enum :dat.unique/identity]]
     [:dat/rel {:optional true}
      [:tuple
       [:enum :dat.rel/one :dat.rel/many]
       :keyword
       :keyword]]]]])

(defn by-key [schema]
  (->> schema
       vals
       (apply concat)
       (into {})))

(defn remove-nils
  [m]
  (->> m
       (filter val)
       (into {})))

(defn ->db-schema
  [db-type schema]
  {:pre [(m/validate [:enum
                      :dat.db/datomic
                      :dat.db/datelevin
                      :dat.db/datascript] db-type)
         (m/validate Schema schema)]}
  (->> schema
       by-key
       (map (fn [[k o]]
              [k
               (-> {:db/unique (case (:dat/unique o)
                                 :dat.unique/identity :db.unique/identity
                                 nil)
                    :db/valueType (or (when (#{:dat.db/datalevin} db-type)
                                        (malli-type->datalog-type (:dat/spec o)))
                                      (when (:dat/rel o)
                                        :db.type/ref))
                    :db/cardinality (if-let [[cardinality _ _] (:dat/rel o)]
                                      (case cardinality
                                        :dat.rel/one
                                        :db.cardinality/one
                                        :dat.rel/many
                                        :db.cardinality/many
                                        nil)
                                      :db.cardinality/one)}
                   remove-nils)]))
       (into {})))


