(ns tweetlog.core
  (:require [permacode.core :as perm]
            [perm.QmNYKXgUt64cvXau5aNFqvTrjyU8hEKdnhkvtcUphacJaf :as clg]))

(perm/pure
 (def msec-in-day (* 1000 60 60 24))

 ;; Rules should reside in their own source file
 (clg/defrule followee-tweets [[user day] author tweet ts]
   [:tweetlog/follows user author] (clg/by user)
   [:tweetlog/tweeted author tweet ts] (clg/by author)
   (let [day (quot ts msec-in-day)]))

 (clg/defclause tl-1
   [:tweetlog/timeline user from-day to-day -> author tweet ts]
   (let [day-range (range from-day to-day)])
   (when-not (> (count day-range) 20))
   (for [day day-range])
   [followee-tweets [user day] author tweet ts])

 (clg/defrule follower [user f]
   [:tweetlog/follows f user] (clg/by f))
 
 (clg/defclause f1
   [:tweetlog/follower user -> f]
   [follower user f]))
