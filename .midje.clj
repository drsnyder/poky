(change-defaults :fact-filter #(not (or (:varnish %) (:integration %))))
