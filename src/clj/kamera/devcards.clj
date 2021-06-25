(ns kamera.devcards
  (:require [kamera.core :as k]
            [hickory.core :as h]
            [hickory.select :as s]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clj-chrome-devtools.automation :as cdp-automation]
            [clj-chrome-devtools.commands.dom :as dom]))

(defn extract-links [content]
  (->> (h/as-hickory (h/parse content))
       (s/select (s/child (s/class "com-rigsomelight-devcards-list-group-item")
                          s/last-child))
       (map (comp first :content))
       (map string/trim)
       (map #(str "#!/" %))))

(defn find-test-urls [{:keys [connection] :as session}]
  (extract-links (:outer-html (dom/get-outer-html connection (cdp-automation/root session)))))

(def devcards-list-ready?
  (k/element-exists? ".com-rigsomelight-devcards-list-group"))

(def devcards-page-ready?
  (k/element-exists? ".com-rigsomelight-devcard"))

(def default-opts
  (-> k/default-opts
      (assoc :devcards-options
             {:path "devcards.html" ;; the relative path to the page where the devcards are hosted
              :init-hook nil ;; (fn [session]) function run before attempting to scrape targets
              :on-targets nil ;; (fn [targets]) function called to allow changing the targets before the test is run
              :timeout 60000  ;; time to wait for any devcards page to load
              })
      ;; wait for devcards div to appear before taking screenshot
      (assoc-in [:default-target :ready?] devcards-page-ready?)))

(defn test-devcards
  ([build-or-id] (test-devcards build-or-id default-opts))
  ([build-or-id opts]
   (k/with-chrome-session (:chrome-options opts)
     (fn [session]
       (test-devcards session build-or-id opts))))

  ([session build-or-id opts]
   (let [devcards-url (:devcards-url opts)]
     (test-devcards devcards-url session build-or-id opts)))

  ([devcards-url session _ {:keys [devcards-options] :as opts}]
   (let [{:keys [init-hook on-targets]} devcards-options]
     (log/info "Navigating to" devcards-url)
     (cdp-automation/to session devcards-url)
     (k/wait-for session devcards-list-ready?)
     (Thread/sleep 500)
     (when init-hook
       (init-hook session))
     (let [target-urls (find-test-urls session)
           targets (map (fn [target-url]
                          {:url (str devcards-url target-url)
                           :resize-to-contents {:dom-selector "#com-rigsomelight-devcards-main"}
                           :reference-file (str (subs target-url 3) ".png")})
                        target-urls)
           targets (if on-targets
                     (on-targets targets)
                     targets)]
       (log/infof "Found %s devcards to test" (count target-urls))
       (k/run-tests session targets opts)))))
