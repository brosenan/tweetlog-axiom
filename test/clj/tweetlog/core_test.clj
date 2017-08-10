(ns tweetlog.core-test
  (:use midje.sweet)
  (:require [tweetlog.core :refer :all]
            [cloudlog-events.testing :as test]))

(fact
 (let [ts (+ (* 3 msec-in-day) 1234)]
   (test/apply-rules [[:tweetlog/follows "alice" "bob" #{"alice"}]
                      [:tweetlog/tweeted "bob" "hello" ts #{"bob"}]]
                     [:tweetlog.core/followee-tweets ["alice" 3]])
   => #{["bob" "hello" ts]}))

(fact
 (test/query
        [[:tweetlog/follows "alice" "bob" #{"alice"}]
         [:tweetlog/tweeted "bob" "Sunday" (* 1 msec-in-day) #{"bob"}]
         [:tweetlog/tweeted "bob" "Monday" (* 2 msec-in-day) #{"bob"}]
         [:tweetlog/tweeted "bob" "Tuesday" (* 3 msec-in-day) #{"bob"}]
         [:tweetlog/tweeted "bob" "Wednesday" (* 4 msec-in-day) #{"bob"}]]
        [:tweetlog/timeline "alice" 2 4])
 => #{["bob" "Monday" (* 2 msec-in-day)]
      ["bob" "Tuesday" (* 3 msec-in-day)]})

;; The query should protect itself, and not answer queries for more than 20 days at a time
(fact
 (test/query [[:tweetlog/follows "alice" "bob" #{"alice"}]
              [:tweetlog/tweeted "bob" "Sunday" (* 1 msec-in-day) #{"bob"}]
              [:tweetlog/tweeted "bob" "Monday" (* 2 msec-in-day) #{"bob"}]
              [:tweetlog/tweeted "bob" "Tuesday" (* 3 msec-in-day) #{"bob"}]
              [:tweetlog/tweeted "bob" "Wednesday" (* 4 msec-in-day) #{"bob"}]]
             [:tweetlog/timeline "alice" 2 23])
 => map?)

(fact
 (test/apply-rules [[:tweetlog/follows "alice" "bob" #{"alice"}]
                    [:tweetlog/follows "eve" "bob" #{"eve"}]
                    [:tweetlog/follows "alice" "charlie" #{"alice"}]
                    [:tweetlog/follows "foo" "bob" #{"eve"}]]
                   [:tweetlog.core/follower "bob"])
 => #{["alice"] ["eve"]})

(fact
 (test/query [[:tweetlog/follows "alice" "bob" #{"alice"}]
              [:tweetlog/follows "eve" "bob" #{"eve"}]
              [:tweetlog/follows "alice" "charlie" #{"alice"}]
              [:tweetlog/follows "foo" "bob" #{"eve"}]]
             [:tweetlog/follower "bob"])
 => #{["alice"] ["eve"]})


