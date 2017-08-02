(ns tweetlog.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [tweetlog.core-test]))

(enable-console-print!)

(doo-tests 'tweetlog.core-test)
