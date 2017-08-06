(ns tweetlog.core-test
  (:use midje.sweet)
  (:require [tweetlog.core :refer :all]
            [cloudlog.core :as clg]))

(def rules (->> (all-ns)
                (mapcat ns-publics)
                (map second)
                (map deref)
                (filter #(-> % meta :source-fact vector?))))

(def apply-rules (partial clg/simulate-rules-with rules))
(def query (partial clg/run-query rules))

(fact
 (let [ts (+ (* 3 msec-in-day) 1234)]
   (clg/simulate-with followee-tweets
                      (clg/f [:tweetlog/follows "alice" "bob"] :writers #{"alice"})
                      (clg/f [:tweetlog/tweeted "bob" "hello" ts] :writers #{"bob"}))
   => #{[["alice" 3] "bob" "hello" ts]}))

(fact
 (query (clg/f [:tweetlog/timeline "alice" 2 4]) ;; Alice's timeline for days 2-4 (exclusive)
        3 ;; The answer is of arity 3 (author, tweet, timestamp)
        #{"alice"} ;; The identity making this query
        [(clg/f [:tweetlog/follows "alice" "bob"] :writers #{"alice"})
         (clg/f [:tweetlog/tweeted "bob" "Sunday" (* 1 msec-in-day)] :writers #{"bob"})
         (clg/f [:tweetlog/tweeted "bob" "Monday" (* 2 msec-in-day)] :writers #{"bob"})
         (clg/f [:tweetlog/tweeted "bob" "Tuesday" (* 3 msec-in-day)] :writers #{"bob"})
         (clg/f [:tweetlog/tweeted "bob" "Wednesday" (* 4 msec-in-day)] :writers #{"bob"})])
 => #{["bob" "Monday" (* 2 msec-in-day)]
      ["bob" "Tuesday" (* 3 msec-in-day)]})

;; The query should protect itself, and not answer queries for more than 20 days at a time
(fact
 (query (clg/f [:tweetlog/timeline "alice" 2 23]) ;; Alice's timeline for days 2-4 (exclusive)
        3 #{"alice"}
        [(clg/f [:tweetlog/follows "alice" "bob"] :writers #{"alice"})
         (clg/f [:tweetlog/tweeted "bob" "Sunday" (* 1 msec-in-day)] :writers #{"bob"})
         (clg/f [:tweetlog/tweeted "bob" "Monday" (* 2 msec-in-day)] :writers #{"bob"})
         (clg/f [:tweetlog/tweeted "bob" "Tuesday" (* 3 msec-in-day)] :writers #{"bob"})
         (clg/f [:tweetlog/tweeted "bob" "Wednesday" (* 4 msec-in-day)] :writers #{"bob"})])
 => #{})
