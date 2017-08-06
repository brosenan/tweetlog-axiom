(ns tweetlog.core
  (:require [reagent.core :as reagent :refer [atom]]
            [axiom-cljs.core :as ax]
            [clojure.string :as str])
  (:require-macros [axiom-cljs.macros :refer [defview defquery]]))

;; This is so that prints will show in the brower's console.
;; Should be removed for production.
(enable-console-print!)

;; The host object holds communication to the host.
;; We provide it with a reagent atom so that we can track its state from the UI.
(defonce host (ax/default-connection atom))

;; A utility function that returns a callback suitable for :on-change.
;; The callback will call the given swap! function to modify the given field value.
(defn update-field [swap! field]
  #(swap! assoc field (-> % .-target .-value)))

;; This is a view.  It tracks facts (in this case, tweets made by the logged-in user),
;; and exposes them as a function of the same name.
(defview tweet-view [me]
  host
  [:tweetlog/tweeted me text ts]
  ;; Allow UI updates on data updates (reagent)
  :store-in (atom nil)
  ;; Order by timestamp, descending.
  :order-by (- ts)
  ;; The logged-in user alone can modify or delete tweets created by this view
  ;; (this is the default)
  :writers #{$user}
  ;; Tweets can be read by anyone.
  ;; (this is the default)
  :readers #{})

;; This function creates the tweets pane
(defn tweets-pane []
  ;; The tweet-view function returns all tweets made by the user
  (let [tweets (tweet-view @(:identity host))]
    ;; This is reagent's hickup-like syntax for generating HTML
    [:div
     [:h2 "Tweets"]
     ;; When a button is clicked we call the view's :add method
     ;; and provide it a map containing all the fields we wish to put in a new (empty) tweet.
     [:button {:on-click #((-> tweets meta :add) {:me @(:identity host)
                                                  :text ""
                                                  ;; The host has a :time method which tells the time...
                                                  :ts ((:time host))})} "tweet!"]
     [:ul
      ;; We now iterate over the results.
      ;; swap! and del! are functions that alow us to modify or delete this specific tweet.
      (for [{:keys [me text ts swap! del!]} tweets]
        ;; React (and hence, reagent) require that items in a list shall have unique keys.
        ;; We use the tweets' timestamps to identify them.
        [:li {:key ts}
         ;; This input box is bound bidirectionally with the stored tweet.
         ;; Its :value property comes from the stored tweet,
         ;; and its :on-change callback uses the swap! function associated with this tweet
         ;; to modify the tweet's text every time the input field's text changes.
         [:input {:value text
                  :on-change (update-field swap! :text)
                  :style {:width "80%"}}]
         ;; The del! function deletes this tweet, so it can be used as the :on-click handler
         ;; of the delete button.
         [:button {:on-click del!} "X"]])]]))

;; This atom stores the name of a new followee (a user we would like to follow) while it is being edited.
(defonce new-followee (atom ""))

;; This view tracks all this user's followees (users he or she follows).
(defview following-view [me]
  host
  [:tweetlog/follows me followee]
  :store-in (atom nil)
  ;; Alphabetical order
  :order-by followee)

;; This function shows the "following" pane.
(defn following-pane []
  [:div
   [:h2 "Following"]
   ;; Query all the users the current user follows
   (let [followees (following-view @(:identity host))]
     [:form
      ;; The name of a new user to follow.
      ;; This is simple reagent-style binding.
      [:input {:value @new-followee
               :on-change #(reset! new-followee (-> % .-target .-value))}]
      ;; When "follow" is clicked...
      [:input {:type "submit"
               :value "follow"
               :on-click #(do
                            ;; Add a "follows" relationship, between the current user and
                            ;; the user who's name was written in the input box, and
                            ((-> followees meta :add) {:me @(:identity host)
                                                       :followee @new-followee})
                            ;; Clear the input box.
                            (reset! new-followee ""))}]
      [:ul
       ;; Iterate over the followees
       (for [{:keys [followee del!]} followees]
         ;; List them...  The followee name is its own unique key
         [:li {:key followee}
          followee
          ;; The del! function is used to unfollow a followee.
          [:button {:on-click del!} "unfollow"]])]])])


;; Timelines can be huge and full of very old, uninteresting tweets.
;; As we would like to only see the most recent ones (and not spend resources on the old ones)
;; we can provide the timeline query a range of day indexes (counting since 1-1-1970),
;; and the query will only provide us with these.
;; To know which days are relevant we have to calculate the current day index.
(defn this-day []
  (let [ms-in-day (* 1000 60 60 24)]
    (quot ((:time host)) ms-in-day)))

;; Which day ranges are we looking at?
(defonce day-ranges (atom [[-2 5]])) ;; One week by default, going 1 day into the future

;; This query will give us the tweets in the currrent user's timeline for the given day-range. 
(defquery timline-query [me day-from day-to]
  host
  [:tweetlog/timeline me day-from day-to -> author tweet ts]
  :store-in (atom nil)
  :order-by (- ts))

;; The timeline pane
(defn timeline-pane []
  [:div
   [:h2 "Timeline"]
   [:ul
    ;; Flatten the two levels of the list
    (doall (apply concat
                  ;; For each range...
                  (for [[to-days-ago from-days-ago] @day-ranges]
                    ;; Fetch all timeline entries (tweets by followed users) in the desired day range
                    (for [{:keys [author tweet ts]} (timline-query @(:identity host)
                                                                   (- (this-day) from-days-ago)
                                                                   (- (this-day) to-days-ago))]
                      [:li {:key ts}
                       (str author ": '" tweet "'")]))))]
   ;; When clicked, adds a range of three more days.
   [:button {:on-click #(swap! day-ranges
                               (fn [ranges]
                                 (let [[from to] (last ranges)]
                                   (conj ranges [to (+ to 3)]))))}
    "Show older..."]])

;; The main page function
(defn twitting []
  [:div
   [:h1 "Hi, " @(:identity host)]
   [:table
    [:tbody
     [:tr.panes
      [:td.pane (tweets-pane)]
      [:td.pane (timeline-pane)]
      [:td.pane (following-pane)]]]]])

;; Renderring the page...
(defn render []
  (reagent/render [twitting] (js/document.getElementById "app")))
