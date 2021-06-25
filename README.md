# kamera

Forked version of [Kamera](https://github.com/oliyh/kamera) to work without
figwheel (for example in a shadow-cljs project)

# usage

```clojure
(ns my-app.cards-test
  (:require [clojure.test :as t]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as server]
            [kamera.devcards :as kam]
            [kamera.core :as k]))

(t/deftest test-all-cards
  (t/testing "Compare devcards to reference images"
    (do
      (server/start!)
      (shadow/watch :cards)
      (let [card-results (k/with-chrome-session {}
                           (fn [session]
                             (kam/test-devcards
                              "http://localhost:1337/cards.html"
                              session
                              nil
                              (assoc-in k/default-opts [:default-target :metric-threshold] 0.001))))]
        (for [card card-results]
          (t/is (< (:metric card) (:metric-threshold card))))))))
```

# Build

```bash
# generate js, used in report pages
clj -M --main cljs.main --output-to resources/public/kamera.js --optimizations advanced -c kamera.app

# generate css for report pages
sassc resources/sass/kamera.scss ./resources/public/css/kamera.css
```

## License

Copyright © 2018 oliyh

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
