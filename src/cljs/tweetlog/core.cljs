(ns tweetlog.core
  (:require [reagent.core :as reagent :refer [atom]]
            [axiom-cljs.core :as ax]
            [clojure.string :as str])
  (:require-macros [axiom-cljs.macros :refer [defview defquery]]))

(enable-console-print!)

(defonce host (-> js/document.location.host
                  ax/ws-url
                  (str "?_identity=boaz")
                  ax/connection
                  ax/update-on-dev-ver))

(defonce tweets (atom nil))

(defview my-tweets [me]
  host
  [:tweetlog/tweeted me text ts]
  :store-in tweets
  :order-by (- ts)
  :writers #{$user}
  :readers #{})

(defn update-field [swap! field]
  #(swap! assoc field (-> % .-target .-value)))

(defn twitting []
  (let [tweets (my-tweets "boaz")]
    [:div
     [:h1 "Tweets: " @(:identity host)]
     [:button {:on-click #((-> tweets meta :add) {:me "boaz"
                                                  :text ""
                                                  :ts ((:time host))})} "tweet!"]
     [:ul
      (for [{:keys [me text ts swap! del!]} tweets]
        [:li {:key ts}
         [:input {:value text
                  :on-change (update-field swap! :text)}]
         [:button {:on-click del!} "X"]])]]))

(defn render []
  (reagent/render [twitting] (js/document.getElementById "app")))
