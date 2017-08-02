(ns tweetlog.core
  (:require [reagent.core :as reagent :refer [atom]]
            [axiom-cljs.core :as ax]
            [clojure.string :as str])
  (:require-macros [axiom-cljs.macros :refer [defview defquery]]))

(enable-console-print!)

(defonce host (ax/default-connection atom))

(defview my-tweets [me]
  host
  [:tweetlog/tweeted me text ts]
  :store-in (atom nil)
  :order-by (- ts)
  :writers #{$user}
  :readers #{})

(defn update-field [swap! field]
  #(swap! assoc field (-> % .-target .-value)))

(defn tweets []
  (let [tweets (my-tweets @(:identity host))]
    [:div
     [:h1 "Tweets: " @(:identity host)]
     [:button {:on-click #((-> tweets meta :add) {:me @(:identity host)
                                                  :text ""
                                                  :ts ((:time host))})} "tweet!"]
     [:ul
      (for [{:keys [me text ts swap! del!]} tweets]
        [:li {:key ts}
         [:input {:value text
                  :on-change (update-field swap! :text)
                  :style {:width "80%"}}]
         [:button {:on-click del!} "X"]])]]))


(defonce new-followee (atom ""))

(defview my-following [me]
  host
  [:tweetlog/follows me followee]
  :store-in (atom nil)
  :order-by followee)

(defn following []
  [:div
   [:h1 "Following"]
   (let [followees (my-following @(:identity host))]
     [:form
      [:input {:value @new-followee
               :on-change #(reset! new-followee (-> % .-target .-value))}]
      [:input {:type "submit"
               :value "follow"
               :on-click #(do
                            ((-> followees meta :add) {:me @(:identity host)
                                                       :followee @new-followee})
                            (reset! new-followee ""))}]
      [:ul
       (for [{:keys [followee del!]} followees]
         [:li {:key followee}
          followee
          [:button {:on-click del!} "unfollow"]])]])])


(defonce ms-in-week (* 1000 60 60 24 7))
(defn this-week []
  (quot ((:time host)) ms-in-week))

(defquery my-timeline [me week]
  host
  [:tweetlog/timeline me week -> author tweet ts]
  :store-in (atom nil)
  :order-by (- ts))

(defn timeline []
  [:div
   [:h1 "Timeline"]
   [:ul
    (for [{:keys [author tweet ts]} (my-timeline @(:identity host) (this-week))]
      [:li {:key ts}
       (str author ": " tweet)])]])

(defview ready-rules [zero]
  host
  [:axiom/rule-ready zero rule]
  :store-in (atom nil))

(defn control-panel []
  [:div
   [:h1 "Control Panel"]
   [:h2 "Ready Rules"]
   [:ul
    (for [{:keys [rule del!]} (ready-rules 0)]
      [:li {:key (str rule)}
       (str rule)
       [:button {:on-click del!} "X"]])]])

(defn twitting []
  [:div
   [:table
    [:tbody
     [:tr.panes
      (doall (for [[idx pane] (map-indexed vector [tweets timeline following])]
               [:td.pane {:key idx}
                (pane)]))]]]
   (control-panel)])

(defn render []
  (reagent/render [twitting] (js/document.getElementById "app")))
