(ns quadvote.ui.form)

(defn radio-list
  [{:keys [legend options value on-change]}]
  [:fieldset {:tw "space-y-1"}
   [:legend legend]
   (for [[opt-value label] options]
     ^{:key opt-value}
     [:label {:tw "flex gap-2 items-center"}
      [:input {:type "radio"
               :checked (= value opt-value)
               :on-change #(on-change opt-value)}]
      label])])
