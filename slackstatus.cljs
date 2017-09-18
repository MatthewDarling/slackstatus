(require 'clojure.string)
(require 'lumo.core)
(require 'lumo.io)
(require 'lumo.json)
(require 'lumo.classpath)
(def url (js/require "url"))
(def https (js/require "https"))
(def process (js/require "process"))

(defn path-for-dep
  [dep-name dep-version]
  (let [[group-id project-name] (clojure.string/split (str dep-name) "/")
        group-id-as-path (clojure.string/replace group-id "." "/")]
    (str (aget js/process.env "HOME")
         "/.m2/repository/"
         group-id-as-path
         "/"
         project-name
         "/"
         dep-version
         "/"
         project-name
         "-"
         dep-version
         ".jar")))

;;; This seems to only work at the REPL, but is harmless when run as a script, so we may as well
(lumo.classpath/add! (map #(path-for-dep (:dep-name %1) (:dep-version %1))
                          [{:dep-name "com.cemerick/url" :dep-version "0.1.1"}
                           {:dep-name "pathetic/pathetic" :dep-version "0.5.0"}]))
(require 'cemerick.url)

(def token (lumo.io/slurp (str (aget js/process.env "HOME") "/.slacktoken")))
(def base-url
  (-> (cemerick.url/url "https://slack.com/")
      (assoc :path "/api/users.profile.set")
      (assoc-in [:query "token"] token)))

(defn should-start-with
  [string beginning]
  (if-not (clojure.string/starts-with? string beginning)
    (str beginning string)
    string))

(defn should-end-with
  [string ending]
  (if-not (clojure.string/ends-with? string ending)
    (str string ending)
    string))

(defn surround-string
  [string surrounder]
  (-> string (should-start-with surrounder) (should-end-with surrounder)))

(defn status-json
  ([text] (status-json text nil))
  ([text emoji]
   (let [json-status (cond-> {}
                       text (assoc "status_text" text)
                       emoji (assoc "status_emoji" (surround-string emoji ":")))]
     (when (not-empty json-status)
       (-> json-status lumo.json/write-str)))))

(defn status-change-url
  ([text] (status-change-url text nil))
  ([text emoji]
   (assoc-in base-url [:query "profile"] (status-json text emoji))))

(defn post
  "From https://codereview.stackexchange.com/q/116507"
  [url-str callback]
  (let [parsed-url (doto (.parse url url-str)
                     (aset "method" "POST"))]
    (doto (.request https parsed-url callback)
      (.end))))

(defn response-body-handler
  [response-body]
  (let [body (lumo.json/read-str (.toString response-body)
                                 :key-fn keyword)]
    (cond
      (nil? body) (throw (ex-info "No response body, something went wrong"  {}))
      (not (:ok body)) (throw (ex-info "Status change failed" body))
      :else (do (println "Status change succeeded!")
                (lumo.core/exit)))))

(defn status-change-handler
  [response]
  (.on response "data"
       response-body-handler))

;; (post (.toString (status-change-url "abcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghijabcdefghij"))
;;       status-change-handler)
;; => {:ok false, :error "too_long", :limit 100, :field "status_text"}

;; (post (.toString (status-change-url "Atlantic office" "fake-emoji"))
;;       status-change-handler)
;; => {:ok false, :error "profile_status_set_failed_not_valid_emoji"}

(defn url-for-args
  []
  (apply status-change-url (take 2 *command-line-args*)))

(if (empty? *command-line-args*)
  (do (println "In order to change Slack status, you must provide the status text as an argument. An optional second argument represents the emoji.")
      (lumo.core/exit 1))
  (post (.toString (url-for-args))
        status-change-handler))
